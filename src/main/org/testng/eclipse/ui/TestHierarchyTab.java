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


/*
 * A view that shows the contents of a test suite
 * as a tree.
 */
public class TestHierarchyTab extends AbstractHierarchyTab  {
   
  protected String getTooltipKey() {
	  return "HierarchyRunView.tab.tooltip";
  }
 
  /**
   * @see net.noco.testng.ui.TestRunTab#getName()
   */
  public String getName() {
    return getResourceString("HierarchyRunView.tab.title"); //$NON-NLS-1$
  }

  protected String getSelectedTestKey(){
	  return "[TestHierarchyTab.setSelectedTest]";
  }
 
  
}
