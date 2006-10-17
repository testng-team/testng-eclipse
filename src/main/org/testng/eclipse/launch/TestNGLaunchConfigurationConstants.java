package org.testng.eclipse.launch;

import org.testng.eclipse.TestNGPlugin;


/**
 * Constants used to pass information from the launch manager to the
 * launcher.

 * @author cbeust
 */
public abstract class TestNGLaunchConfigurationConstants {
  public static final String JDK15_COMPLIANCE = "jdk";
  public static final String JDK14_COMPLIANCE = "javadoc";
  
  public static final String ID_TESTNG_APPLICATION = "org.testng.eclipse.launchconfig"; //$NON-NLS-1$
  
  private static String make(String s) {
    return TestNGPlugin.PLUGIN_ID + "." + s;
  }
  
  /**
   * Root directory of the tests.  If this property is set,
   * TestNG will run all the tests contained in this directory.
   */
  public static final String DIRECTORY_TEST_LIST = 
    make("DIRECTORY_TEST_LIST"); //$NON-NLS-1$
  
  /**
   * List of classes
   */
  public static final String CLASS_TEST_LIST = 
    make("CLASS_TEST_LIST"); //$NON-NLS-1$

  /**
   * List of methods
   */
  public static final String METHOD_TEST_LIST = 
    make("METHOD_TEST_LIST"); //$NON-NLS-1$
  
  /**
   * List of packages
   */
  public static final String PACKAGE_TEST_LIST = 
    make("PACKAGE_TEST_LIST"); //$NON-NLS-1$
  
  /**
   * List of sources
   */
  public static final String SOURCE_TEST_LIST = 
      make("SOURCE_TEST_LIST"); //$NON-NLS-1$
  
  /**
   * List of groups
   */
  public static final String GROUP_LIST = 
    make("GROUP_LIST"); //$NON-NLS-1$
  
  public static final String GROUP_CLASS_LIST = make("GROUP_LIST_CLASS");
  
  public static final int DEFAULT_LOG_LEVEL = 2;
  
  /**
   * List of suites
   */
  public static final String SUITE_TEST_LIST = 
    make("SUITE_TEST_LIST");  //$NON-NLS-1$
  
  /**
   * Port of the launcher
   */
  public static final String PORT = 
    make("PORT"); //$NON-NLS-1$
  
  /**
   * Name of the project
   */
  public static final String PROJECT_NAME = 
    make("PROJECT_NAME"); //$NON-NLS-1$
  
  public static final String TESTNG_RUN_NAME_ATTR = 
    make("SUBNAME"); //$NON-NLS-1$

  public static final String TEMP_SUITE_LIST = 
    make("TEMP_SUITE_LIST"); //$NON-NLS-1$
  
  public static final String TYPE = make("TYPE");  //$NON-NLS-1$ 
  
  public static final String LOG_LEVEL = make("LOG_LEVEL");  //$NON-NLS-1$ 
  
  public static final String TESTNG_COMPLIANCE_LEVEL_ATTR = make("COMPLIANCE_LEVEL"); //$NON-NLS-1$
  
  public static final String VM_ENABLEASSERTION_OPTION = "-ea";
  
  // What kind of run we are doing
  public static final int CLASS = 1;
  public static final int GROUP = 2;
  public static final int SUITE = 3;
  public static final int METHOD = 4;
  public static final int PACKAGE = 5;
  public static final String PARAMS = make("PARAMETERS");
  
  
}
