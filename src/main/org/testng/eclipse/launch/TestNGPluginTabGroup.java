package org.testng.eclipse.launch;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.pde.ui.launcher.ConfigurationTab;
import org.eclipse.pde.ui.launcher.PluginsTab;
import org.eclipse.pde.ui.launcher.TracingTab;

public class TestNGPluginTabGroup extends AbstractLaunchConfigurationTabGroup {
	/**
	 * @see ILaunchConfigurationTabGroup#createTabs(ILaunchConfigurationDialog,
	 *      String)
	 */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new TestNGMainTab(), new PluginTestNGMainTab(),
				new JavaArgumentsTab(), new PluginsTab(false),
				new ConfigurationTab(true), new TracingTab(),
				new EnvironmentTab(), new CommonTab() };
		setTabs(tabs);

	}
}
