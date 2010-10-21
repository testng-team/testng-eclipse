package org.testng.eclipse.ui;

import static org.testng.eclipse.ui.Images.IMG_SUITE;
import static org.testng.eclipse.ui.Images.IMG_SUITE_FAIL;
import static org.testng.eclipse.ui.Images.IMG_SUITE_OK;
import static org.testng.eclipse.ui.Images.IMG_SUITE_RUN;
import static org.testng.eclipse.ui.Images.IMG_SUITE_SKIP;
import static org.testng.eclipse.ui.Images.IMG_TEST;
import static org.testng.eclipse.ui.Images.IMG_TEST_FAIL;
import static org.testng.eclipse.ui.Images.IMG_TEST_HIERARCHY;
import static org.testng.eclipse.ui.Images.IMG_TEST_OK;
import static org.testng.eclipse.ui.Images.IMG_TEST_RUN;
import static org.testng.eclipse.ui.Images.IMG_TEST_SKIP;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.search2.internal.ui.basic.views.ExpandAllAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.testng.ITestResult;
import org.testng.eclipse.collections.Maps;
import org.testng.eclipse.util.ResourceUtil;

import java.util.Hashtable;
import java.util.Map;

abstract public class AbstractTab extends TestRunTab implements IMenuListener {
  private final Image m_suiteIcon = Images.getImage(IMG_SUITE);
  private final Image m_suiteOkeIcon = Images.getImage(IMG_SUITE_OK);
  private final Image m_suiteSkipIcon = Images.getImage(IMG_SUITE_SKIP);
  private final Image m_suiteFailIcon = Images.getImage(IMG_SUITE_FAIL);
  private final Image m_suiteRunIcon = Images.getImage(IMG_SUITE_RUN);

  private final Image m_testHierarchyIcon = Images.getImage(IMG_TEST_HIERARCHY); 
  private final Image m_testIcon = Images.getImage(IMG_TEST);
  private final Image m_testOkeIcon = Images.getImage(IMG_TEST_OK);
  private final Image m_testSkipIcon = Images.getImage(IMG_TEST_SKIP);
  private final Image m_testFailIcon = Images.getImage(IMG_TEST_FAIL);
  private final Image m_testRunIcon = Images.getImage(IMG_TEST_RUN);

  private final static String FORMATTED_MESSAGE = "{0} ( {1}/{2}/{3}/{4} ) ({5} s)";

  // Strings used to store data on the TreeItem nodes
  private static final String DATA_RUN_INFO = "runInfo";
  private static final String DATA_CUMULATED_TIME = "cumulatedTime";
  private static final String DATA_TEXT_FORMAT = "textFormat";

  private Tree m_tree;
  private TestRunnerViewPart m_testRunnerPart;
//  private boolean m_moveSelection = false;

  @Override
  public String getSelectedTestId() {
    return null;
  }

  @Override
  public void createTabControl(CTabFolder tabFolder, TestRunnerViewPart runner) {
    m_testRunnerPart = runner;

    CTabItem hierarchyTab = new CTabItem(tabFolder, SWT.NONE);
    hierarchyTab.setText(getName());
    hierarchyTab.setImage(m_testHierarchyIcon);

    Composite  testTreePanel = new Composite(tabFolder, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    testTreePanel.setLayout(gridLayout);

    GridData gridData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
    testTreePanel.setLayoutData(gridData);

    hierarchyTab.setControl(testTreePanel);
    hierarchyTab.setToolTipText(ResourceUtil.getString(getTooltipKey())); //$NON-NLS-1$

    m_tree = new Tree(testTreePanel, SWT.V_SCROLL | SWT.SINGLE);

//    TreeColumn column1 = new TreeColumn(fTree, SWT.LEFT);
//    column1.setText("Method");
//    TreeColumn column2 = new TreeColumn(fTree, SWT.CENTER);
//    column2.setText("Time");

    gridData = new GridData(GridData.FILL_BOTH
                            | GridData.GRAB_HORIZONTAL
                            | GridData.GRAB_VERTICAL);

    m_tree.setLayoutData(gridData);

    initMenu();
    addListeners();
  }

  private void initMenu() {
    MenuManager menuMgr = new MenuManager();
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(this);

    Menu menu = menuMgr.createContextMenu(m_tree);
    m_tree.setMenu(menu);
  }
  
  private void addListeners() {
    m_tree.addSelectionListener(new SelectionListener() {
        public void widgetSelected(SelectionEvent e) {
          activate();
        }

        public void widgetDefaultSelected(SelectionEvent e) {
          activate();
        }
      });

    m_tree.addDisposeListener(new DisposeListener() {
        public void widgetDisposed(DisposeEvent e) {
          disposeIcons();
        }
      });

    m_tree.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseDoubleClick(MouseEvent e) {
          handleDoubleClick(e);
        }
      });
  }

  void disposeIcons() {
    m_suiteIcon.dispose();
    m_suiteOkeIcon.dispose();
    m_suiteFailIcon.dispose();
    m_suiteSkipIcon.dispose();
    m_suiteRunIcon.dispose();

    m_testHierarchyIcon.dispose();
    m_testIcon.dispose();
    m_testOkeIcon.dispose();
    m_testFailIcon.dispose();
    m_testSkipIcon.dispose();
    m_testRunIcon.dispose();
  }

  void handleDoubleClick(MouseEvent e) {
    RunInfo testInfo = getSelectionRunInfo();

    if(null == testInfo) {
      return;
    }

    if(RunInfo.RESULT_TYPE == testInfo.getType()) {
//      OpenTestAction action = new OpenTestAction(fTestRunnerPart, testInfo.m_className, testInfo.m_methodName);
      OpenTestAction action = new OpenTestAction(m_testRunnerPart, testInfo);
      
      if(action.isEnabled()) {
        action.run();
      }
    }
  }

  /**
   * @return the RunInfo associated with the current selection, or null it not applicable.
   */
  private RunInfo getSelectionRunInfo() {
    TreeItem[] treeItems= m_tree.getSelection();
    
    return treeItems.length == 0 ? null : (RunInfo) treeItems[0].getData(DATA_RUN_INFO);
  }

  /**
   * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
   */
  public void menuAboutToShow(IMenuManager manager) {
    if(m_tree.getSelectionCount() > 0) {
      TreeItem treeItem = m_tree.getSelection()[0];
      RunInfo  testInfo = (RunInfo) treeItem.getData(DATA_RUN_INFO);

      manager.add(new OpenTestAction(m_testRunnerPart, testInfo));
      manager.add(new Separator());
      manager.add(new QuickRunAction(m_testRunnerPart.getLaunchedProject(), 
          m_testRunnerPart.getLastLaunch(),
          testInfo,
          ILaunchManager.RUN_MODE));
      manager.add(new QuickRunAction(m_testRunnerPart.getLaunchedProject(),
          m_testRunnerPart.getLastLaunch(),
          testInfo,
          ILaunchManager.DEBUG_MODE));
      manager.add(new Separator());
      manager.add(new ExpandAllAction());
    }
  }

  protected abstract String getTooltipKey();
  
  protected abstract String getSelectedTestKey();

  private Map<String, TreeItem> m_treeItemMap = Maps.newHashMap();

  @Override
  public void updateTestResult(RunInfo resultInfo) {
    p("New result: " + resultInfo);
    TreeItem ti = m_treeItemMap.get(resultInfo.getId());
    TreeItem parentItem = null;
    if (ti == null) {
      parentItem = maybeCreateParents(resultInfo);
      ti = createTreeItem(parentItem, resultInfo, resultInfo.getTreeLabel());
      registerTreeItem(resultInfo.getId(), ti);
    } else {
      parentItem = ti.getParentItem();
    }
    ti.setImage(getStatusImage(resultInfo.getType(), resultInfo.getStatus()));

    propagateTestResult(parentItem, resultInfo);
  }

  private void propagateTestResult(TreeItem ti, RunInfo childRunInfo) {
    System.out.println("Propagating treeItem:" + ti);
    Float cumulatedTime = (Float) ti.getData(DATA_CUMULATED_TIME);
    if (cumulatedTime == null) {
      cumulatedTime = 0.0f;
    }
    float c = cumulatedTime + childRunInfo.getTime();
    ti.setData(DATA_CUMULATED_TIME, c);
    RunInfo resultInfo = (RunInfo) ti.getData(DATA_RUN_INFO);
//    ti.setText(MessageFormat.format(FORMATTED_MESSAGE,
//        new Object[] {
//            (String) ti.getData(DATA_TEXT_FORMAT),
//            resultInfo.getMethodName(),
//            new Integer(resultInfo.m_passed),
//            new Integer(resultInfo.m_failed),
//            new Integer(resultInfo.m_skipped),
//            new Integer(resultInfo.m_successPercentageFailed),
//            c / 1000
//        })
//    );
    if (ti.getParentItem() != null) {
      propagateTestResult(ti.getParentItem(), childRunInfo);
    }
  }

  private void p(String string) {
    System.out.println("[AbstractTab] " + string);
  }

  private TreeItem createTreeItem(Tree parent, RunInfo runInfo, String text) {
    TreeItem result = new TreeItem(parent, SWT.None);
    return configureTreeItem(result, runInfo, text);
  }

  private TreeItem createTreeItem(TreeItem parent, RunInfo runInfo, String text) {
    TreeItem result = new TreeItem(parent, SWT.None);
    return configureTreeItem(result, runInfo, text);
  }
  
  private TreeItem configureTreeItem(TreeItem result, RunInfo runInfo, String text) {
    result.setExpanded(true);
    result.setData(DATA_RUN_INFO, runInfo);
    result.setText(text);
    return result;
  }

  /**
   * @return the parent tree item for this ResultInfo, possibly creating all the
   * other parents if they don't exist yet.
   */
  private TreeItem maybeCreateParents(RunInfo resultInfo) {
    String suiteId = resultInfo.getSuiteName();
    TreeItem suiteTreeItem = m_treeItemMap.get(suiteId);
    if (suiteTreeItem == null) {
      suiteTreeItem = createTreeItem(m_tree, resultInfo, resultInfo.getSuiteName());
      registerTreeItem(suiteId, suiteTreeItem);
    }

    String testId = suiteId + "." + resultInfo.getTestName();
    TreeItem testTreeItem = m_treeItemMap.get(testId);
    if (testTreeItem == null) {
      testTreeItem = createTreeItem(suiteTreeItem, resultInfo, resultInfo.getTestName());
      registerTreeItem(testId, testTreeItem);
    }

    String classId = testId + "." + resultInfo.getClassName();
    TreeItem classTreeItem = m_treeItemMap.get(classId);
    if (classTreeItem == null) {
      classTreeItem = createTreeItem(testTreeItem, resultInfo, resultInfo.getClassName());
      registerTreeItem(classId, classTreeItem);
    }

    return classTreeItem;
  }

  private Image getStatusImage(int type, int state) {
    if(RunInfo.SUITE_TYPE == type) {
      switch(state) {
        case ITestResult.SUCCESS:
          return m_suiteOkeIcon;
        case ITestResult.FAILURE:
        case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
          return m_suiteFailIcon;
        case ITestResult.SKIP:
          return m_suiteSkipIcon;
      }
    }
    else {
      switch(state) {
        case ITestResult.SUCCESS:
          return m_testOkeIcon;
        case ITestResult.FAILURE:
        case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
          return m_testFailIcon;
        case ITestResult.SKIP:
          return m_testSkipIcon;
      }
    }
    
    return null;
  }

  @Override
  public void aboutToStart() {
    m_tree.removeAll();
    m_treeItemMap = new Hashtable<String, TreeItem>();
//    m_runningItems= new Hashtable<String, List<TreeItem>>();
//    m_duplicateItemsIndex= 0;
//    m_moveSelection = false;
  }

  @Override
  public void activate() {
//    m_moveSelection = false;
    testSelected();
  }

  @Override
  public void setFocus() {
    m_tree.setFocus();
  }

  private void testSelected() {
    m_testRunnerPart.handleTestSelected(getSelectionRunInfo());
  }

  private void registerTreeItem(String id, TreeItem treeItem) {
    m_treeItemMap.put(id, treeItem);
  }

}
