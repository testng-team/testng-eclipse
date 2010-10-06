package org.testng.eclipse.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
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
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.ResourceUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static org.testng.eclipse.ui.Images.*;

/*
 * A view that shows the contents of a test suite as a tree.
 */
public abstract class AbstractHierarchyTab extends TestRunTab implements IMenuListener {
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
  
  private Tree fTree;
  
  /**
   * Maps test Ids to TreeItems.
   */
  private Map m_treeItemMap = new Hashtable();
  private Map m_runningItems= new Hashtable();
  
  private int m_duplicateItemsIndex= 0;
  
  /**
   * List of test failure Ids
   */
  private List m_failureIds = new ArrayList();

  private boolean fMoveSelection = false;
  
  private TestRunnerViewPart fTestRunnerPart;
  
  //do not create TreeItems for tests for FailureTab until failure is known
  // may be used in subclass constructor, eg for FailureTab
  private boolean delayItemCreation=false; 
  
  public AbstractHierarchyTab(){}
  
  
  public AbstractHierarchyTab(boolean delayItemCreation){
    this.delayItemCreation = delayItemCreation;
  }

  @Override
  public void createTabControl(CTabFolder tabFolder, TestRunnerViewPart runner) {
    fTestRunnerPart = runner;

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

    fTree = new Tree(testTreePanel, SWT.V_SCROLL | SWT.SINGLE);

//    TreeColumn column1 = new TreeColumn(fTree, SWT.LEFT);
//    column1.setText("Method");
//    TreeColumn column2 = new TreeColumn(fTree, SWT.CENTER);
//    column2.setText("Time");

    gridData = new GridData(GridData.FILL_BOTH
                            | GridData.GRAB_HORIZONTAL
                            | GridData.GRAB_VERTICAL);

    fTree.setLayoutData(gridData);

    initMenu();
    addListeners();
  }

  private void initMenu() {
    MenuManager menuMgr = new MenuManager();
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(this);

    Menu menu = menuMgr.createContextMenu(fTree);
    fTree.setMenu(menu);
  }
  
  private void addListeners() {
    fTree.addSelectionListener(new SelectionListener() {
        public void widgetSelected(SelectionEvent e) {
          activate();
        }

        public void widgetDefaultSelected(SelectionEvent e) {
          activate();
        }
      });

    fTree.addDisposeListener(new DisposeListener() {
        public void widgetDisposed(DisposeEvent e) {
          disposeIcons();
        }
      });

    fTree.addMouseListener(new MouseAdapter() {
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
      OpenTestAction action = new OpenTestAction(fTestRunnerPart, testInfo);
      
      if(action.isEnabled()) {
        action.run();
      }
    }
  }

  @Override
  public String getSelectedTestId() {
    RunInfo testInfo = getTestInfo();
    
    if(testInfo == null) {
      return null;
    }

    return testInfo.getId();
  }

  @Override
  public void activate() {
    fMoveSelection = false;
    testSelected();
  }

  @Override
  public void setFocus() {
    fTree.setFocus();
  }

  @Override
  public void aboutToStart() {
    fTree.removeAll();
    m_treeItemMap = new Hashtable();
    m_runningItems= new Hashtable();
    m_duplicateItemsIndex= 0;
    fMoveSelection = false;
  }

  @Override
  public void setSelectedTest(String testId) {
    if(null == testId) {
      TestNGPlugin.log(new Status(IStatus.WARNING,
          TestNGPlugin.PLUGIN_ID,
          IStatus.WARNING,
          "[" + getSelectedTestKey() + "] was called with '" + testId + "'",
          null));
      return;
    }
    
    TreeItem treeItem = (TreeItem) getTreeEntry(testId); 
    if(null != treeItem) {
      fTree.setSelection(new TreeItem[] { treeItem });
      treeItem.setExpanded(true);
    }
  }
  
  /**
   * Called on suite and test events.
   */
  @Override
  public void updateEntry(String id) {
    TreeItem ti = (TreeItem) m_treeItemMap.get(id);
    
    if(null == ti) {
      return;
    }
    
    RunInfo ri = (RunInfo) ti.getData("runinfo");
    int state = ITestResult.SUCCESS;
    if(ri.m_failed + ri.m_successPercentageFailed > 0) {
      state = ITestResult.FAILURE;
    }
    else if(ri.m_skipped > 0) {
      state = ITestResult.SKIP;
    }

    onPostUpdate(ti, state);
    ti.setImage(getStatusImage(ri.getType(), state));
  }

  /**
   * Called after an item has been updated, meant to be overridden by subclasses
   */
  protected void onPostUpdate(TreeItem ti, int state) {
  }

  /**
   * Called on test results.
   */
  @Override
  public void updateTestResult(RunInfo resultInfo) {

    TreeItem ti = (TreeItem) getRunningEntry(resultInfo.getId(), resultInfo.getTestDescription());
//    System.out.println("treeItem:" + ti + " resultInfo:" + resultInfo);
    
    if(null == ti) {
      // probably this is a @Configuration failures
      // or else the FailureTab is waiting to do the creating
      assert resultInfo.getStatus() == 2;
      ti= createFailedEntry(resultInfo);
     // updateView(ti);
     // return;
    }

    ti.setData("runinfo", resultInfo);
    ti.setExpanded(true);
    ti.setImage(getStatusImage(resultInfo.getType(), resultInfo.getStatus()));
    ti.setText(resultInfo.getTreeLabel());
    
    if(ITestResult.SUCCESS != resultInfo.getStatus()) {
      m_failureIds.add((String) ti.getData("testid"));
    }
    
    propagateResult(ti.getParentItem(), resultInfo);
    updateView(ti);
  }
  
  private void updateView(TreeItem ti) {
    // TESTNG-157: scroll latest marked test to visible in the view
    ti.setExpanded(true);
    fTree.setSelection(ti);    
  }
  
  private TreeItem createFailedEntry(RunInfo runInfo) {
    String enclosingTestId = runInfo.getTestFQN(); 
    TreeItem parentItem = (TreeItem) m_treeItemMap.get(enclosingTestId);
    
    if (null == parentItem) {
      // the failures in beforeSuite/beforeTest are reported before a test context exists
      newTreeEntry(new RunInfo(runInfo.getSuiteName()));
      newTreeEntry(new RunInfo(runInfo.getSuiteName(), runInfo.getTestName()));
      parentItem = (TreeItem) m_treeItemMap.get(enclosingTestId);
    }
    
    TreeItem treeItem = testTreeItem(parentItem, runInfo);
    
    if(ITestResult.SUCCESS != runInfo.getStatus()) {
      m_failureIds.add(runInfo.getId());
    }
    
    propagateResult(parentItem, runInfo);
    
    return treeItem;
  }
  
  private TreeItem createNewTreeItem(TreeItem parentItem, RunInfo runInfo ) {
	  if (parentItem == null) {
		  throw new IllegalArgumentException ("parentItem must not be null");
	  }
	  TreeItem treeItem = new TreeItem(parentItem, SWT.NONE);
	  treeItem.setImage(getStatusImage(runInfo.getType(), runInfo.getStatus()));
	  treeItem.setData("runinfo", runInfo);
	  treeItem.setData("testid", runInfo.getId());
	  String testname = runInfo.getTestName();
	  if (testname != null) {
		  treeItem.setData("testname", testname);
	  }
	  String testdesc = runInfo.getTestDescription();
      if (testdesc != null) {
        treeItem.setData("testdesc", testdesc);
      }
	  treeItem.setExpanded(true);
	  return treeItem;
  }
  
  private void propagateResult(TreeItem ti, RunInfo childRunInfo) {
    int state = childRunInfo.getStatus();
    if(null == ti) {
      return;
    }
    
    RunInfo ri = (RunInfo) ti.getData("runinfo");
    if(RunInfo.SUITE_TYPE == ri.getType()
        || RunInfo.TEST_TYPE == ri.getType()) {
      switch(state) {
        case ITestResult.SUCCESS:
          ri.m_passed++;
          break;
        case ITestResult.FAILURE:
          ri.m_failed++;
          break;
        case ITestResult.SKIP:
          ri.m_skipped++;
          break;
        case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
          ri.m_successPercentageFailed++;
          break;
      }
      
      ti.setExpanded(true);
      String itemName = RunInfo.SUITE_TYPE == ri.getType() ? ri.getSuiteName() : ri.getTestName();
      
      Float cumulatedTime = (Float) ti.getData("cumulatedTime");
      if (cumulatedTime == null) {
        cumulatedTime = 0.0f;
      }
      float c = cumulatedTime + childRunInfo.getTime();
      ti.setData("cumulatedTime", c);

      ti.setText(MessageFormat.format(FORMATTED_MESSAGE,
          new Object[] {
              itemName,
              new Integer(ri.m_passed),
              new Integer(ri.m_failed),
              new Integer(ri.m_skipped),
              new Integer(ri.m_successPercentageFailed),
              c / 1000
          })
      );
      
      propagateResult(ti.getParentItem(), childRunInfo);
    }
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
  public void newTreeEntry(RunInfo treeEntry) {
    TreeItem treeItem = null;
    boolean running= false;
    boolean allowDups= true;
      
      switch(treeEntry.getType()) {
        case RunInfo.SUITE_TYPE:
          if(!m_treeItemMap.containsKey(treeEntry.getId())) {
            treeItem = new TreeItem(fTree, SWT.NONE);
            treeItem.setData("runinfo", treeEntry);
            treeItem.setData("testid", treeEntry.getId());
            configureTreeItem(treeEntry, treeItem, m_suiteRunIcon, treeEntry.getSuiteName());
          }
          break;

        case RunInfo.TEST_TYPE:
          if(!m_treeItemMap.containsKey(treeEntry.getId())) {
            TreeItem suiteItem = (TreeItem) m_treeItemMap.get(treeEntry.getSuiteName());            
            treeItem = createNewTreeItem(suiteItem, treeEntry);
            configureTreeItem(treeEntry, treeItem, m_testRunIcon, treeEntry.getTestName());
          }
          break;

        case RunInfo.RESULT_TYPE:
          String enclosingTestId = treeEntry.getTestFQN(); 
          TreeItem testItem = (TreeItem) m_treeItemMap.get(enclosingTestId);
          
          if (null != testItem) {
            running= true;
            allowDups= false;
          }
          else { 
            // the failures in beforeSuite/beforeTest are reported before a test context exists
            newTreeEntry(new RunInfo(treeEntry.getSuiteName()));
            newTreeEntry(new RunInfo(treeEntry.getSuiteName(), treeEntry.getTestName()));
            testItem = (TreeItem) m_treeItemMap.get(enclosingTestId);
          }
          // if it's for failure tab, at this point do not create a TreeItem
          if (!delayItemCreation){
            treeItem = testTreeItem(testItem, treeEntry);
          } else {
            ppp("Delaying creation of " + testItem + " entry:" + treeEntry);
          }
          break;
      }
      
      if(null != treeItem) {
        registerTreeEntry(treeEntry, treeItem, allowDups, running);
      }
  }


  private void configureTreeItem(RunInfo treeEntry, TreeItem treeItem, Image icon, String name) {
    treeItem.setImage(icon);
    treeItem.setText(MessageFormat.format(FORMATTED_MESSAGE,
        new Object[] {
            name,
            new Integer(treeEntry.m_passed),
            new Integer(treeEntry.m_failed),
            new Integer(treeEntry.m_skipped),
            new Integer(treeEntry.m_successPercentageFailed)
        })
    );
  }
  
  private TreeItem testTreeItem(TreeItem parent, RunInfo treeEntry){
    ppp("Creating on " + this + " parent:" + parent + " entry:" + treeEntry);
    TreeItem result = createNewTreeItem(parent, treeEntry);
    result.setImage(m_testRunIcon);
    result.setText(treeEntry.getTreeLabel());
//    result.setText(new String[] { treeEntry.getTreeLabel(), "foo", "bar" });
//    result.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));

    return result;
  }
  
  private void registerTreeEntry(RunInfo runInfo, TreeItem item, boolean allowDups, boolean running) {
	
    String itemKey= runInfo.getId();

    if(!allowDups && m_treeItemMap.containsKey(runInfo.getId())) {
      TreeItem ti= (TreeItem) m_treeItemMap.get(runInfo.getId());
      RunInfo ri= (RunInfo) ti.getData("runinfo");
      if(runInfo.getTestDescription().equals(ri.getTestDescription())) {
        m_duplicateItemsIndex++;
        itemKey+= m_duplicateItemsIndex;
  
        item.setText(item.getText() + "[" + m_duplicateItemsIndex + "]");
        item.setData("testid", itemKey);
      }
    }
    
    if(running) {
      List dups= (List) m_runningItems.get(runInfo.getId());
      if(null == dups) {
        dups= new ArrayList();
        m_runningItems.put(runInfo.getId(), dups);
      }
      dups.add(item);
    }
    
    m_treeItemMap.put(itemKey, item);
  }
  
  private TreeItem getRunningEntry(String originalId, String testdesc) {
    if(m_runningItems.containsKey(originalId)) {
      List dups= (List) m_runningItems.get(originalId);
      if(dups.size() == 1) {
        m_runningItems.remove(originalId);
        return (TreeItem) dups.get(0);
      }
      else {
        java.util.Iterator it = dups.iterator();
        while (it.hasNext()) {
          TreeItem next = (TreeItem)it.next();
          RunInfo ri= (RunInfo) next.getData("runinfo");
          if (testdesc != null & ri.getTestDescription().equals(testdesc)) {
            return next;
          }
        }
        // if we didn't find a match using test desc ..
        return (TreeItem) dups.remove(0);
      }
    }

    return null;
  }
  
  private TreeItem getTreeEntry(String originalId) {
    if(m_runningItems.containsKey(originalId)) {
      List dups= (List) m_runningItems.get(originalId);
      if(dups.size() == 1) {
        m_runningItems.remove(originalId);
        return (TreeItem) dups.get(0);
      }
      else {
        return (TreeItem) dups.remove(0);
      }
    }
    else {
      return (TreeItem) m_treeItemMap.get(originalId);
    }
  }
  
  private TreeItem getInitialSearchSelection() {
    TreeItem[] treeItems= fTree.getSelection(); 
    TreeItem selection= null;
    
    if(treeItems.length == 0) {  
      selection= fTree.getItems()[0];
    }
    else {
      selection= treeItems[0];
    }
    
    return selection;
  }

  @Override
  public void selectNext() {
    TreeItem currentSelection = getInitialSearchSelection();
    String currentId = (String) currentSelection.getData("testid");
    
    int currentIndex = m_failureIds.indexOf(currentId);
    if(++currentIndex <= m_failureIds.size()) {
      setSelectedTest((String) m_failureIds.get(currentIndex));
      testSelected();
    }
  }

  @Override
  public void selectPrevious() {
    TreeItem currentSelection = getInitialSearchSelection();
    String currentId = (String) currentSelection.getData("testid");
    
    int currentIndex = m_failureIds.indexOf(currentId);
    if(--currentIndex >= 0 && currentIndex < m_failureIds.size()) {
      setSelectedTest((String) m_failureIds.get(currentIndex));
      testSelected();
    }
  }
  
  /**
   * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
   */
  public void menuAboutToShow(IMenuManager manager) {
	  if(fTree.getSelectionCount() > 0) {
		  TreeItem treeItem = fTree.getSelection()[0];
		  RunInfo  testInfo = (RunInfo) treeItem.getData("runinfo");

		  manager.add(new OpenTestAction(fTestRunnerPart, testInfo));
		  manager.add(new Separator());
		  manager.add(new QuickRunAction(fTestRunnerPart.getLaunchedProject(), 
				  fTestRunnerPart.getLastLaunch(),
				  testInfo,
				  ILaunchManager.RUN_MODE));
		  manager.add(new QuickRunAction(fTestRunnerPart.getLaunchedProject(),
				  fTestRunnerPart.getLastLaunch(),
				  testInfo,
				  ILaunchManager.DEBUG_MODE));
		  manager.add(new Separator());
		  manager.add(new ExpandAllAction());
	  }
  }
  
  
  private RunInfo getTestInfo() {
    TreeItem[] treeItems= fTree.getSelection();
    
    return treeItems.length == 0 ? null : (RunInfo) treeItems[0].getData("runinfo");
  }
  
  private void testSelected() {
    fTestRunnerPart.handleTestSelected(getTestInfo());
  }

  protected void expandAll() {
    TreeItem[] treeItems = fTree.getSelection();
    fTree.setRedraw(false);
    for(int i = 0; i < treeItems.length; i++) {
      expandAll(treeItems[i]);
    }
    fTree.setRedraw(true);
  }

  private void expandAll(TreeItem item) {
    item.setExpanded(true);

    TreeItem[] items = item.getItems();
    for(int i = 0; i < items.length; i++) {
      expandAll(items[i]);
    }
  }


  private class ExpandAllAction extends Action {
    public ExpandAllAction() {
      setText(ResourceUtil.getString("ExpandAllAction.text")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("ExpandAllAction.tooltip")); //$NON-NLS-1$
    }

    public void run() {
      expandAll();
    }
  }
  
  protected String getResourceString(String key) {
	  return ResourceUtil.getString(key);
  }

  private static final boolean DEBUG = false;

  private static void ppp(final Object msg) {
    if (DEBUG) {
      System.out.println("[AbstractHierarchyTab] " + msg);
    }
  }
  
  protected abstract String getTooltipKey();
  
  protected abstract String getSelectedTestKey();

  @Override
  public abstract String getName();
}
