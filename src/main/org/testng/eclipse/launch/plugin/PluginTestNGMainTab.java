package org.testng.eclipse.launch.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.ui.launcher.MainTab;

/**
 * A launch configuration tab that displays and edits the main launching arguments
 * of a Plug-in TestNG test.
 * <p>
 * This class may be instantiated. This class is not intended to be subclassed by clients.
 * </p>
 * @since 3.2
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings("restriction")
public class PluginTestNGMainTab extends MainTab {
  /**
   * Overrides the implementation of the basis MainTab.
   */
  protected void createProgramBlock() {
    fProgramBlock = new TestNGProgramBlock(this);
  }
  
  /*
   * (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public void initializeFrom(ILaunchConfiguration config) {
    try {
      fDataBlock.initializeFrom(config, true);
      fProgramBlock.initializeFrom(config);
      fJreBlock.initializeFrom(config);
    } catch (CoreException e) {
      PDEPlugin.logException(e);
    } finally {
    }
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

  /*
   * (non-Javadoc)
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
   */
  public void performApply(ILaunchConfigurationWorkingCopy config) {
    fDataBlock.performApply(config, true);
    fProgramBlock.performApply(config);
    fJreBlock.performApply(config);
  }

  /* (non-Javadoc)
   * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#getId()
   */
  public String getId() {
    return ITestNGPluginLauncherConstants.TAB_PLUGIN_TESTNG_MAIN_ID;
  }

}
