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

import org.eclipse.jface.action.Action;

class ShowNextFailureAction extends Action {
	
	private TestRunnerViewPart fPart;

	public ShowNextFailureAction(TestRunnerViewPart part) {
		super(ResourceUtil.getString("ShowNextFailureAction.label"));  //$NON-NLS-1$
		setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("dlcl16/select_next.png")); //$NON-NLS-1$
		setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/select_next.png")); //$NON-NLS-1$
		setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/select_next.png")); //$NON-NLS-1$
		setToolTipText(ResourceUtil.getString("ShowNextFailureAction.tooltip")); //$NON-NLS-1$
		fPart= part;
	}
	
	public void run() {
		fPart.selectNextFailure();
	}
}
