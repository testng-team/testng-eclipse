# Design: Paginated Test Result Display

## Problem

When running thousands of parameterized tests, the TestNG Eclipse plugin's UI rendering lags far behind test execution. The root cause: every single test result triggers a synchronous UI update (`postSyncRunnable` -> `Display.syncExec`) that creates SWT `TreeItem` nodes. With thousands of results, this creates thousands of synchronous round-trips to the UI thread, each allocating SWT resources and expanding tree nodes.

## Current Architecture

```
Test Process --[socket]--> EclipseTestRunnerClient
    --> SuiteRunInfo (stores all RunInfo in List<RunInfo>)
    --> TestRunnerViewPart.postTestResult()
        --> postSyncRunnable {
              progressBar.step()
              for each tab: tab.updateTestResult(runInfo)  // creates TreeItems
            }
```

**Key classes:**
- `RunInfo` - immutable data object for a single test result
- `SuiteRunInfo` - aggregates all `RunInfo` in `List<RunInfo> m_results`
- `AbstractTab` - renders tree: Suite > Test > Class > Method hierarchy
- `TestRunnerViewPart` - orchestrates everything, implements listener interfaces

**Current bottleneck:** `AbstractTab.updateTestResult(RunInfo, boolean)` is called for every result, creating `TreeItem` widgets and expanding nodes synchronously on the UI thread.

## Proposed Solution: Pagination with Virtual Data Store

Inspired by JUnit's approach of limiting display to N tests at a time, but with navigable pages.

### 1. Data Storage Layer: `PagedResultStore`

A new class that sits between `SuiteRunInfo` (which keeps all results) and the tree tabs (which render them).

```
PagedResultStore
├── allResults: List<RunInfo>          // reference to SuiteRunInfo.m_results
├── filteredResults: List<RunInfo>     // after applying accept + search filter
├── pageSize: int                      // default 1000, configurable
├── currentPage: int                   // 0-based
├── getPageResults(): List<RunInfo>    // returns current page slice
├── getTotalPages(): int
├── getTotalFilteredCount(): int
├── setPage(int page): void
├── nextPage() / prevPage(): void
├── setFilter(Predicate<RunInfo>): void
└── addResult(RunInfo): void           // appends, updates filteredResults if matches
```

**Design decisions:**
- `allResults` is a shared reference to `SuiteRunInfo.m_results` (no data duplication)
- `filteredResults` is lazily rebuilt only when the filter changes
- Page navigation only re-renders the current page slice (rebuild tree from scratch for that page)
- During a live test run, new results append to the last page; the tree is only updated if the user is viewing the last page

### 2. Modified AbstractTab

```
AbstractTab (modified)
├── m_store: PagedResultStore          // NEW: replaces direct m_runInfos tracking
├── m_runInfos: Set<RunInfo>           // REMOVED (data now in store)
├── updateTestResult(RunInfo, expand)  // MODIFIED: delegates to store, conditionally renders
├── updateTestResult(List<RunInfo>)    // MODIFIED: bulk-loads into store, renders current page
├── renderCurrentPage()                // NEW: clears tree, renders only page slice
├── updateSearchFilter(String)         // MODIFIED: updates store filter, re-renders page
└── [existing tree-building logic stays but only operates on page slice]
```

**Key behavioral changes:**

| Scenario | Current Behavior | New Behavior |
|----------|-----------------|--------------|
| Result arrives during run | Creates TreeItem immediately | Adds to store; if on last page AND page not full, creates TreeItem. Otherwise, just updates page label. |
| Suite finishes | Rebuilds entire tree with all results | Renders page 1 only (1000 items max) |
| User changes search filter | Rebuilds tree with all matching results | Rebuilds filtered list in store, renders page 1 of filtered results |
| User navigates page | N/A | Clears tree, renders requested page slice |

### 3. Pagination UI Controls

Added to `AbstractTab.createTabControl()`, below the tree widget:

```
┌──────────────────────────────────────────────────────┐
│  [Tree: Suite > Test > Class > Method...]            │
│                                                      │
│                                                      │
├──────────────────────────────────────────────────────┤
│ [<<] [<] Page 1 of 5 (4237 results) [>] [>>]        │
└──────────────────────────────────────────────────────┘
```

Components:
- `Composite m_paginationBar` - horizontal bar at bottom of tree area
- `Button m_firstPage, m_prevPage, m_nextPage, m_lastPage` - navigation buttons
- `Label m_pageInfo` - "Page X of Y (Z results)"
- Buttons disabled at boundaries (first/last page)
- Bar hidden entirely when total results <= pageSize (no pagination needed)

### 4. Integration Points

#### 4.1 TestRunnerViewPart.postTestResult()

No change to the method signature. The existing flow remains:

```java
postTestResult(RunInfo runInfo, boolean isSuccess) {
    currentSuiteRunInfo.add(runInfo);  // unchanged: store in master list
    postSyncRunnable(() -> {
        progressBar.step(isSuccess);   // unchanged: always update progress
        for (TestRunTab tab : ALL_TABS) {
            tab.updateTestResult(runInfo, true);  // tab decides whether to render
        }
    });
}
```

The intelligence moves into `AbstractTab.updateTestResult()`:

```java
// pseudocode
void updateTestResult(RunInfo runInfo, boolean expand) {
    m_store.addResult(runInfo);
    if (m_store.isOnLastPage() && m_store.currentPageHasRoom()) {
        // render this single item into the tree (existing logic)
        renderSingleResult(runInfo, expand);
    }
    updatePaginationLabel();  // always update "Page X of Y (Z results)"
}
```

#### 4.2 AbstractTab.updateSearchFilter()

```java
// pseudocode
void updateSearchFilter(String text) {
    m_store.setSearchFilter(text);  // rebuilds filteredResults
    m_store.setPage(0);             // reset to first page
    renderCurrentPage();            // clear tree + render page slice
}
```

#### 4.3 FailureTab / SuccessTab

These subclasses override `acceptTestResult()`. The store's filter predicate should incorporate both:
- The subclass's `acceptTestResult()` filter (pass/fail/all)
- The search text filter

This means `PagedResultStore` needs a composite filter:

```java
store.setFilter(runInfo -> acceptTestResult(runInfo) && matchesSearchFilter(runInfo));
```

#### 4.4 SummaryTab

`SummaryTab` uses a `TableViewer` not a tree, and aggregates results differently. It should **not** be paginated (it shows one row per test, not per method). No changes needed.

### 5. Page Size Configuration

- Default: 1000 results per page
- Stored in Eclipse preferences: `TestNGPluginConstants.S_PAGE_SIZE`
- Accessible via Preferences > TestNG > Page Size
- Reasonable range: 500-5000

### 6. Rendering Performance: renderCurrentPage()

When switching pages, the tree is fully cleared and rebuilt from the page slice:

```java
void renderCurrentPage() {
    m_tree.setRedraw(false);         // suppress repaints
    reset();                         // clear all tree items and maps
    List<RunInfo> pageResults = m_store.getPageResults();
    for (RunInfo ri : pageResults) {
        renderSingleResult(ri, false);  // don't expand individually
    }
    expandAll();                     // expand all at once
    m_tree.setRedraw(true);          // single repaint
    updatePaginationControls();      // update button states + label
}
```

This ensures at most `pageSize` TreeItems exist at any time, keeping SWT resource usage bounded.

### 7. Edge Cases

| Case | Handling |
|------|----------|
| Results < pageSize | Pagination bar hidden, behavior identical to current |
| User on page 2, new results arrive | Page label updates count, tree unchanged until user navigates |
| User on last page, new results arrive | If room on page, item added to tree. If page full, label updates only. |
| Filter reduces results to < pageSize | Pagination bar hidden, single page shown |
| Filter produces 0 results | Empty tree, label shows "0 results" |
| Suite finishes (showResultsInTree) | Store resets to page 0, renders first page |
| Run history switch (reset(SuiteRunInfo)) | Store gets new data source, renders page 0 |

### 8. Class Diagram

```
TestRunnerViewPart
    │
    ├── SuiteRunInfo (owns List<RunInfo>)
    │
    ├── AbstractTab
    │     ├── PagedResultStore (references SuiteRunInfo's list)
    │     │     ├── filteredResults: List<RunInfo>
    │     │     ├── pageSize: int
    │     │     ├── currentPage: int
    │     │     └── filter: Predicate<RunInfo>
    │     │
    │     ├── m_tree: Tree (SWT)
    │     ├── m_paginationBar: Composite (NEW)
    │     │     ├── m_firstPage, m_prevPage: Button
    │     │     ├── m_pageInfo: Label
    │     │     └── m_nextPage, m_lastPage: Button
    │     │
    │     ├── FailureTab (acceptTestResult filters to failures)
    │     └── SuccessTab (acceptTestResult accepts all)
    │
    └── SummaryTab (unchanged, no pagination)
```

### 9. Files to Modify

| File | Change |
|------|--------|
| **NEW** `PagedResultStore.java` | New class: paginated, filterable view over `List<RunInfo>` |
| `AbstractTab.java` | Add `PagedResultStore`, pagination UI, modify `updateTestResult()` and `updateSearchFilter()` |
| `TestRunTab.java` | No change (interface unchanged) |
| `TestRunnerViewPart.java` | No change (tabs handle pagination internally) |
| `SuiteRunInfo.java` | No change (data store unchanged) |
| `RunInfo.java` | No change |
| `TestNGPluginConstants.java` | Add `S_PAGE_SIZE` preference key |
| Preference page | Add page size setting |

### 10. Migration Risk

- **Low risk**: All changes are internal to `AbstractTab` and its subclasses
- **Backward compatible**: When results < pageSize, behavior is identical to current
- **No protocol changes**: Data reception pipeline is untouched
- **No data model changes**: `RunInfo` and `SuiteRunInfo` are unchanged
- **Incremental**: Can be implemented and tested in isolation within `AbstractTab`
