/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.testng.eclipse.ui;


import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.ResourceUtil;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.ILaunchManager;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.testng.ITestResult;


/**
 * A tab presenting the failed and successPercentageFailures tests in a table.
 * <P/>
 * Original idea from org.eclipse.jdt.internal.junit.ui.FailureTab
 * 
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class FailureTab extends TestRunTab implements IMenuListener {
  private Table              m_table;
  private TestRunnerViewPart fRunnerViewPart;
  private boolean            fMoveSelection = false;
  private Map m_tableItems = new HashMap();

  private final Image m_failIcon = TestNGPlugin.getImageDescriptor("obj16/testfail.gif").createImage(); //$NON-NLS-1$
  private final Image m_spFailIcon = TestNGPlugin.getImageDescriptor("obj16/testfail.gif").createImage(); //$NON-NLS-1$
  private final Image fFailureTabIcon = TestNGPlugin.getImageDescriptor("obj16/failures.gif").createImage(); //$NON-NLS-1$

  public FailureTab() {
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#createTabControl(org.eclipse.swt.custom.CTabFolder, net.noco.testng.ui.TestRunnerViewPart)
   */
  public void createTabControl(CTabFolder tabFolder, TestRunnerViewPart runner) {
    fRunnerViewPart = runner;

    CTabItem failureTab = new CTabItem(tabFolder, SWT.NONE);
    failureTab.setText(getName());
    failureTab.setImage(fFailureTabIcon);

    Composite  composite = new Composite(tabFolder, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    composite.setLayout(gridLayout);

    GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                                     | GridData.VERTICAL_ALIGN_FILL
                                     | GridData.GRAB_HORIZONTAL
                                     | GridData.GRAB_VERTICAL);
    composite.setLayoutData(gridData);

    m_table = new Table(composite, SWT.NONE);
    gridLayout = new GridLayout();
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    m_table.setLayout(gridLayout);

    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL 
                            | GridData.VERTICAL_ALIGN_FILL
                            | GridData.GRAB_HORIZONTAL 
                            | GridData.GRAB_VERTICAL);
    m_table.setLayoutData(gridData);

    failureTab.setControl(composite);
    failureTab.setToolTipText(ResourceUtil.getString("FailureRunView.tab.tooltip")); //$NON-NLS-1$

    initMenu();
    addListeners();
  }

  private void initMenu() {
    MenuManager menuMgr = new MenuManager();
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(this);
    Menu menu = menuMgr.createContextMenu(m_table);
    m_table.setMenu(menu);
  }
  
  private void addListeners() {
    m_table.addSelectionListener(new SelectionListener() {
        public void widgetSelected(SelectionEvent e) {
          activate();
        }

        public void widgetDefaultSelected(SelectionEvent e) {
          activate();
        }
      });

    m_table.addDisposeListener(new DisposeListener() {
        public void widgetDisposed(DisposeEvent e) {
          disposeIcons();
        }
      });

    m_table.addMouseListener(new MouseAdapter() {
        public void mouseDoubleClick(MouseEvent e) {
          handleDoubleClick(e);
        }

        public void mouseDown(MouseEvent e) {
          activate();
        }

        public void mouseUp(MouseEvent e) {
          activate();
        }
      });
  }

  void handleDoubleClick(MouseEvent e) {
    if(m_table.getSelectionCount() > 0) {
      TableItem item = m_table.getItem(m_table.getSelectionIndex());
      RunInfo   info = (RunInfo) item.getData();

      new OpenTestAction(fRunnerViewPart, info.m_className, info.m_methodName).run();
    }
  }
  
  private void disposeIcons() {
    m_failIcon.dispose();
    m_spFailIcon.dispose();
    fFailureTabIcon.dispose();
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#getSelectedTestId()
   */
  public String getSelectedTestId() {
    int index = m_table.getSelectionIndex();
    if(index == -1) {
      return null;
    }

    return ((RunInfo) m_table.getItem(index).getData()).m_id;
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#activate()
   */
  public void activate() {
    fMoveSelection = false;
    if(m_table.getSelectionCount() > 0) {
      TableItem item = m_table.getItem(m_table.getSelectionIndex());
      RunInfo   info = (RunInfo) item.getData();
//      System.out.println("[FailureTab]:" + info);
      fRunnerViewPart.handleTestSelected(info);
    }
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#setFocus()
   */
  public void setFocus() {
    m_table.setFocus();
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#aboutToStart()
   */
  public void aboutToStart() {
    fMoveSelection = false;
    m_table.removeAll();
    m_tableItems = new HashMap();
  }
  
  /**
   * @see net.noco.testng.ui.TestRunTab#getName()
   */
  public String getName() {
    return ResourceUtil.getString("FailureRunView.tab.title"); //$NON-NLS-1$
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#setSelectedTest(java.lang.String)
   */
  public void setSelectedTest(String testId) {
    TableItem ti = (TableItem) m_tableItems.get(testId);
    
    if(null == ti) {
      return;
    }

    m_table.showItem(ti);
    m_table.setSelection(new TableItem[] {ti});
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#updateTestResult(net.noco.testng.ui.RunInfo)
   */
  public void updateTestResult(RunInfo treeEntry) {
    if(null == treeEntry) {
//      System.out.println("[XXX:updateTestResult]: IS NULL");
      return;
    }

    if(RunInfo.RESULT_TYPE == treeEntry.m_type
        && ITestResult.SUCCESS != treeEntry.m_status
        && ITestResult.SKIP != treeEntry.m_status) {
      TableItem ti = new TableItem(m_table, SWT.NONE);
      ti.setData(treeEntry);
      ti.setText(treeEntry.m_className + "." + treeEntry.m_methodName);
      ti.setImage(getImage(treeEntry.m_status));
      m_tableItems.put(treeEntry.m_id, ti);
    }
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#updateEntry(java.lang.String)
   */
  public void updateEntry(String id) {
  }
  
  /**
   * A new tree entry got posted.
   */
  public void newTreeEntry(RunInfo treeEntry) {
  }

  private Image getImage(int state) {
    switch(state) {
      case ITestResult.FAILURE:
        return m_failIcon;
      case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
        return m_spFailIcon;
    }
    
    return null;
  }
  
  /**
   * @see net.noco.testng.ui.TestRunTab#selectNext()
   */
  public void selectNext() {
    if(m_table.getItemCount() == 0) {
      return;
    }

    int index = m_table.getSelectionIndex();
    if(index == -1) {
      index = 0;
    }

    if(fMoveSelection) {
      index = Math.min(m_table.getItemCount() - 1, index + 1);
    }
    else {
      fMoveSelection = true;
    }

    selectTest(index);
  }

  /**
   * @see net.noco.testng.ui.TestRunTab#selectPrevious()
   */
  public void selectPrevious() {
    if(m_table.getItemCount() == 0) {
      return;
    }

    int index = m_table.getSelectionIndex();
    if(index == -1) {
      index = m_table.getItemCount() - 1;
    }

    if(fMoveSelection) {
      index = Math.max(0, index - 1);
    }
    else {
      fMoveSelection = true;
    }
    
    selectTest(index);
  }

  private void selectTest(int index) {
    TableItem item = m_table.getItem(index);
    RunInfo   info = (RunInfo) item.getData();
    fRunnerViewPart.showTest(info);
  }
  
  /**
   * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
   */
  public void menuAboutToShow(IMenuManager manager) {
    if(m_table.getSelectionCount() > 0) {
      TableItem item = m_table.getItem(m_table.getSelectionIndex());
      RunInfo   info = (RunInfo) item.getData();

      if(null != info.m_className) {
        manager.add(new OpenTestAction(fRunnerViewPart, info.m_className, info.m_methodName));
        manager.add(new Separator());
        manager.add(new QuickRunAction(fRunnerViewPart.getLaunchedProject(), 
                                       fRunnerViewPart.getLastLaunch(),
                                       info.m_className,
                                       info.m_methodName,
                                       ILaunchManager.RUN_MODE));
        manager.add(new QuickRunAction(fRunnerViewPart.getLaunchedProject(),
                                       fRunnerViewPart.getLastLaunch(),
                                       info.m_className,
                                       info.m_methodName,
                                       ILaunchManager.DEBUG_MODE));
      }
    }
  }

}
