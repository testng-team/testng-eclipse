package org.testng.eclipse.ui;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.testng.eclipse.collections.Maps;
import org.testng.eclipse.collections.Sets;
import org.testng.eclipse.util.ResourceUtil;

import java.util.Map;
import java.util.Set;

/**
 * A view that shows a summary of the test run.
 */
public class SummaryTab extends TestRunTab  {

  private Map<String, RunInfo> m_tests = Maps.newHashMap();

  /** The table that contains all the tests */
  private TableViewer m_testViewer;
  private Map<String, Long> m_testTimes = Maps.newHashMap();

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
    label.setText("Summary tab");

    tab.setControl(composite);
    tab.setToolTipText(ResourceUtil.getString(getTooltipKey())); //$NON-NLS-1$
    m_testViewer = new TableViewer(composite);
    Table table = m_testViewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    String[] titles = { "Test name", "Time" };
    int[] bounds = { 150, 150 };
    for (int i = 0; i < titles.length; i++) {
      TableViewerColumn viewerColumn = new TableViewerColumn(m_testViewer, SWT.NONE);
      final TableColumn column = viewerColumn.getColumn();
      column.setText(titles[i]);
      column.setWidth(bounds[i]);
      column.setResizable(true);
      column.setMoveable(true);      
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
        String testId = getTestId(runInfo);
        switch(columnIndex) {
          case 0:  return ((RunInfo) element).getTestName();
          case 1: return m_testTimes.get(testId).toString();
          default: return "";
        }
      }

      public Image getColumnImage(Object element, int columnIndex) {
        return null;
      }
    });

    m_testViewer.setInput(m_tests);
  }

  private String getTestId(RunInfo runInfo) {
    return runInfo.getSuiteName() + "." + runInfo.getTestName();
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
    String testId = getTestId(runInfo);
    m_tests.put(testId, runInfo);
    Long time = m_testTimes.get(testId);
    if (time == null) {
      time = 0L;
    }
    time += runInfo.getTime();
    m_testTimes.put(testId, time);
    m_testViewer.setInput(m_tests);
  }

}
