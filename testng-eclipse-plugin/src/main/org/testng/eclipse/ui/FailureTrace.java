/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids: sdavids@gmx.de bug 37333, 26653
 *     Johan Walles: walles@mailblocks.com bug 68737
 *******************************************************************************/
package org.testng.eclipse.ui;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IOpenEventListener;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.PreferenceStoreUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

/**
 * A pane that shows a stack trace of a failed test.
 */
class FailureTrace implements IMenuListener {
  private static final String  FRAME_PREFIX = "at "; //$NON-NLS-1$
  
  private final Image m_stackIcon = TestNGPlugin.getImageDescriptor(
      "obj16/stkfrm_obj.png").createImage(); //$NON-NLS-1$
  private final Image m_exceptionIcon = TestNGPlugin.getImageDescriptor(
      "obj16/exc_catch.png").createImage(); //$NON-NLS-1$

  private final Clipboard fClipboard;
  private Table fTable;
  private TestRunnerViewPart fTestRunner;
  private String fInputTrace;
  private RunInfo fFailure;
  private CompareResultsAction fCompareAction;
  private String fMessage;

  public FailureTrace(Composite parent,
                      TestRunnerViewPart testRunner,
                      ToolBar toolBar) {

    // fill the failure trace viewer toolbar
    ToolBarManager failureToolBarmanager = new ToolBarManager(toolBar);
    fCompareAction = new CompareResultsAction(this);
    fCompareAction.setEnabled(false);
    failureToolBarmanager.add(fCompareAction);
    failureToolBarmanager.update(true);

    fTable = new Table(parent, SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
    fTestRunner = testRunner;

    fClipboard= new Clipboard(parent.getDisplay());

    OpenStrategy handler = new OpenStrategy(fTable);
    handler.addOpenListener(new IOpenEventListener() {
        public void handleOpen(SelectionEvent e) {

          if (fTable.getSelectionIndex() == 0) {
            (new CompareResultsAction(FailureTrace.this)).run();
            return;
          }
          if(fTable.getSelection().length != 0) {
            Action a = createOpenEditorAction(getSelectedText());
            if(a != null) {
              a.run();
            }
          }
        }
      });

    initMenu();

    parent.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
         disposeIcons();
         if(null != fClipboard && fClipboard.isDisposed()) {
           fClipboard.dispose();
         }
      }
    });
  }

    public String getTrace() {
        return fInputTrace;
    }
    
  private void initMenu() {
    MenuManager menuMgr = new MenuManager();
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(this);

    Menu menu = menuMgr.createContextMenu(fTable);
    fTable.setMenu(menu);
  }

  /**
   * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
   */
  public void menuAboutToShow(IMenuManager manager) {
    if(fTable.getSelectionCount() > 0) {
      Action action = createOpenEditorAction(getSelectedText());
      if(null != null) {
        manager.add(action);
      }
      
      manager.add(new TraceCopyAction(FailureTrace.this, fClipboard));
      manager.add(new MessageCopyAction(FailureTrace.this, fClipboard));
    }
  }

  private String getSelectedText() {
    return fTable.getSelection()[0].getText();
  }

  private Action createOpenEditorAction(String traceLine) {
    try {
      String testName = traceLine;
      testName = testName.substring(testName.indexOf(FRAME_PREFIX)); //$NON-NLS-1$
      testName = testName.substring(FRAME_PREFIX.length(), testName.lastIndexOf('(')).trim();
      testName = testName.substring(0, testName.lastIndexOf('.'));

      int innerSeparatorIndex = testName.indexOf('$');
      if(innerSeparatorIndex != -1) {
        testName = testName.substring(0, innerSeparatorIndex);
      }

      String lineNumber = traceLine;
      lineNumber = lineNumber.substring(lineNumber.indexOf(':') + 1, lineNumber.lastIndexOf(')'));

      int    line = Integer.valueOf(lineNumber).intValue();

      //fix for bug 37333
      String cuName = traceLine.substring(traceLine.lastIndexOf('(') + 1,
                                          traceLine.lastIndexOf(':'));

      return new OpenEditorAtLineAction(fTestRunner, cuName, testName, line);
    }
    catch(NumberFormatException e) {
      ;
    }
    catch(IndexOutOfBoundsException e) {
      ;
    }

    return null;
  }

  private void disposeIcons() {
    m_exceptionIcon.dispose();
    m_stackIcon.dispose();
  }

  /**
   * Returns the composite used to present the trace
   */
  Composite getComposite() {
    return fTable;
  }

  /**
   * Shows a TestFailure
   * @param failure the failed test
   */
  public void showFailure(RunInfo failure) {
    if (null == failure || failure.getStackTrace() == null) {
      fCompareAction.setEnabled(false);
      clear();
      return;
    }
    
    fFailure = failure;
    fCompareAction.setEnabled(true);
    String trace = failure.getStackTrace();

    if(fInputTrace == trace) {
      return;
    }
    fInputTrace = trace;
    updateTable(trace);
  }


  private void updateTable(String trace) {
    if((trace == null) || trace.trim().equals("")) { //$NON-NLS-1$
      clear();

      return;
    }
    trace = trace.trim();
    fTable.setRedraw(false);
    fTable.removeAll();
    fillTable(trace);
    fTable.setRedraw(true);
  }

  private void fillTable(String trace) {
    StringReader   stringReader = new StringReader(trace);
    BufferedReader bufferedReader = new BufferedReader(stringReader);
    String         line;
    

    try {    	

      // first line contains the thrown exception
      line = bufferedReader.readLine();
      if(line == null) {
        return;
      }
      boolean stackFound = false;

      TableItem tableItem = new TableItem(fTable, SWT.NONE);
      String    itemLabel = line.replace('\t', ' ');
      tableItem.setText(itemLabel);
      tableItem.setImage(m_exceptionIcon);

      // the stack frames of the trace
      while((line = bufferedReader.readLine()) != null) {
        if (isExcluded(line)) continue;

        itemLabel = line.replace('\t', ' ');
        tableItem = new TableItem(fTable, SWT.NONE);

        // heuristic for detecting a stack frame - works for JDK
        if((itemLabel.indexOf(" at ") >= 0)) { //$NON-NLS-1$
          tableItem.setImage(m_stackIcon);
          if (!stackFound) {
        	  String messagebase = trace.replace('\t', ' ');
        	  fMessage = messagebase.substring(0, messagebase.indexOf(" at "));
        	  stackFound = true;
          }
        }
        tableItem.setText(itemLabel);
      }
      if (!stackFound) fMessage = trace;
    }
    catch(IOException e) {
      TableItem tableItem = new TableItem(fTable, SWT.NONE);
      tableItem.setText(trace);
    }
  }

  /**
   * @return true if this line of the stack trace should not be shown.
   */
  private boolean isExcluded(String line) {
    PreferenceStoreUtil storage =
      new PreferenceStoreUtil(TestNGPlugin.getDefault().getPreferenceStore());

    String projectName = fTestRunner.getLaunchedProject().getProject().getName();
    String excludedStackTraces = storage.getExcludedStackTraces(projectName);
    if (excludedStackTraces.trim().length() > 0) {
      String[] excluded = excludedStackTraces.split(" ");
      for (String e : excluded) {
        if (Pattern.matches(".*" + e + ".*", line)) return true;
      }
    }
    return false;
  }

  /**
   * Clears the non-stack trace info
   */
  public void clear() {
    fTable.removeAll();
    fInputTrace = null;
  }

  public RunInfo getFailedTest() {
    return fFailure;
  }

  public Shell getShell() {
    return fTable.getShell();
  }
  
  public String getMessage() {
	return fMessage;
  }
}
