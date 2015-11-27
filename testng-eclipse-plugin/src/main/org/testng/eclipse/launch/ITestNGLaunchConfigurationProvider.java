package org.testng.eclipse.launch;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

public interface ITestNGLaunchConfigurationProvider {

  /**
   * Returns VM arguments to be used with this vm install whenever the TestNG VM
   * is launched as they should be passed to the command line, or null if none.
   * 
   * @param configuration
   * @return
   * @throws CoreException
   */
  String getVmArguments(ILaunchConfiguration configuration)
      throws CoreException;

  /**
   * Returns a list of environment variables to be used when launching the given
   * configuration or null if unspecified.
   * 
   * @param configuration
   * @return
   * @throws CoreException
   */
  List<String> getEnvironment(ILaunchConfiguration configuration)
      throws CoreException;
}
