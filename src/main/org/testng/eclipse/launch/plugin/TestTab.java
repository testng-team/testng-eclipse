/*******************************************************************************
 * Copyright (c) 2009, 2011 ThoughtWorks, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ketan Padegaonkar - initial API and implementation
 *******************************************************************************/
package org.testng.eclipse.launch.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.testng.eclipse.launch.TestNGMainTab;

/**
 * The launch configuration tab for TestNG Plug-in Tests. This tab enhances the 
 * {@link TestNGMainTab} to allow for tests to (optionally) 
 * run on a non-UI thread.
 * 
 * <p>
 * This class may be instantiated but is not intended to be subclassed.
 * </p>
 * @since 3.5
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings("restriction")
public class TestTab extends AbstractLaunchConfigurationTab {
  
	private ILaunchConfigurationDialog fLaunchConfigurationDialog;

	private final TestNGMainTab testngLaunchTab;
	private Button runInUIThread;

	/**
	 * Constructor to create a new junit test tab
	 */
	public TestTab() {
		this.testngLaunchTab = new TestNGMainTab();
	}

	public void createControl(Composite parent) {
		testngLaunchTab.createControl(parent);

		Composite composite = (Composite) getControl();
		createSpacer(composite);
		createRunInUIThreadGroup(composite);
	}

	private void createRunInUIThreadGroup(Composite comp) {
		runInUIThread = new Button(comp, SWT.CHECK);
		runInUIThread.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
		runInUIThread.setText(PDEUIMessages.PDEJUnitLaunchConfigurationTab_Run_Tests_In_UI_Thread);
		GridDataFactory.fillDefaults().span(2, 0).grab(true, false).applyTo(runInUIThread);
	}

	private void createSpacer(Composite comp) {
		Label label = new Label(comp, SWT.NONE);
		GridDataFactory.fillDefaults().span(3, 0).applyTo(label);
	}

	public void initializeFrom(ILaunchConfiguration config) {
		testngLaunchTab.initializeFrom(config);
		updateRunInUIThreadGroup(config);
	}

	private void updateRunInUIThreadGroup(ILaunchConfiguration config) {
		boolean shouldRunInUIThread = true;
		try {
			shouldRunInUIThread = config.getAttribute(IPDELauncherConstants.RUN_IN_UI_THREAD, true);
		} catch (CoreException ce) {
		}
		runInUIThread.setSelection(shouldRunInUIThread);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		testngLaunchTab.performApply(config);
		boolean selection = runInUIThread.getSelection();
		config.setAttribute(IPDELauncherConstants.RUN_IN_UI_THREAD, selection);
	}

	public String getId() {
		return IPDELauncherConstants.TAB_TEST_ID;
	}

	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		testngLaunchTab.activated(workingCopy);
	}

	public boolean canSave() {
		return testngLaunchTab.canSave();
	}

	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
		testngLaunchTab.deactivated(workingCopy);
	}

	public void dispose() {
		testngLaunchTab.dispose();
	}

	public String getErrorMessage() {
		return testngLaunchTab.getErrorMessage();
	}

	public Image getImage() {
		return testngLaunchTab.getImage();
	}

	public String getMessage() {
		return testngLaunchTab.getMessage();
	}

	public String getName() {
		return testngLaunchTab.getName();
	}

	public boolean isValid(ILaunchConfiguration config) {
		return testngLaunchTab.isValid(config);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		testngLaunchTab.setDefaults(config);
	}

	public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
		testngLaunchTab.setLaunchConfigurationDialog(dialog);
		this.fLaunchConfigurationDialog = dialog;
	}

}
