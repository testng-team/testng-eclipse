package org.testng.eclipse.ui.summary;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.testng.eclipse.ui.OpenTestAction;
import org.testng.eclipse.ui.RunInfo;
import org.testng.eclipse.ui.TestRunTab;
import org.testng.eclipse.ui.TestRunnerViewPart;
import org.testng.remote.strprotocol.SuiteMessage;

/**
 * A view that shows a summary of the test run and allows the user to sort these results
 * by timings, names, counts, etc...
 */
public class SummaryTab extends TestRunTab  {

  private Map<String, RunInfo> m_tests = new HashMap<>();

  /** The table that contains all the tests */
  private TableViewer m_testViewer;

  /** A test result, updated whenever we receive a new test result */
  static class TestResult {
    Long time = 0L;
    Set<String> methods = new HashSet<>();
    Set<String> classes = new HashSet<>();
  }

  /** The model for the excluded method table */
  private Map<String, TestResult> m_testResults = new HashMap<>();

  /** The table that contains the excluded methods */
  private TableViewer m_excludedMethodViewer;

  /** The model for the excluded method table */
  private List<ExcludedMethod> m_excludedMethodsModel = new ArrayList<>();

  // The filters for the two tables
  private RunInfoFilter m_testSearchFilter;
  private StringFilter m_excludedMethodFilter;

  /** The id of the currently selected item */
  private String m_selectedId;

  private TestRunnerViewPart m_testRunnerPart;


  @Override
  public String getTooltipKey() {
    return "Summary.tab.tooltip";
  }

  @Override
  public String getNameKey() {
    return "Summary.tab.title"; //$NON-NLS-1$
  }

  @Override
  public Image getImage() {
    return null;
  }

  @Override
  public Composite createTabControl(Composite parent, TestRunnerViewPart runner) {
    m_testRunnerPart = runner;
    Composite result = new Composite(parent, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 1;
    result.setLayout(gridLayout);

    //
    // Test table
    //
    {
      createLabel(result, "Tests");
      createTestViewer(result);
    }

    //
    // Excluded methods
    //
    {
      createLabel(result, "Excluded methods");
      createExcludedMethodViewer(result);
    }

    return result;
  }

  private void createLabel(Composite parent, String text) {
    Label result = new Label(parent, SWT.NONE);
    result.setText(text);
    result.setFont(JFaceResources.getBannerFont());
    result.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
  }

  private void createExcludedMethodViewer(Composite result) {
    m_excludedMethodFilter = new StringFilter();
    m_excludedMethodViewer = createViewer(result,
        new String[] { "Class name", "Method name", "Description" },
        new int[] { 300, 250, 300 },
        new StringTableSorter(this),
        m_excludedMethodFilter
        );

    m_excludedMethodViewer.getTable().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDoubleClick(MouseEvent e) {
        handleDoubleClick(e);
      }

      private void handleDoubleClick(MouseEvent e) {
        TableItem[] items = ((Table) e.getSource()).getSelection();
        if (items.length > 0) {
          ExcludedMethod em = (ExcludedMethod) items[0].getData();
          System.out.println("Double click " + em.packageName + "." + em.methodName);
          OpenTestAction openAction
              = new OpenTestAction(m_testRunnerPart, em.packageName, em.methodName,
                    true /* activate */);
          openAction.run();
        }
      }
    });

    //
    // Label provider
    //
    m_excludedMethodViewer.setLabelProvider(new ITableLabelProvider() {

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
        ExcludedMethod em = (ExcludedMethod) element;
        switch(columnIndex) {
          case 0: return em.packageName;
          case 1: return em.methodName;
          case 2: return em.description;
          default: return "";
        }
      }

      public Image getColumnImage(Object element, int columnIndex) {
        return null;
      }
    });

    //
    // Content provider
    //
    m_excludedMethodViewer.setContentProvider(new IStructuredContentProvider() {

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public Object[] getElements(Object inputElement) {
        return ((List<String>) inputElement).toArray();
      }

    });
  }

  /**
   * Parses the fully qualified name to extract the package and class name.
   * 
   * @return an String array made of the package name and the class name  
   */
  private String[] parseFqn(String fqn) {
    String packageName = fqn;
    String className = fqn;
    int ind = fqn.lastIndexOf(".");
    if (ind >= 0) {
      packageName = fqn.substring(0, ind);
      className = fqn.substring(ind + 1);
    }
    return new String[] { packageName, className };
  }

  private void createTestViewer(Composite result) {
    //
    // Table sorter
    //
    m_testSearchFilter = new RunInfoFilter();
    m_testViewer = createViewer(result,
        new String[] { "Test name", "Time (seconds)", "Class count", "Method count" },
        new int[] { 150, 150, 100, 100 },
        new RunInfoTableSorter(this),
        m_testSearchFilter
        );

    //
    // Row selection
    //
    m_testViewer.addSelectionChangedListener(new ISelectionChangedListener() {

      public void selectionChanged(SelectionChangedEvent event) {
        ISelection selection = event.getSelection();
        if (selection instanceof StructuredSelection) {
          StructuredSelection ss = (StructuredSelection) selection;
          RunInfo selected = ((RunInfo) ss.getFirstElement());
          if (selected != null) {
            String selectedId = ((RunInfo) ss.getFirstElement()).getTestId();
            if (m_selectedId != null && !m_selectedId.startsWith(selectedId)) {
              m_selectedId = selectedId;
            }
          }
        }
      }

    });

    //
    // Content provider
    //
    m_testViewer.setContentProvider(new IStructuredContentProvider() {

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      public Object[] getElements(Object inputElement) {
        return m_tests.values().toArray();
      }
      
    });

    //
    // Label provider
    //
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

  private TableViewer createViewer(Composite parent, String[] columns, int[] bounds,
      final AbstractTableSorter tableSorter, ViewerFilter filter) {
    final TableViewer result = new TableViewer(parent);
    final Table table = result .getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    result.setSorter(tableSorter);

    for (int i = 0; i < columns.length; i++) {
      final int index = i;
      TableViewerColumn viewerColumn = new TableViewerColumn(result, SWT.NONE);
      final TableColumn column = viewerColumn.getColumn();
      column.setText(columns[i]);
      column.setWidth(bounds[i]);
      column.setResizable(true);
      column.setMoveable(true);
      column.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          if (tableSorter != null) {
            tableSorter.setColumn(index);
          }
          int dir = table.getSortDirection();
          if (table.getSortColumn() == column) {
            dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
          } else {
            dir = SWT.DOWN;
          }
          table.setSortDirection(dir);
          table.setSortColumn(column);
          result.refresh();        }
      });
    }

    //
    // Filter
    //
    result.setFilters(new ViewerFilter[] { filter });

    return result;
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
    return m_selectedId;
  }

  @Override
  public void setSelectedTest(String id) {
    if (id == null) return;

    m_selectedId = id;
    // We might be receiving the id of a method, but we only store test names,
    // so just find the name of the test that contains this method and select
    // the corresponding row. Note that we still store the original id so that
    // when the user switches to a different tab, their selection there remains
    // untouched.
    for (String test : m_testResults.keySet()) {
      if (id.startsWith(test)) {
        m_testViewer.setSelection(new StructuredSelection(m_tests.get(test)));
      }
    }
  }

  @Override
  public void updateTestResult(RunInfo runInfo, boolean expand) {
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
  public void updateTestResult(List<RunInfo> results) {
    if (results.size() > 0) {
      aboutToStart(); // do a reset
      for (RunInfo ri : results) {
        updateTestResult(ri, false /* unused param in this tab */);
      }
    }
  }
  
  @Override
  public void aboutToStart() {
    m_tests.clear();
    m_testResults.clear();
    m_testViewer.refresh();
    m_excludedMethodViewer.getTable().clearAll();
  }

  @Override
  public void updateSearchFilter(String text) {
    m_testSearchFilter.setFilter(text);
    m_testViewer.refresh();

    m_excludedMethodFilter.setFilter(text);
    m_excludedMethodViewer.refresh();
  }

  private class ExcludedMethod {
    public String packageName;
    public String methodName;
    public String description;
  }

  public void setExcludedMethodsModel(final SuiteMessage message) {
    m_excludedMethodsModel.clear();
    for (String method : message.getExcludedMethods()) {
      ExcludedMethod em = new ExcludedMethod();
      String[] parsed = parseFqn(method);
      em.packageName = parsed[0];
      em.methodName = parsed[1];
      em.description = message.getDescriptionForMethod(method);
      m_excludedMethodsModel.add(em);
    }
    m_excludedMethodViewer.getTable().getDisplay().syncExec(new Runnable() {
      public void run() {
        m_excludedMethodViewer.setInput(m_excludedMethodsModel);
        m_excludedMethodViewer.refresh();
      }
    });
  }
}
