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

import org.eclipse.swt.widgets.TreeItem;
import org.testng.ITestResult;


/*
 * A view that shows the contents of a test suite as a tree, 
 * with the additional property of showing only the failed 
 * tests.
 */
public class FailureTab extends AbstractHierarchyTab  {
  
  public FailureTab() {
    super(true /* delay creation */);
  }
   
  @Override
  protected String getTooltipKey() {
    return "FailureRunView.tab.tooltip";
  }
 
  @Override
  public String getName() {
    return getResourceString("FailureRunView.tab.title"); //$NON-NLS-1$
  }

  @Override
  protected String getSelectedTestKey() {
    return "[FailureTab.setSelectedTest]";
  }

  @Override
  public void updateTestResult(RunInfo treeEntry) {
    if (null == treeEntry) {
      return;
    }

    if (RunInfo.RESULT_TYPE == treeEntry.getType()) {
      if (ITestResult.SUCCESS != treeEntry.getStatus() && ITestResult.SKIP != treeEntry.getStatus()) {
        super.updateTestResult(treeEntry);
      }
    }
  }

  @Override
  protected void onPostUpdate(TreeItem ti, int state) {
    // Horrible hack to remove all the SUCCESS nodes from our view. It's a hack because they
    // shouldn't have been added there in the first place. Without this hack, this tab
    // will still show green tree items for tests that have no errors.
    if (state == ITestResult.SUCCESS) {
      ti.dispose();
    }
  }

}
