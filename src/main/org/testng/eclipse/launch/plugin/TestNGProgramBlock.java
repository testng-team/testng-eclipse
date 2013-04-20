package org.testng.eclipse.launch.plugin;

import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.launching.launcher.LauncherUtils;
import org.eclipse.pde.internal.ui.launcher.ProgramBlock;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;

@SuppressWarnings({"restriction", "rawtypes", "unchecked"})
public class TestNGProgramBlock extends ProgramBlock {
	public TestNGProgramBlock(AbstractLauncherTab tab) {
		super(tab);
	}

	protected String getApplicationAttribute() {
		return IPDELauncherConstants.APP_TO_TEST;
	}
	
	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		if (!LauncherUtils.requiresUI(config))
			config.setAttribute(IPDELauncherConstants.APPLICATION, ITestNGPluginLauncherConstants.CORE_TEST_APPLICATION);
		else
			super.setDefaults(config);
	}
	
  protected String[] getApplicationNames() {
		TreeSet result = new TreeSet();
		result.add(ITestNGPluginLauncherConstants.TestNGProgramBlock_headless); 
		String[] appNames = super.getApplicationNames();
		for (int i = 0; i < appNames.length; i++) {
			result.add(appNames[i]);
		}
		return (String[])result.toArray(new String[result.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.launcher.BasicLauncherTab#initializeApplicationSection(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	protected void initializeApplicationSection(ILaunchConfiguration config) throws CoreException {
		String application = config.getAttribute(IPDELauncherConstants.APPLICATION, (String)null);
		if (ITestNGPluginLauncherConstants.CORE_TEST_APPLICATION.equals(application)) 
			fApplicationCombo.setText(ITestNGPluginLauncherConstants.TestNGProgramBlock_headless); 
		else
			super.initializeApplicationSection(config);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.launcher.BasicLauncherTab#saveApplicationSection(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	protected void saveApplicationSection(ILaunchConfigurationWorkingCopy config) {
		if (fApplicationCombo.getText().equals(ITestNGPluginLauncherConstants.TestNGProgramBlock_headless)) { 
			String appName = fApplicationCombo.isEnabled() ? ITestNGPluginLauncherConstants.CORE_TEST_APPLICATION : null;
			config.setAttribute(IPDELauncherConstants.APPLICATION, appName);
			config.setAttribute(IPDELauncherConstants.APP_TO_TEST, (String)null);
		} else {
			config.setAttribute(IPDELauncherConstants.APPLICATION, (String)null);
			super.saveApplicationSection(config);
		}
	}
	
}
