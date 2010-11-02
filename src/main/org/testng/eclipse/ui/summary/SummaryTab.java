package org.testng.eclipse.ui.summary;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.testng.eclipse.collections.Maps;
import org.testng.eclipse.collections.Sets;
import org.testng.eclipse.ui.RunInfo;
import org.testng.eclipse.ui.TestRunTab;
import org.testng.eclipse.ui.TestRunnerViewPart;
import org.testng.eclipse.util.ResourceUtil;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;

/**
 * A view that shows a summary of the test run.
 */
public class SummaryTab extends TestRunTab  {

  private Map<String, RunInfo> m_tests = Maps.newHashMap();

  /** The table that contains all the tests */
  private TableViewer m_testViewer;

  class TestResult {
    Long time = 0L;
    Set<String> methods = Sets.newHashSet();
    Set<String> classes = Sets.newHashSet();
  }

  private Map<String, TestResult> m_testResults = Maps.newHashMap();

  private TestNameFilter m_searchFilter;

  protected String getTooltipKey() {
    return "Summary.tab.tooltip";
  }

  public String getName() {
    return getResourceString("Summary.tab.title"); //$NON-NLS-1$
  }

  @Override
  public void createTabControl(CTabFolder tabFolder, TestRunnerViewPart runner) {
    CTabItem tab = new CTabItem(tabFolder, SWT.NONE);
    tab.setText(getName());
//    hierarchyTab.setImage(m_testHierarchyIcon);

    Composite composite = new Composite(tabFolder, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    gridLayout.numColumns = 1;
    composite.setLayout(gridLayout);

    //
    // Tests
    //
    Label label = new Label(composite, SWT.NONE);
    label.setText("Tests");
    label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));

    tab.setControl(composite);
    tab.setToolTipText(ResourceUtil.getString(getTooltipKey())); //$NON-NLS-1$
    m_testViewer = new TableViewer(composite);
    Table table = m_testViewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    //
    // Table sorter
    //
    final TestTableSorter tableSorter = new TestTableSorter(this);
    m_testViewer.setSorter(tableSorter);

    //
    // Filter
    //
    m_searchFilter = new TestNameFilter();
    m_testViewer.setFilters(new ViewerFilter[] { m_searchFilter });

    //
    // Columns
    //
    String[] titles = { "Test name", "Time (seconds)", "Class count", "Method count" };
    int[] bounds = { 150, 150, 100, 100 };
    for (int i = 0; i < titles.length; i++) {
      final int index = i;
      TableViewerColumn viewerColumn = new TableViewerColumn(m_testViewer, SWT.NONE);
      final TableColumn column = viewerColumn.getColumn();
      column.setText(titles[i]);
      column.setWidth(bounds[i]);
      column.setResizable(true);
      column.setMoveable(true);      
      column.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          tableSorter.setColumn(index);
          int dir = m_testViewer.getTable().getSortDirection();
          if (m_testViewer.getTable().getSortColumn() == column) {
            dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
          } else {
            dir = SWT.DOWN;
          }
          m_testViewer.getTable().setSortDirection(dir);
          m_testViewer.getTable().setSortColumn(column);
          m_testViewer.refresh();        }
      });
    }

    m_testViewer.setContentProvider(new IStructuredContentProvider() {

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public Object[] getElements(Object inputElement) {
        return m_tests.values().toArray();
      }
      
    });
    m_testViewer.setLabelProvider(new ITableLabelProvider() {
      
      public void removeListener(ILabelProviderListener listener) {
      }
      
      public boolean isLabelProperty(Object element, String property) {
        return false;
      }
      
      public void dispose() {
      }
      
      public void addListener(ILabelProviderListener listener) {
      }
      
      public String getColumnText(Object element, int columnIndex) {
        RunInfo runInfo = (RunInfo) element;
        String testId = runInfo.getTestId();
        switch(columnIndex) {
          case 0:  return ((RunInfo) element).getTestName();
          case 1: return MessageFormat.format("{0}", ((float) getTestTime(testId)) / 1000);
          case 2: return Integer.toString(getTestClassCount(testId));
          case 3: return Integer.toString(getTestMethodCount(testId));
          default: return "";
        }
      }

      public Image getColumnImage(Object element, int columnIndex) {
        return null;
      }
    });

    m_testViewer.setInput(m_tests);
  }

  protected int getTestMethodCount(String testId) {
    return m_testResults.get(testId).methods.size();
  }

  public long getTestTime(String testId) {
    return m_testResults.get(testId).time;
  }

  public int getTestClassCount(String testId) {
    return m_testResults.get(testId).classes.size();
  }

  @Override
  public String getSelectedTestId() {
    return null;
  }

  @Override
  public void updateTestResult(RunInfo runInfo) {
    //
    // Update tests
    //
    String testId = runInfo.getTestId();
    m_tests.put(testId, runInfo);
    TestResult tr = m_testResults.get(testId);
    if (tr == null) {
      tr = new TestResult();
      m_testResults.put(testId, tr);
    }
    tr.time += runInfo.getTime();
    tr.methods.add(runInfo.getMethodId());
    tr.classes.add(runInfo.getClassId());
    m_testViewer.refresh();
  }

  @Override
  public void aboutToStart() {
    m_tests.clear();
    m_testResults.clear();
    m_testViewer.refresh();
  }

  @Override
  public void updateSearchFilter(String text) {
    m_searchFilter.setFilter(text);
    m_testViewer.refresh();
  }

}
