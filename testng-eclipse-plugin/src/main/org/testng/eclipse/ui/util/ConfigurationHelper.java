package org.testng.eclipse.ui.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.Protocols;
import org.testng.eclipse.ui.RunInfo;
import org.testng.eclipse.util.StringUtils;
import org.testng.eclipse.util.SuiteGenerator;
import org.testng.remote.RemoteTestNG;
import org.testng.xml.LaunchSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Helper methods to store and retrieve values from a launch configuration.
 * 
 * @author cbeust
 */
public class ConfigurationHelper {
  
  public static class LaunchInfo {
    private String m_projectName;
    private  LaunchType m_launchType;
    private Collection<String> m_classNames;
    private Collection<String> m_packageNames;
    private Map<String, List<String>> m_classMethods;
    private String m_suiteName;
    private Map<String, List<String>> m_groupMap = Maps.newHashMap();
    private String m_logLevel;
    private boolean m_verbose;
    private boolean m_debug;
    private Protocols m_protocol;
    private boolean m_prefixVmArgsFromPom;

    public LaunchInfo(String projectName,
                      LaunchType launchType,
                      Collection<String> classNames,
                      Collection<String> packageNames,
                      Map<String, List<String>> classMethodsMap,
                      Map<String, List<String>> groupMap,
                      String suiteName,
                      String logLevel,
                      boolean verbose,
                      boolean debug,
                      Protocols protocol,
                      boolean prefixVmArgsFromPom) {
      m_projectName= projectName;
      m_launchType= launchType;
      m_classNames= classNames;
      m_classMethods= classMethodsMap;
      m_groupMap= groupMap;
      m_suiteName= suiteName.trim();
      m_logLevel= logLevel;
      m_packageNames = packageNames;
      m_verbose = verbose;
      m_debug = debug;
      m_protocol = protocol;
      m_prefixVmArgsFromPom = prefixVmArgsFromPom;
    }
  }

  public static int getLogLevel(ILaunchConfiguration config) {
    String stringResult = getStringAttribute(config, TestNGLaunchConfigurationConstants.LOG_LEVEL);
    if(null == stringResult) {
      return TestNGLaunchConfigurationConstants.DEFAULT_LOG_LEVEL;
    }
    else {
      return Integer.parseInt(stringResult);
    }
  }
  
  public static boolean getVerbose(ILaunchConfiguration config) {
    return getBooleanAttribute(config, TestNGLaunchConfigurationConstants.VERBOSE);
  }

  public static boolean getDebug(ILaunchConfiguration config) {
    return getBooleanAttribute(config, TestNGLaunchConfigurationConstants.DEBUG);
  }

  public static Protocols getProtocol(ILaunchConfiguration config) {
    String stringResult = getStringAttribute(config, TestNGLaunchConfigurationConstants.PROTOCOL);
    return null == stringResult ? TestNGLaunchConfigurationConstants.DEFAULT_SERIALIZATION_PROTOCOL : Protocols.get(stringResult); 
  }

  public static boolean isPrefixVmArgsFromPom(ILaunchConfiguration config) {
    return getBooleanAttribute(config, TestNGLaunchConfigurationConstants.PREFIX_VM_ARGS_FROM_POM, 
        TestNGLaunchConfigurationConstants.DEFAULT_PREFIX_VM_ARGS_FROM_POM);
  }

  public static String getSourcePath(ILaunchConfiguration config) {
    return getStringAttribute(config, TestNGLaunchConfigurationConstants.DIRECTORY_TEST_LIST);
  }

  public static List<String> getGroups(ILaunchConfiguration config) {
    return getListAttribute(config, TestNGLaunchConfigurationConstants.GROUP_LIST);
  }

  public static List<String> getGroupClasses(ILaunchConfiguration config) {
    return getListAttribute(config, TestNGLaunchConfigurationConstants.GROUP_CLASS_LIST);
  }
  
  public static List<String> getClasses(ILaunchConfiguration config) {
    return getListAttribute(config, TestNGLaunchConfigurationConstants.CLASS_TEST_LIST);
  }
  
  public static List<String> getPackages(ILaunchConfiguration config) {
	    return getListAttribute(config, TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST);
	  }
  
  public static List<String> getSuites(ILaunchConfiguration config) {
    return getListAttribute(config, TestNGLaunchConfigurationConstants.SUITE_TEST_LIST);
  }

  public static List<String> getSources(ILaunchConfiguration config) {
    return getListAttribute(config, TestNGLaunchConfigurationConstants.SOURCE_TEST_LIST);
  }

  public static String getProjectName(ILaunchConfiguration config) {
    return getStringAttribute(config, IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME);
  }
  
  public static String getMain(ILaunchConfiguration configuration) {
    return getStringAttribute(configuration, IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME);
  }
  
  public static List<String> getMethods(ILaunchConfiguration configuration) {
    return getListAttribute(configuration, TestNGLaunchConfigurationConstants.METHOD_TEST_LIST);
  }

  private static String getProjectJvmArgs() {
    IPreferenceStore store = TestNGPlugin.getDefault().getPreferenceStore();
    String result = store.getString(TestNGPluginConstants.S_JVM_ARGS);
    return result;
  }

  /**
   * @return the JVM args from the configuration or, if not found, from the preferences.
   */
  public static String getJvmArgs(ILaunchConfiguration configuration) throws CoreException {
    StringBuilder jvmArgs = new StringBuilder();
    jvmArgs.append(TestNGLaunchConfigurationConstants.VM_ENABLEASSERTION_OPTION);

    try {
      jvmArgs.append(getVMArgsFromPom(configuration));
    } catch (Exception e) {
      // log any exception when get JVM args from maven pom.xml,
      // just let the process carry on
      TestNGPlugin.log(e);
    }

    // JVM args from the previous configuration take precedence over the preference
    jvmArgs.append(" ").append(configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
		    getProjectJvmArgs()));

    addDebugProperties(jvmArgs, configuration);

    switch (ConfigurationHelper.getProtocol(configuration)) {
    case STRING:
      jvmArgs.append(" -Dtestng.eclipse.stringprotocol");
      break;
    default:
      break;
    }

    return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(jvmArgs.toString());
  }

  /**
   * Get the JVM args from maven pom.xmll.
   * <ul>
   * Here is the return value of different cases:
   * <li>no pom.xml -- return empty String</li>
   * <li>pom.xml exists, but no maven-surefire-plugin or maven-safefail-plugin, in essential, no "argLine" element in the pom.xml -- return empty String</li>
   * <li>there is one "argLine" element -- return the text content of the "argLine" element</li>
   * <li>there are more then one "argLine" elements -- return the first "argLine"<profile></li>
   * </ul>
   * @param conf
   * @return
   * @throws Exception
   */
  private static String getVMArgsFromPom(ILaunchConfiguration conf) throws Exception {
    StringBuilder vmArgs = new StringBuilder();
    if (isPrefixVmArgsFromPom(conf)) {
      IJavaProject javaProject = getJavaProject(conf);
      IFile pomFile = javaProject.getProject().getFile("pom.xml");
      if (pomFile.exists()) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(false);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(pomFile.getLocation().toFile());
  
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
  
        XPathExpression expr = xpath.compile("//argLine");
        NodeList argLineNodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        if (argLineNodes.getLength() > 0) {
          Node argLineNode = argLineNodes.item(0);
          vmArgs.append(" ").append(argLineNode.getTextContent());
        }
      }
    }
    return vmArgs.toString();
  }

  /**
   * Pass the system properties we were called with to the RemoteTestNG process.
   */
  private static void addDebugProperties(StringBuilder vmArgs, ILaunchConfiguration config) {
    String[] debugProperties = new String[] {
        RemoteTestNG.PROPERTY_DEBUG,
        RemoteTestNG.PROPERTY_VERBOSE
    };
    for (String p : debugProperties) {
      if (System.getProperty(p) != null) {
        vmArgs.append(" -D").append(p);
      }
    }

    if (ConfigurationHelper.getVerbose(config)) {
      vmArgs.append(" -D" + RemoteTestNG.PROPERTY_VERBOSE);
    }
    if (ConfigurationHelper.getDebug(config)) {
      vmArgs.append(" -D" + RemoteTestNG.PROPERTY_DEBUG);
    }
  }

  public static IJavaProject getJavaProject(ILaunchConfiguration configuration)
      throws CoreException {
    String projectName = getProjectName(configuration);
    if (projectName != null) {
      projectName = projectName.trim();
      if (projectName.length() > 0) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot()
            .getProject(projectName);
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject != null && javaProject.exists()) {
          return javaProject;
        }
      }
    }
    return null;
  }

  public static ILaunchConfigurationWorkingCopy setJvmArgs(
			ILaunchConfigurationWorkingCopy configuration, String args) {
		configuration.setAttribute(
				IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, args);

		return configuration;
	}
  
  public static LaunchType getType(ILaunchConfiguration configuration) {
    int result = getIntAttribute(configuration, TestNGLaunchConfigurationConstants.TYPE);
    return LaunchType.fromInt(result);
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
  private static Map<String, String> getMapAttribute(ILaunchConfiguration config, String attr) {
    Map<String, String> result = null;
    
    try {
      result = config.getAttribute(attr, result);
    }
    catch(CoreException cex) {
      TestNGPlugin.log(cex);
    }
    
    return result;
  }
  
  private static List<String> getListAttribute(ILaunchConfiguration config, String attr) {
    List<String> result = null;
    
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

  private static boolean getBooleanAttribute(ILaunchConfiguration config, String attr) {
    return getBooleanAttribute(config, attr, false);
  }

  private static boolean getBooleanAttribute(ILaunchConfiguration config, String attr, boolean defaultValue) {
    boolean result = defaultValue;
    
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
  
  /**
   * @param selectedProject
   */
  public static void createBasicConfiguration(IJavaProject javaProject, ILaunchConfigurationWorkingCopy config) {
    final String projectName = (javaProject == null) ? "" : javaProject.getElementName();
    
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                        projectName);
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                        RemoteTestNG.class.getName());
    config.setAttribute(TestNGLaunchConfigurationConstants.TYPE, LaunchType.CLASS.ordinal());
    config.setAttribute(TestNGLaunchConfigurationConstants.LOG_LEVEL, "2");
  }

  /**
   * @return List<LaunchSuite>
   */
  public static List<LaunchSuite> getLaunchSuites(IJavaProject ijp,
      ILaunchConfiguration configuration) {
    LaunchType type = ConfigurationHelper.getType(configuration);
    
    List<String> packages= null;
    List<String> testClasses = null;
    List<String> groups = null;
    Map<String, List<String>> classMethods= null;
    Map<String, String> parameters= null;
    
    parameters= getMapAttribute(configuration, TestNGLaunchConfigurationConstants.PARAMS);
    if (type == LaunchType.SUITE) {
      return createLaunchSuites(ijp.getProject(), getSuites(configuration));
    }
    
    if (type == LaunchType.GROUP) {
      groups = getGroups(configuration);
      testClasses = getGroupClasses(configuration);
      packages= getListAttribute(configuration, TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST);
    }
    else if (type == LaunchType.CLASS) {
      testClasses = getClasses(configuration);
    }
    else if (type == LaunchType.METHOD) {
      classMethods= getClassMethods(configuration);
    }
    else if (type == LaunchType.PACKAGE) {
      packages= getListAttribute(configuration, TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST);
    }

    return createLaunchSuites(ijp.getProject().getName(),
                              packages,
                              testClasses, 
                              classMethods, 
                              groups,
                              parameters,
                              getLogLevel(configuration)
           );      
  }

  public static Map<String, List<String>> getClassMethods(ILaunchConfiguration configuration) {
    Map<String, String> confResult =
        getMapAttribute(configuration, TestNGLaunchConfigurationConstants.ALL_METHODS_LIST);
    if(null == confResult) return null;
    
    Map<String, List<String>> results= new HashMap<String, List<String>>();
    for (Map.Entry<String, String> entry : confResult.entrySet()) {
      String className= entry.getKey();
      String methodNames= entry.getValue();
      results.put(className, Arrays.asList(methodNames.split(";")));
    }
    
    return results;
  }

  /**
   * @return List<LaunchSuite>
   */
  private static List<LaunchSuite> createLaunchSuites(final IProject project, List<String> suites) {
    List<LaunchSuite> result = Lists.newArrayList();

    for (String suitePath : suites) {
      File suiteFile= new File(suitePath);
      if(suiteFile.exists() && suiteFile.isFile()) {
      }
      else {
        suiteFile= project.getFile(suitePath).getLocation().toFile();
      }
      
      result.add(SuiteGenerator.createProxiedXmlSuite(suiteFile));
    }
    
    return result;
  }
    
  /**
   * Custom Eclipse plugin suite generator. Instead of using TestNG core
   * suite generator, we are using a set of custom generators that allow 
   * more customization.
   */
  private static List<LaunchSuite> createLaunchSuites(String projectName, List<String> packages,
      List<String> classNames, Map<String, List<String>> classMethods, List<String> groupNames,
      Map<String, String> parameters, final int logLevel) 
  {
    return Arrays.asList(
        new LaunchSuite[] {
            SuiteGenerator.createCustomizedSuite(projectName, packages, classNames, 
                classMethods, groupNames, parameters, logLevel)
        });
  }
  
  /**
   * Looks for an available configuration that matches the project and confName parameters.
   * If the defaultConfiguration is not null, it is used. The
   * defaultConfiguration may be null, which may cause null to be returned if there is not 
   * an exact match for the project and confName parameters. This method was added to allow
   * the FailureTab to pass along the previous configuration for re-use, so that any jvm args
   * defined there will be used.
   * @param launchManager
   * @param project
   * @param confName
   * @param defaultConfiguration
   * @return
   */
  public static ILaunchConfiguration findConfiguration(ILaunchManager launchManager, 
		  IProject project, String confName, RunInfo runInfo) {
 
	    ILaunchConfiguration resultConf = null;
		try {
				ILaunchConfigurationType confType = launchManager
						.getLaunchConfigurationType(TestNGLaunchConfigurationConstants.ID_TESTNG_APPLICATION);
				;

				ILaunchConfiguration[] availConfs = launchManager
						.getLaunchConfigurations(confType);

				final String projectName = project.getName();
				final String mainRunner = TestNGPluginConstants.MAIN_RUNNER;

				for (ILaunchConfiguration availConf : availConfs) {
					String confProjectName = ConfigurationHelper
					.getProjectName(availConf);
					String confMainName = ConfigurationHelper
					.getMain(availConf);

					if (projectName.equals(confProjectName)
							&& mainRunner.equals(confMainName) ) {
						if (confName != null &&
								confName.equals(availConf.getName())) {
							resultConf = availConf;
							break;
						}
						else if (runInfo != null) {
							Map<String, List<String>> availableClassMethods = getClassMethods(availConf);
							String method = runInfo.getMethodName();
							if (method != null && availableClassMethods != null) {
								String className = runInfo.getClassName();
								Object o = availableClassMethods.get(className);
								if (o != null && o instanceof List) {
									List methods = (List) o;
									if (methods.size() == 1) {
										String available = (String) methods.get(0);
										if (method.equalsIgnoreCase(available)) {
											resultConf = availConf;
											break;
										}
									}
								}
							}
						}// else if
						// TODO: else complain about no reference parameters
					}// if
				}	// for			   
		} catch (CoreException ce) {
			; // IGNORE
		}
		
		return resultConf;
	}

  public static Map<String, String> toClassMethodsMap(Map<String,
      Collection<String>> classMethods) {
    Map<String, String> result= new HashMap<String, String>();
    for (Map.Entry<String, Collection<String>> entry : classMethods.entrySet()) {
      String clsName = entry.getKey();
      Collection<String> methods= entry.getValue();
      StringBuffer strMethods = new StringBuffer();
      int i = 0;
      for (String method : methods) {
        if (i++ > 0) strMethods.append(";");
        strMethods.append(method);
      }
      
      result.put(clsName, strMethods.toString());
    }
    
    return result;
  }

  /**
   * @param configuration
   */
  public static void updateLaunchConfiguration(ILaunchConfigurationWorkingCopy configuration, LaunchInfo launchInfo) {
    Map<String, Collection<String>> classMethods = Maps.newHashMap();
    if (launchInfo.m_groupMap != null) {
      Collection<List<String>> classes= launchInfo.m_groupMap.values();
      if(null != classes) {
        for (List<String> classList : classes) {
          for (String c : classList) {
            classMethods.put(c, Collections.<String>emptyList());
          }
        }
      }
    }
    Collection<String> classNames= launchInfo.m_classNames;
    List<String> classNamesList= new ArrayList<String>();
    if(null != classNames && !classNames.isEmpty()) {
      for (String cls : classNames) {
        classMethods.put(cls, Collections.<String>emptyList());
        classNamesList.add(cls);
      }
    }
    List<String> packageList = new ArrayList<String>();
    if (launchInfo.m_packageNames != null) {
    	packageList.addAll(launchInfo.m_packageNames);
    }    
    if(null != launchInfo.m_classMethods) {
      classMethods.putAll(launchInfo.m_classMethods);
    }
    
    configuration.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
        launchInfo.m_launchType.ordinal());
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                               launchInfo.m_projectName);
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                               RemoteTestNG.class.getName());
    configuration.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
                               classNamesList);
    configuration.setAttribute(TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST,
    		packageList);
    configuration.setAttribute(TestNGLaunchConfigurationConstants.GROUP_LIST,
                               new ArrayList<String>(launchInfo.m_groupMap.keySet()));
    configuration.setAttribute(TestNGLaunchConfigurationConstants.GROUP_CLASS_LIST,
                               Utils.uniqueMergeList(launchInfo.m_groupMap.values()));
    configuration.setAttribute(TestNGLaunchConfigurationConstants.SUITE_TEST_LIST,
                               StringUtils.stringToNullList(launchInfo.m_suiteName));
    configuration.setAttribute(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST,
                               toClassMethodsMap(classMethods));
    configuration.setAttribute(TestNGLaunchConfigurationConstants.LOG_LEVEL,
                               launchInfo.m_logLevel);
    configuration.setAttribute(TestNGLaunchConfigurationConstants.VERBOSE, launchInfo.m_verbose);
    configuration.setAttribute(TestNGLaunchConfigurationConstants.DEBUG, launchInfo.m_debug);
    configuration.setAttribute(TestNGLaunchConfigurationConstants.PROTOCOL, launchInfo.m_protocol.toString());
    configuration.setAttribute(TestNGLaunchConfigurationConstants.PREFIX_VM_ARGS_FROM_POM, launchInfo.m_prefixVmArgsFromPom);
  }
}
