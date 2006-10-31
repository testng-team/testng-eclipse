/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids - sdavids@gmx.de bugs 26754, 41228
*******************************************************************************/
package org.testng.eclipse.ui;


import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.ResourceUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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

/*
 * A view that shows the contents of a test suite
 * as a tree.
 */
public class TestHierarchyTab extends TestRunTab implements IMenuListener {
  private final Image m_suiteIcon = TestNGPlugin.getImageDescriptor("obj16/suite.gif").createImage(); //$NON-NLS-1$
  private final Image m_suiteOkeIcon = TestNGPlugin.getImageDescriptor("obj16/suiteok.gif").createImage(); //$NON-NLS-1$
  private final Image m_suiteSkipIcon = TestNGPlugin.getImageDescriptor("obj16/suiteskip.gif").createImage(); //$NON-NLS-1$
  private final Image m_suiteFailIcon = TestNGPlugin.getImageDescriptor("obj16/suitefail.gif").createImage(); //$NON-NLS-1$
  private final Image m_suiteRunIcon = TestNGPlugin.getImageDescriptor("obj16/suiterun.gif").createImage(); //$NON-NLS-1$

  private final Image m_testHierarchyIcon = TestNGPlugin.getImageDescriptor("obj16/testhier.gif").createImage(); //$NON-NLS-1$ 
  private final Image m_testIcon = TestNGPlugin.getImageDescriptor("obj16/test.gif").createImage(); //$NON-NLS-1$
  private final Image m_testOkeIcon = TestNGPlugin.getImageDescriptor("obj16/testok.gif").createImage(); //$NON-NLS-1$
  private final Image m_testSkipIcon = TestNGPlugin.getImageDescriptor("obj16/testskip.gif").createImage(); //$NON-NLS-1$
  private final Image m_testFailIcon = TestNGPlugin.getImageDescriptor("obj16/testfail.gif").createImage(); //$NON-NLS-1$
  private final Image m_testRunIcon = TestNGPlugin.getImageDescriptor("obj16/testrun.gif").createImage(); //$NON-NLS-1$

  private final static String FORMATTED_MESSAGE = "{0} ( {1}/{2}/{3}/{4} )";
  
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


  public TestHierarchyTab() {
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#createTabControl(org.eclipse.swt.custom.CTabFolder, net.noco.testng.ui.TestRunnerViewPart)
   */
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
    hierarchyTab.setToolTipText(ResourceUtil.getString("HierarchyRunView.tab.tooltip")); //$NON-NLS-1$

    fTree = new Tree(testTreePanel, SWT.V_SCROLL | SWT.SINGLE);
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

    if(RunInfo.RESULT_TYPE == testInfo.m_type) {
//      OpenTestAction action = new OpenTestAction(fTestRunnerPart, testInfo.m_className, testInfo.m_methodName);
      OpenTestAction action = new OpenTestAction(fTestRunnerPart, testInfo);
      
      if(action.isEnabled()) {
        action.run();
      }
    }
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#getName()
   */
  public String getName() {
    return ResourceUtil.getString("HierarchyRunView.tab.title"); //$NON-NLS-1$
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#getSelectedTestId()
   */
  public String getSelectedTestId() {
    RunInfo testInfo = getTestInfo();
    
    if(testInfo == null) {
      return null;
    }

    return testInfo.m_id;
  }
  
  /**
   * @see net.noco.testng.ui.TestRunTab#activate()
   */
  public void activate() {
    fMoveSelection = false;
    testSelected();
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#setFocus()
   */
  public void setFocus() {
    fTree.setFocus();
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#aboutToStart()
   */
  public void aboutToStart() {
    fTree.removeAll();
    m_treeItemMap = new Hashtable();
    m_runningItems= new Hashtable();
    m_duplicateItemsIndex= 0;
    fMoveSelection = false;
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#setSelectedTest(java.lang.String)
   */
  public void setSelectedTest(String testId) {
    if(null == testId) {
      TestNGPlugin.log(new Status(IStatus.WARNING,
          TestNGPlugin.PLUGIN_ID,
          IStatus.WARNING,
          "[TestHierarchyTab.setSelectedTest] was called with '" + testId + "'",
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
   * @see org.testng.eclipse.ui.TestRunTab#updateEntry(java.lang.String)
   */
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
    
    ti.setImage(getStatusImage(ri.m_type, state));
  }

  /**
   * Called on test results.
   * 
   * @see net.noco.testng.ui.TestRunTab#updateTestResult(net.noco.testng.ui.RunInfo)
   */
  public void updateTestResult(RunInfo resultInfo) {
    TreeItem ti = (TreeItem) getRunningEntry(resultInfo.m_id);
    
    if(null == ti) {
      // probably this is a @Configuration failures
      createNewEntry(resultInfo);
      return;
    }
    
    ti.setData("runinfo", resultInfo);
    ti.setExpanded(true);
    ti.setImage(getStatusImage(resultInfo.m_type, resultInfo.m_status));
    
    if(ITestResult.SUCCESS != resultInfo.m_status) {
      m_failureIds.add((String) ti.getData("testid"));
    }
    
    perpetuateResult(ti.getParentItem(), resultInfo.m_status);
  }
  
  private void createNewEntry(RunInfo runInfo) {
    String enclosingTestId = runInfo.m_suiteName + "." + runInfo.m_testName; 
    TreeItem parentItem = (TreeItem) m_treeItemMap.get(enclosingTestId);
    
    if (null == parentItem) {
      // the failures in beforeSuite/beforeTest are reported before a test context exists
      newTreeEntry(new RunInfo(runInfo.m_suiteName));
      newTreeEntry(new RunInfo(runInfo.m_suiteName, runInfo.m_testName));
      parentItem = (TreeItem) m_treeItemMap.get(enclosingTestId);
    }
    
    TreeItem treeItem = new TreeItem(parentItem, SWT.NONE);
    treeItem.setImage(getStatusImage(runInfo.m_type, runInfo.m_status));
    treeItem.setData("runinfo", runInfo);
    treeItem.setData("testid", runInfo.m_id);
    treeItem.setText(runInfo.getMethodDisplay());
    treeItem.setExpanded(true);
    
    if(ITestResult.SUCCESS != runInfo.m_status) {
      m_failureIds.add(runInfo.m_id);
    }
    
    perpetuateResult(parentItem, runInfo.m_status);
  }
  
  private void perpetuateResult(TreeItem ti, int state) {
    if(null == ti) {
      return;
    }
    
    RunInfo ri = (RunInfo) ti.getData("runinfo");
    if(RunInfo.SUITE_TYPE == ri.m_type
        || RunInfo.TEST_TYPE == ri.m_type) {
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
      String itemName = RunInfo.SUITE_TYPE == ri.m_type ? ri.m_suiteName : ri.m_testName;
      

      ti.setText(MessageFormat.format(FORMATTED_MESSAGE,
          new Object[] {
              itemName,
              new Integer(ri.m_passed),
              new Integer(ri.m_failed),
              new Integer(ri.m_skipped),
              new Integer(ri.m_successPercentageFailed)
          })
      );
      
      perpetuateResult(ti.getParentItem(), state);
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
    
  /**
   * @see net.noco.testng.ui.TestRunTab#newTreeEntry(net.noco.testng.ui.RunInfo)
   */
  public void newTreeEntry(RunInfo treeEntry) {
    TreeItem treeItem = null;
    boolean running= false;
    boolean allowDups= true;
      
      switch(treeEntry.m_type) {
        case RunInfo.SUITE_TYPE:
          if(!m_treeItemMap.containsKey(treeEntry.m_id)) {
            treeItem = new TreeItem(fTree, SWT.NONE);
            treeItem.setImage(m_suiteRunIcon);
            treeItem.setData("runinfo", treeEntry);
            treeItem.setData("testid", treeEntry.m_id);
            treeItem.setText(MessageFormat.format(FORMATTED_MESSAGE,
                new Object[] {
                    treeEntry.m_suiteName,
                    new Integer(treeEntry.m_passed),
                    new Integer(treeEntry.m_failed),
                    new Integer(treeEntry.m_skipped),
                    new Integer(treeEntry.m_successPercentageFailed)
                })
            );
          }

          break;
        case RunInfo.TEST_TYPE:
          if(!m_treeItemMap.containsKey(treeEntry.m_id)) {
            TreeItem suiteItem = (TreeItem) m_treeItemMap.get(treeEntry.m_suiteName);
            treeItem = new TreeItem(suiteItem, SWT.NONE);
            treeItem.setImage(m_testRunIcon);
            treeItem.setData("runinfo", treeEntry);
            treeItem.setData("testid", treeEntry.m_id);
            treeItem.setData("testname", treeEntry.m_testName);
            treeItem.setText(MessageFormat.format(FORMATTED_MESSAGE,
                new Object[] {
                  treeEntry.m_testName,
                  new Integer(treeEntry.m_passed),
                  new Integer(treeEntry.m_failed),
                  new Integer(treeEntry.m_skipped),
                  new Integer(treeEntry.m_successPercentageFailed)
                })
            );
          }
          break;
        case RunInfo.RESULT_TYPE:
          String enclosingTestId = treeEntry.m_suiteName + "." + treeEntry.m_testName; 
          TreeItem testItem = (TreeItem) m_treeItemMap.get(enclosingTestId);
          
          if (null != testItem) {
            running= true;
            allowDups= false;
          }
          else { 
//            System.err.println("[INFO]" + treeEntry.m_id);
            // the failures in beforeSuite/beforeTest are reported before a test context exists
            newTreeEntry(new RunInfo(treeEntry.m_suiteName));
            newTreeEntry(new RunInfo(treeEntry.m_suiteName, treeEntry.m_testName));
            testItem = (TreeItem) m_treeItemMap.get(enclosingTestId);
          }
          
          treeItem = new TreeItem(testItem, SWT.NONE);
          treeItem.setImage(m_testRunIcon);
          treeItem.setData("runinfo", treeEntry);
          treeItem.setData("testid", treeEntry.m_id);
          String parentName= (String) testItem.getData("testname");
          if(treeEntry.m_className.equals(parentName)) {
            treeItem.setText(treeEntry.m_methodName + treeEntry.getParametersDisplay());
          }
          else {
            treeItem.setText(treeEntry.getMethodDisplay());
          }
          
          break;
      }
      
      if(null != treeItem) {
        registerTreeEntry(treeEntry, treeItem, allowDups, running);
//        m_treeItemMap.put(treeEntry.m_id, treeItem);
      }
//    }
  }
  
  private void registerTreeEntry(RunInfo runInfo, TreeItem item, boolean allowDups, boolean running) {
    String itemKey= runInfo.m_id;

    if(!allowDups && m_treeItemMap.containsKey(runInfo.m_id)) {
      m_duplicateItemsIndex++;
      itemKey+= m_duplicateItemsIndex;

      item.setText(item.getText() + "[" + m_duplicateItemsIndex + "]");
      item.setData("testid", itemKey);
    }
    
    if(running) {
      List dups= (List) m_runningItems.get(runInfo.m_id);
      if(null == dups) {
        dups= new ArrayList();
        m_runningItems.put(runInfo.m_id, dups);
      }
      dups.add(item);
    }
    
    m_treeItemMap.put(itemKey, item);
  }
  
  private TreeItem getRunningEntry(String originalId) {
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
  
  /**
   * @see org.eclipse.jdt.internal.junit.ui.ITestRunView#selectNext()
   */
  public void selectNext() {
    TreeItem currentSelection = getInitialSearchSelection();
    String currentId = (String) currentSelection.getData("testid");
    
    int currentIndex = m_failureIds.indexOf(currentId);
    if(++currentIndex <= m_failureIds.size()) {
      setSelectedTest((String) m_failureIds.get(currentIndex));
      testSelected();
    }
  }

  /**
   * @see org.eclipse.jdt.internal.junit.ui.ITestRunView#selectPrevious()
   */
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

      if(RunInfo.RESULT_TYPE == testInfo.m_type) {
//        manager.add(new OpenTestAction(fTestRunnerPart, testInfo.m_className, testInfo.m_methodName));
        manager.add(new OpenTestAction(fTestRunnerPart, testInfo));
      }
      else {
        manager.add(new OpenTestAction(fTestRunnerPart, testInfo.m_testName));
        manager.add(new Separator());
        manager.add(new ExpandAllAction());
      }
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
  
  private static void ppp(final Object msg) {
//    System.out.println("[TestHierachy]:- " + msg);
  }
}
