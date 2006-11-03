package org.testng.eclipse.ui.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.SuiteGenerator;
import org.testng.remote.RemoteTestNG;

/**
 * Helper methods to store and retrieve values from a launch configuration.
 * 
 * @author cbeust
 */
public class ConfigurationHelper {
  
  public static int getLogLevel(ILaunchConfiguration config) {
    String stringResult = getStringAttribute(config, TestNGLaunchConfigurationConstants.LOG_LEVEL);
    if(null == stringResult) {
      return TestNGLaunchConfigurationConstants.DEFAULT_LOG_LEVEL;
    }
    else {
      return Integer.parseInt(stringResult);
    }
  }
  
  public static String getSourcePath(ILaunchConfiguration config) {
    return getStringAttribute(config, TestNGLaunchConfigurationConstants.DIRECTORY_TEST_LIST);
  }

  public static List getGroups(ILaunchConfiguration config) {
    return getListAttribute(config, TestNGLaunchConfigurationConstants.GROUP_LIST);
  }

  public static List getGroupClasses(ILaunchConfiguration config) {
    return getListAttribute(config, TestNGLaunchConfigurationConstants.GROUP_CLASS_LIST);
  }
  
  public static List getClasses(ILaunchConfiguration config) {
    return getListAttribute(config, TestNGLaunchConfigurationConstants.CLASS_TEST_LIST);
  }
  
  public static List getSuites(ILaunchConfiguration config) {
    return getListAttribute(config, TestNGLaunchConfigurationConstants.SUITE_TEST_LIST);
  }

  public static List getSources(ILaunchConfiguration config) {
    return getListAttribute(config, TestNGLaunchConfigurationConstants.SOURCE_TEST_LIST);
  }

  public static String getProjectName(ILaunchConfiguration config) {
    return getStringAttribute(config, IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME);
  }
  
  public static String getMain(ILaunchConfiguration configuration) {
    return getStringAttribute(configuration, IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME);
  }
  
  private static List getMethods(ILaunchConfiguration configuration) {
    return getListAttribute(configuration, TestNGLaunchConfigurationConstants.METHOD_TEST_LIST);
  }

  public static String getComplianceLevel(ILaunchConfiguration configuration) {
    return getStringAttribute(configuration, TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR);
  }
  
  public static String getComplianceLevel(IJavaProject ijproject, ILaunchConfiguration configuration) {
    String result = getStringAttribute(configuration, TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR);
    
    if(null == result) {
      result = JDTUtil.getProjectVMVersion(ijproject);
    }
    
    return result;
  }
  
  public static int getType(ILaunchConfiguration configuration) {
    int result = getIntAttribute(configuration, TestNGLaunchConfigurationConstants.TYPE);
    return result;
  }
  
  public static String getProjectName(ILaunch launch) {
    return launch.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME);
  }
  
  public static int getPort(ILaunch launch) {
    try {
      return Integer.parseInt(launch.getAttribute(TestNGLaunchConfigurationConstants.PORT));
    }
    catch(Throwable thr) {
      return 0;
    }
  }
  
  public static String getSubName(ILaunch launch) {
    return launch.getAttribute(TestNGLaunchConfigurationConstants.TESTNG_RUN_NAME_ATTR);
  }

  ///////////////////// 
  private static Map getMapAttribute(ILaunchConfiguration config, String attr) {
    Map result = null;
    
    try {
      result = config.getAttribute(attr, result);
    }
    catch(CoreException cex) {
      TestNGPlugin.log(cex);
    }
    
    return result;
  }
  
  private static List getListAttribute(ILaunchConfiguration config, String attr) {
    List result = null;
    
    try {
      result = config.getAttribute(attr, result);
    }
    catch (CoreException e) {
      TestNGPlugin.log(e);
    }
    
    return result;
    
  }
  
  private static String getStringAttribute(ILaunchConfiguration config, String attr) {
    String result = null;
    
    try {
      result = config.getAttribute(attr, result);
    }
    catch (CoreException e) {
      TestNGPlugin.log(e);
    }
    
    return result;
    
  }

  private static int getIntAttribute(ILaunchConfiguration config, String attr) {
    int result = 0;
    
    try {
      result = config.getAttribute(attr, result);
    }
    catch (CoreException e) {
      TestNGPlugin.log(e);
    }
    
    return result;
  }

  public static ILaunchConfigurationWorkingCopy createBasicConfiguration(final ILaunchManager launchManager,
                                                                         final IProject project,
                                                                         final String confName) {
    ILaunchConfigurationWorkingCopy wConf = null;
    
    try {
      ILaunchConfigurationType configurationType = launchManager.getLaunchConfigurationType(TestNGLaunchConfigurationConstants.ID_TESTNG_APPLICATION);
      wConf = configurationType.newInstance(null /*project*/, confName); // launchManager.generateUniqueLaunchConfigurationNameFrom(confName));
      wConf.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                         RemoteTestNG.class.getName());
      wConf.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                         project.getName());
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }
    
    return wConf;
  }
  
  private static String computeRelativePath(final String rootPath, final String sourcePath) {
    File rootFile = new File(rootPath);
    String rootRelativeName = rootFile.getName();
    
    int idx = sourcePath.indexOf(rootPath);
    return File.separator + rootRelativeName + sourcePath.substring(idx + rootPath.length());
  }

  /**
   * @return List<LaunchSuite>
   */
  public static List getLaunchSuites(IJavaProject ijp, ILaunchConfiguration configuration) {
    int type = ConfigurationHelper.getType(configuration);
    
    List packages= null;
    List testClasses = null;
    List groups = null;
    List testMethods = null;
    Map parameters= null;
    
    parameters= getMapAttribute(configuration, TestNGLaunchConfigurationConstants.PARAMS);
    if (type == TestNGLaunchConfigurationConstants.SUITE) {
      return createLaunchSuites(ijp.getProject(), getSuites(configuration));
    }
    else if (type == TestNGLaunchConfigurationConstants.GROUP) {
      groups = getGroups(configuration);
      testClasses = getGroupClasses(configuration);
    }
    else if (type == TestNGLaunchConfigurationConstants.CLASS) {
      testClasses = getClasses(configuration);
    }
    else if (type == TestNGLaunchConfigurationConstants.METHOD) {
      testClasses = getClasses(configuration);
      testMethods = getMethods(configuration);
    }
    else if (type == TestNGLaunchConfigurationConstants.PACKAGE) {
      packages= getListAttribute(configuration, TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST);
    }

    return createLaunchSuites(ijp.getProject().getName(),
                              packages,
                              testClasses, 
                              testMethods, 
                              groups,
                              parameters,
                              getComplianceLevel(ijp, configuration),
                              getLogLevel(configuration)
           );      
  }

  /**
   * @return List<LaunchSuite>
   */
  private static List createLaunchSuites(final IProject project, List suites) {
    List suiteList = new ArrayList();

    for(Iterator it = suites.iterator(); it.hasNext(); ) {
      String suitePath= (String) it.next();
      File suiteFile= new File(suitePath);
      if(suiteFile.exists() && suiteFile.isFile()) {
      }
      else {
        suiteFile= project.getFile(suitePath).getLocation().toFile();
      }
      
      suiteList.add(SuiteGenerator.createProxiedXmlSuite(suiteFile));
    }
    
    return suiteList;
  }
    
  // FIXME: does not support multiple classes
  private static List createLaunchSuites(String projectName,
                                         List packages,
                                         List classNames, 
                                         List methodNames, 
                                         List groupNames,
                                         Map parameters,
                                         String annotationType,
                                         final int logLevel) 
  {
    return Arrays.asList(
        new Object[] {SuiteGenerator.createCustomizedSuite(projectName,
                                                           packages,
                                                           classNames, 
                                                           methodNames, 
                                                           groupNames,
                                                           parameters,
                                                           annotationType,
                                                           logLevel)});
  }

  /**
   * @param launchManager
   * @param project
   * @param confName
   * @return
   */
  public static ILaunchConfiguration findConfiguration(ILaunchManager launchManager, IProject project, String confName) {
    ILaunchConfigurationType confType = launchManager.getLaunchConfigurationType(TestNGLaunchConfigurationConstants.ID_TESTNG_APPLICATION);;
    ILaunchConfiguration resultConf = null;
    try {
      ILaunchConfiguration[] availConfs = launchManager.getLaunchConfigurations(confType);
      
      final String projectName = project.getName();
      final String mainRunner = TestNGPlugin.MAIN_RUNNER;
      
      for(int i = 0; i < availConfs.length; i++) {
        String confProjectName = ConfigurationHelper.getProjectName(availConfs[i]);
        String confMainName = ConfigurationHelper.getMain(availConfs[i]);
        
        if(projectName.equals(confProjectName) && mainRunner.equals(confMainName) && confName.equals(availConfs[i].getName())) {
          resultConf= availConfs[i];
          break;
        }
      }
    }
    catch(CoreException ce) {
      ; // IGNORE
    }
    
    return resultConf;
  }

}
