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

import static org.testng.eclipse.ui.Images.IMG_COMPARE;
import static org.testng.eclipse.ui.Images.IMG_COMPARE_DISABLED;
import static org.testng.eclipse.ui.Images.IMG_COMPARE_HOVER;

import org.eclipse.jface.action.Action;
import org.testng.eclipse.util.ResourceUtil;

/**
 * Action to enable/disable stack trace filtering.
 */
public class CompareResultsAction extends Action {
  private FailureTrace fView;

    public CompareResultsAction(FailureTrace view) {
      super(ResourceUtil.getString("CompareResultsAction.label"));   //$NON-NLS-1$
      setDescription(ResourceUtil.getString("CompareResultsAction.description"));   //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("CompareResultsAction.tooltip"));  //$NON-NLS-1$
      
      setDisabledImageDescriptor(Images.getImageDescriptor(IMG_COMPARE_DISABLED));
      setHoverImageDescriptor(Images.getImageDescriptor(IMG_COMPARE_HOVER));
      setImageDescriptor(Images.getImageDescriptor(IMG_COMPARE));

      fView= view;
    }

  /*
   * @see Action#actionPerformed
   */
  public void run() {
    CompareResultDialog dialog= new CompareResultDialog(fView.getShell(), fView.getFailedTest());
    dialog.create();
    dialog.open();
  }
}


