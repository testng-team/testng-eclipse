package org.testng.eclipse.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * A paginated, filterable view over a list of {@link RunInfo} results.
 * This class sits between {@link SuiteRunInfo} (which stores all results)
 * and the tree tabs (which render them), limiting the number of tree items
 * created at any one time to a configurable page size.
 *
 * @see <a href="https://github.com/testng-team/testng-eclipse/issues/578">Issue #578</a>
 */
public class PagedResultStore {

  public static final int DEFAULT_PAGE_SIZE = 1000;

  private final List<RunInfo> allResults = new ArrayList<>();
  private List<RunInfo> filteredResults = new ArrayList<>();
  private Predicate<RunInfo> filter = r -> true;
  private int pageSize;
  private int currentPage;

  public PagedResultStore() {
    this(DEFAULT_PAGE_SIZE);
  }

  public PagedResultStore(int pageSize) {
    this.pageSize = pageSize;
    this.currentPage = 0;
  }

  /**
   * Appends a new result. If it matches the current filter it is added to
   * the filtered list as well.
   *
   * @return true if the result was added to the filtered list
   */
  public boolean addResult(RunInfo runInfo) {
    allResults.add(runInfo);
    if (filter.test(runInfo)) {
      filteredResults.add(runInfo);
      return true;
    }
    return false;
  }

  /**
   * Replaces the filter and rebuilds the filtered results list.
   */
  public void setFilter(Predicate<RunInfo> filter) {
    this.filter = filter;
    rebuildFilteredResults();
  }

  /**
   * Returns the slice of filtered results for the current page.
   */
  public List<RunInfo> getPageResults() {
    int from = currentPage * pageSize;
    if (from >= filteredResults.size()) {
      return Collections.emptyList();
    }
    int to = Math.min(from + pageSize, filteredResults.size());
    return Collections.unmodifiableList(filteredResults.subList(from, to));
  }

  public int getCurrentPage() {
    return currentPage;
  }

  /**
   * Sets the current page (0-based). Clamped to valid range.
   */
  public void setPage(int page) {
    int maxPage = Math.max(0, getTotalPages() - 1);
    this.currentPage = Math.max(0, Math.min(page, maxPage));
  }

  public void nextPage() {
    setPage(currentPage + 1);
  }

  public void prevPage() {
    setPage(currentPage - 1);
  }

  public void firstPage() {
    setPage(0);
  }

  public void lastPage() {
    setPage(getTotalPages() - 1);
  }

  public int getTotalPages() {
    if (filteredResults.isEmpty()) return 1;
    return (filteredResults.size() + pageSize - 1) / pageSize;
  }

  public int getTotalFilteredCount() {
    return filteredResults.size();
  }

  public int getTotalCount() {
    return allResults.size();
  }

  public int getPageSize() {
    return pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  /**
   * @return true if the user is viewing the last page
   */
  public boolean isOnLastPage() {
    return currentPage >= getTotalPages() - 1;
  }

  /**
   * @return true if the current page has not yet reached pageSize items
   */
  public boolean currentPageHasRoom() {
    int itemsOnCurrentPage = filteredResults.size() - (currentPage * pageSize);
    return itemsOnCurrentPage < pageSize;
  }

  /**
   * Returns all results (unfiltered).
   */
  public List<RunInfo> getAllResults() {
    return Collections.unmodifiableList(allResults);
  }

  /**
   * Clears all data and resets to page 0.
   */
  public void clear() {
    allResults.clear();
    filteredResults.clear();
    currentPage = 0;
  }

  /**
   * Reloads the store with a new set of results (e.g. from run history).
   */
  public void load(List<RunInfo> results) {
    clear();
    allResults.addAll(results);
    rebuildFilteredResults();
  }

  private void rebuildFilteredResults() {
    filteredResults = new ArrayList<>();
    for (RunInfo ri : allResults) {
      if (filter.test(ri)) {
        filteredResults.add(ri);
      }
    }
    // Clamp current page to valid range after filter change
    if (currentPage >= getTotalPages()) {
      currentPage = Math.max(0, getTotalPages() - 1);
    }
  }
}
