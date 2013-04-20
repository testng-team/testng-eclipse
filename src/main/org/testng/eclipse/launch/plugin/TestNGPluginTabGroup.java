package org.testng.eclipse.launch.plugin;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaArgumentsTab;
import org.eclipse.pde.ui.launcher.ConfigurationTab;
import org.eclipse.pde.ui.launcher.PluginsTab;
import org.eclipse.pde.ui.launcher.TracingTab;

/**
 * Creates and initializes the tabs for the Plug-in TestNG test launch configuration.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p>
 */
public class TestNGPluginTabGroup extends AbstractLaunchConfigurationTabGroup {
  
  /*
   * (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
   */
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
				new TestTab(), new PluginTestNGMainTab(),
				new JavaArgumentsTab(), new PluginsTab(), 
				new ConfigurationTab(true), new TracingTab(), 
				new EnvironmentTab(), new CommonTab() };
		setTabs(tabs);
	}
	
}
