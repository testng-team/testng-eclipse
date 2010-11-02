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


import org.eclipse.swt.custom.CTabFolder;
import org.testng.eclipse.util.ResourceUtil;

/**
 * A TestRunTab is shown as a tab in a tabbed folder.
 */
public abstract class TestRunTab {

  /**
   * Create the tab control
   * @param tabFolder the containing tab folder
   * @param clipboard the clipboard to be used by the tab
   * @param runner the testRunnerViewPart containing the tab folder
   */
  public abstract void createTabControl(CTabFolder tabFolder, TestRunnerViewPart runner);

  /**
   * @return the id of the currently selected node in the tree view or null if no nodes
   * are selected. If the user selects an item on a tab and then switches to another tab,
   * the new tab can then try to make a best effort to keep the same item selected.
   */
  public abstract String getSelectedTestId();

  /**
   * Ask this tab to select the item with the given id, if possible. Used when the user
   * switches tabs.
   */
  public void setSelectedTest(String testId) {
  }

  /**
   * Activates the TestRunView.
   */
  public void activate() {
  }

  /**
   * Sets the focus in the TestRunView.
   */
  public void setFocus() {
  }

  /**
   * Informs that the suite is about to start.
   */
  public void aboutToStart() {
  }

  /**
   * Returns the name of the tab.
   */
  public abstract String getName();

  /**
   * Called by the TestRunnerViewPart whenenver a new test result is received.
   */
  public void updateTestResult(RunInfo resultInfo) {
  }

  /**
   * Select next test failure.
   */
  public void selectNext() {
  }

  /**
   * Select previous test failure.
   */
  public void selectPrevious() {
  }

  protected String getResourceString(String key) {
    return ResourceUtil.getString(key);
  }

  public void updateSearchFilter(String text) {
  }
}
