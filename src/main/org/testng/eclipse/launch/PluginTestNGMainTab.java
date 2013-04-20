package org.testng.eclipse.launch;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.pde.ui.launcher.MainTab;

public class PluginTestNGMainTab extends MainTab {
	/**
	 * Overrides the implementation of the basis MainTab.
	 */
	protected void createProgramBlock() {
		fProgramBlock = new TestNGProgramBlock(this);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		fDataBlock.setDefaults(config, true);
		fProgramBlock.setDefaults(config);
		fJreBlock.setDefaults(config);
	}
}
