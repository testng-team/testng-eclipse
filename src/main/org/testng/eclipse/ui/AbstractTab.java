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
import org.testng.eclipse.collections.Maps;
import org.testng.eclipse.util.ResourceUtil;

import java.text.MessageFormat;
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

  private Tree m_tree;
  private TestRunnerViewPart m_testRunnerPart;

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
    RunInfo testInfo = getTestInfo();

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

  private RunInfo getTestInfo() {
    TreeItem[] treeItems= m_tree.getSelection();
    
    return treeItems.length == 0 ? null : (RunInfo) treeItems[0].getData("runinfo");
  }

  /**
   * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
   */
  public void menuAboutToShow(IMenuManager manager) {
    if(m_tree.getSelectionCount() > 0) {
      TreeItem treeItem = m_tree.getSelection()[0];
      RunInfo  testInfo = (RunInfo) treeItem.getData("runinfo");

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
    if (ti == null) {
      TreeItem parentItem = maybeCreateParents(resultInfo);
      ti = new TreeItem(parentItem, SWT.None);
      Float cumulatedTime = (Float) ti.getData("cumulatedTime");
      if (cumulatedTime == null) {
        cumulatedTime = 0.0f;
      }
      float c = 0.0f;
//      float c = cumulatedTime + childRunInfo.getTime();
//      ti.setData("cumulatedTime", c);
      ti.setText(MessageFormat.format(FORMATTED_MESSAGE,
          new Object[] {
              resultInfo.getMethodName(),
              new Integer(resultInfo.m_passed),
              new Integer(resultInfo.m_failed),
              new Integer(resultInfo.m_skipped),
              new Integer(resultInfo.m_successPercentageFailed),
              c / 1000
          })
      );
      registerTreeItem(resultInfo.getId(), ti);
    }
  }

  private void p(String string) {
    System.out.println("[AbstractTab] " + string);
  }

  private TreeItem createTreeItem(Tree parent) {
    TreeItem result = new TreeItem(parent, SWT.None);
    return configureTreeItem(result);
  }

  private TreeItem createTreeItem(TreeItem parent) {
    TreeItem result = new TreeItem(parent, SWT.None);
    return configureTreeItem(result);
  }
  
  private TreeItem configureTreeItem(TreeItem result) {
    result.setExpanded(true);
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
      suiteTreeItem = createTreeItem(m_tree);
      suiteTreeItem.setText(resultInfo.getSuiteName());
      registerTreeItem(suiteId, suiteTreeItem);
    }

    String testId = suiteId + "." + resultInfo.getTestName();
    TreeItem testTreeItem = m_treeItemMap.get(testId);
    if (testTreeItem == null) {
      testTreeItem = createTreeItem(suiteTreeItem);
      testTreeItem.setText(resultInfo.getTestName());
      registerTreeItem(testId, testTreeItem);
    }

    String classId = testId + "." + resultInfo.getClassName();
    TreeItem classTreeItem = m_treeItemMap.get(classId);
    if (classTreeItem == null) {
      classTreeItem = createTreeItem(testTreeItem);
      classTreeItem.setText(resultInfo.getClassName());
      registerTreeItem(classId, classTreeItem);
    }

    return classTreeItem;
  }

  private void registerTreeItem(String id, TreeItem treeItem) {
    m_treeItemMap.put(id, treeItem);
  }

}
