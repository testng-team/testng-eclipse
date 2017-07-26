package org.testng.eclipse.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.ui.RunInfo;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.util.param.ParameterSolver;
import org.testng.reporters.FailedReporter;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * An utility class that centralize the work about configuration launchers.
 * 
 * <p/>
 * <b>Implementation notes</b>:
 * <ul>
 * <li>
 * <i>Strategy for existing launch configurations</i>: when an existing launch
 * configuration already exists, its attributes are used to initialize the new
 * launch configuration and then these are overwritten with the new attributes</li>
 * <li>
 * <i>Dependency resolving</i>:
 * <ul>
 * <li><b>dependsOnMethods</b>: needed only by method-based launch type</li>
 * <li><b>dependsOnGroups</b>: needed by all non-suite launch types. For the
 * moment a warning is displayed</li>
 * <li><b>static data provider</b>: this dependency is solved by TestNG core</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class LaunchUtil {
  private static final List<String> EMPTY_ARRAY_PARAM = new ArrayList<>();
  
  /**
   * Suite file launcher. The file may reside outside the workbench.
   */
  public static void launchFailedSuiteConfiguration(IJavaProject javaProject, 
		  String runMode) {
	  launchFailedSuiteConfiguration(javaProject, runMode, null, null);
  }
  

  /**
   * Suite file launcher. The file may reside outside the workbench.
   */
  public static void launchFailedSuiteConfiguration(IJavaProject javaProject, 
		  String runMode, ILaunchConfiguration prevConfig, Set<String> failureDescriptions) {
    final String suiteConfName= javaProject.getElementName() + "-" + FailedReporter.TESTNG_FAILED_XML;
    final String suiteFilePath= TestNGPlugin.getPluginPreferenceStore().getOutputAbsolutePath(javaProject).toOSString() + "/" + FailedReporter.TESTNG_FAILED_XML;
    
    launchSuiteConfiguration(javaProject.getProject(),
        suiteConfName,
        suiteFilePath,
        runMode, prevConfig, failureDescriptions);
  }

  /**
   * Suite file launcher. The <code>IFile</code> must exist in the workbench.
   */
  public static void launchSuiteConfiguration(IFile suiteFile, String mode) {
	  launchSuiteConfiguration(suiteFile, mode, null, null);
  }
  
  /**
   * Suite file launcher. The <code>IFile</code> must exist in the workbench.
   */
  public static void launchSuiteConfiguration(IFile suiteFile, String mode, 
		  ILaunchConfiguration prevConfig, Set<String> failureDescriptions) {
    final IProject project= suiteFile.getProject();
    final String fileConfName= suiteFile.getProjectRelativePath().toString().replace('/', '.');
    final String suitePath= suiteFile.getLocation().toOSString();

    launchSuiteConfiguration(project, fileConfName, suitePath, mode, prevConfig, failureDescriptions);
  }
  
    
  private static void launchSuiteConfiguration(IProject project, String fileConfName,
      String suiteFilePath,
		  String mode, ILaunchConfiguration prevConfig, Set<String> failureDescriptions) {
    ILaunchConfigurationWorkingCopy configWC =
        createLaunchConfiguration(project, project.getName() + "_" + fileConfName, null);
    try {
      if (prevConfig != null) {
        Map<String, String> previousEnv
            = prevConfig.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map<String, String>) null);
        configWC.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, previousEnv);
      }
    } catch (CoreException e) {
      TestNGPlugin.log(e);
    }

    configWC.setAttribute(TestNGLaunchConfigurationConstants.SUITE_TEST_LIST,
                          Collections.singletonList(suiteFilePath));
    configWC.setAttribute(TestNGLaunchConfigurationConstants.TYPE, LaunchType.SUITE.ordinal());


    // carry over jvm args from prevConfig
    // set failed test jvm args
    try {
      String jargs = null;
      if (prevConfig != null) {
        jargs = ConfigurationHelper.getJvmArgs(prevConfig);
      }
      if (jargs != null) ConfigurationHelper.setJvmArgs(configWC, jargs);
      if (failureDescriptions != null && !failureDescriptions.isEmpty()) {
    		setFailedTestsJvmArg(StringUtils.listToString(failureDescriptions), configWC);
      }
    } catch (CoreException e) {
      // TODO throw the exception rather than catch it
      TestNGPlugin.log(e);
    }
    runConfig(configWC, mode);
  }
  
  public static void launchMapConfiguration(IProject project,
                                            String configName,
                                            Map<String, Object> launchAttributes,
                                            ICompilationUnit compilationUnit,
                                            String launchMode) {
    ILaunchConfigurationWorkingCopy workingCopy= createLaunchConfiguration(project, configName, null);

    try {
      launchAttributes.putAll(workingCopy.getAttributes());
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }

    workingCopy.setAttributes(launchAttributes);

    runConfig(workingCopy, launchMode);
  }

  /**
   * Creates a Map containing the basic properties of a new launch configuration,
   * based on types.
   */
  public static Map<String, Object> createClassLaunchConfigurationMap(IType mainType, IType[] types, String annotationType) {
    Map<String, Object> attrs= new HashMap<String, Object>();

    List<String> classNames= new ArrayList<>();
    Multimap<String, String> classMethods = ArrayListMultimap.create();
    classMethods.get(null);

    for (IType type : types) {
      classNames.add(type.getFullyQualifiedName());
//      classMethods.put(types[i].getFullyQualifiedName(), EMPTY_ARRAY_PARAM);
    }

    attrs.put(TestNGLaunchConfigurationConstants.TYPE, LaunchType.CLASS.ordinal());
    attrs.put(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST, classNames);
    attrs.put(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST,
        ConfigurationHelper.toClassMethodsMap(classMethods.asMap()));

    return attrs;
  }

  public static void launchPackageConfiguration(IJavaProject ijp, IPackageFragment ipf, String mode) {
    List<String> packageNames= new ArrayList<String>();
    packageNames.add(ipf.getElementName());

    try {
      if (findDependsOnGroups(ipf.getCompilationUnits()).length > 0) {
        groupDependencyWarning("package " + ipf.getElementName(), null);
      }
    }
    catch(JavaModelException jmex) {
      ; // this should never happen but who knows
    }

    ILaunchConfigurationWorkingCopy workingCopy= createLaunchConfiguration(ijp.getProject(), "package " + ipf.getElementName(), null);

    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
                             EMPTY_ARRAY_PARAM);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.METHOD_TEST_LIST,
                             EMPTY_ARRAY_PARAM);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST,
                             packageNames);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
        LaunchType.PACKAGE.ordinal());
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST,
        ConfigurationHelper.toClassMethodsMap(new HashMap<String, Collection<String>>()));

    String projectName= ijp.getProject().getName();

    PreferenceStoreUtil storage = TestNGPlugin.getPluginPreferenceStore();
    String preDefinedListeners = storage.getPreDefinedListeners(projectName, false);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PRE_DEFINED_LISTENERS, preDefinedListeners.toString().trim());

    runConfig(workingCopy, mode);
  }

  public static void launchMethodConfiguration(IJavaProject javaProject,
          IMethod imethod,
          String runMode) {
	  launchMethodConfiguration(javaProject, imethod, runMode, null);
  }

  private static boolean methodHasDependencies(IMethod method) throws JavaModelException {
    IAnnotation annotation = method.getAnnotation("Test");
    return annotation != null &&
        (contains(annotation.getMemberValuePairs(), "dependsOnGroups")
         || contains(annotation.getMemberValuePairs(), "dependsOnMethods"));
  }

  private static boolean contains(IMemberValuePair[] memberValuePairs, String string) {
    for (IMemberValuePair pair : memberValuePairs) {
      if (string.equals(pair.getMemberName())) {
        return true;
      }
    }

    return false;
  }

  public static void launchMethodConfiguration(IJavaProject javaProject,
          IMethod iMethod,
          String runMode,
          RunInfo runInfo) {
    launchMethodConfiguration(javaProject, iMethod.getDeclaringType(), iMethod, runMode, runInfo);
  }
  
  public static void launchMethodConfiguration(IJavaProject javaProject,
          IType iType,
          IMethod iMethod,
          String runMode,
          RunInfo runInfo) {

    Set<TypeAndMethod> typeAndMethods = new HashSet<>();
    typeAndMethods.add(new TypeAndMethod(iType, iMethod));

    try {
      if (methodHasDependencies(iMethod)) {
        DependencyInfo groupInfo = DependencyInfo.createDependencyInfo(javaProject);
        Set<IMethod> transitiveMethods = findMethodTransitiveClosure(iMethod, groupInfo);
        for (IMethod transitiveMethod : transitiveMethods) {
          IType t = transitiveMethod.getDeclaringType();
          typeAndMethods.add(new TypeAndMethod(t, transitiveMethod));
        }
      }
    } catch (JavaModelException e) {
      TestNGPlugin.log(e);
    }

    launchMethodBasedConfiguration(javaProject, typeAndMethods.toArray(new TypeAndMethod[typeAndMethods.size()]),
        runMode, runInfo);
  }
 
  /**
   * @param elementName
   * @param groups
   */
  private static void groupDependencyWarning(String elementName, Set<String> groups) {
    ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
        "WARNING", 
        elementName + " defines group dependencies that will be ignored. To reliably test methods with group dependencies use a suite definition.", 
        new Status(IStatus.WARNING, TestNGPlugin.PLUGIN_ID, 3333, 
            elementName + " uses group dependencies " + (groups != null ? groups.toString() : "")
            + " which due to a plugin limitation will be ignored", null));

  }


  private static void launchMethodBasedConfiguration(IJavaProject ijp,
          TypeAndMethod[] typeAndMethods, String runMode, RunInfo runInfo) {
    List<String> methodNames = new ArrayList<>();
    IMethod[] methods = new IMethod[typeAndMethods.length];
    Multimap<String, String> classMethods = ArrayListMultimap.create();
    Set<IType> typesSet = new HashSet<IType>();
    for (int i = 0; i < typeAndMethods.length; i++) {
      IMethod method = typeAndMethods[i].getMethod();
      IType type = typeAndMethods[i].getType();
      methods[i] = method;
      methodNames.add(method.getElementName());
      classMethods.put(type.getFullyQualifiedName(), method.getElementName());
      typesSet.add(type);
    }

    final IType[] types= typesSet.toArray(new IType[typesSet.size()]);

    List<String> typeNames = new ArrayList<String>();
    for (IType type : types) {
      typeNames.add(type.getFullyQualifiedName());
    }
    String name = types[0].getTypeQualifiedName().toString() + "." + methodNames.get(0).toString();

    ILaunchConfigurationWorkingCopy workingCopy= createLaunchConfiguration(ijp.getProject(), name, runInfo);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
                             typeNames);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.METHOD_TEST_LIST,
                             methodNames);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST,
                             EMPTY_ARRAY_PARAM);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
        LaunchType.METHOD.ordinal());
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST,
                             ConfigurationHelper.toClassMethodsMap(classMethods.asMap()));
    String projectName= ijp.getProject().getName();

    PreferenceStoreUtil storage = TestNGPlugin.getPluginPreferenceStore();
    String preDefinedListeners = storage.getPreDefinedListeners(projectName, false);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PRE_DEFINED_LISTENERS, preDefinedListeners.toString().trim());

    if (runInfo != null) {
    	// set the class and method

    	// set any jvm args
    	String jargs = runInfo.getJvmArgs();
    	if (jargs != null) workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
    			jargs);
      Map<String, String> envVars = runInfo.getEnvironmentVariables();
      if (envVars != null) {
        workingCopy.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, envVars);
      }
    	setFailedTestsJvmArg(runInfo.getTestDescription(), workingCopy);
    }
    runConfig(workingCopy, runMode);
  }

  /**
   * Launch a type-based test.
   */
  public static void launchTypeConfiguration(IJavaProject ijp, IType type, String mode) {
    launchTypeBasedConfiguration(ijp, type.getElementName(), new IType[] {type}, mode);
  }

  /**
   * Launch a compilation unit (a source file) based test.
   */
  public static void launchCompilationUnitConfiguration(IJavaProject ijp,
      List<ICompilationUnit> units, String mode) {
    List<IType> types = new ArrayList<>();
    IType mainType = null;
    for (ICompilationUnit icu : units) {
      try {
        for (IType type : icu.getTypes()) {
          types.add(type);
        }
      }
      catch(JavaModelException jme) {
        TestNGPlugin.log(new Status(IStatus.ERROR, TestNGPlugin.PLUGIN_ID, TestNGPluginConstants.LAUNCH_ERROR, "No types in compilation unit " + icu.getElementName(), jme));
      }

      if(null == types) return;

      mainType = icu.findPrimaryType();
    }

    launchTypeBasedConfiguration(ijp, createConfName(mainType, units.size()),
        types.toArray(new IType[types.size()]), mode);
  }

  /**
   * @return the name of this configuration, which will be displayed in the menu. For
   * one type, it's the name of this type. For more than one type, just show the name
   * of the first type followed by ellipses.
   */
  private static String createConfName(IType mainType, int unitCount) {
    String result = mainType.getElementName();
    if (unitCount > 1) result = result + ", ...";

    return result;
  }

  public static void launchTypesConfiguration(IJavaProject project, List<IType> types, String mode)
  {
    launchTypeBasedConfiguration(project, createConfName(types.get(0), types.size()),
        types.toArray(new IType[types.size()]), mode);
  }

  private static void launchTypeBasedConfiguration(IJavaProject javaProject, String confName,
      IType[] types, String mode) {
    Multimap<String, String> classMethods = ArrayListMultimap.create();
    Set<String> typeNames = new HashSet<>();
    Set<IType> allTypes = new HashSet<>();
    allTypes.addAll(Arrays.asList(types));

    Set<IMethod> allMethods = new HashSet<>();

    // If we depend on groups, need to add all the necessary types
    Object[] groupDependencies = findDependsOnMethodsOrGroups(types);
    if (groupDependencies.length > 0) {
      DependencyInfo depInfo = DependencyInfo.createDependencyInfo(javaProject);
      Set<IType> closure = findTypeTransitiveClosure(types, depInfo);
      allTypes.addAll(closure);
      Set<IMethod> methods = findMethodTransitiveClosure(types, depInfo);
      allMethods.addAll(methods);
    }

    for (IType type : allTypes) {
      typeNames.add(type.getFullyQualifiedName());
    }

    for (IMethod m : allMethods) {
      classMethods.put(m.getDeclaringType().getFullyQualifiedName(), m.getElementName());
      typeNames.add(m.getDeclaringType().getFullyQualifiedName());
    }

    ILaunchConfigurationWorkingCopy workingCopy =
        createLaunchConfiguration(javaProject.getProject(), confName, null);

    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE, LaunchType.CLASS.ordinal());
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST,
                             ConfigurationHelper.toClassMethodsMap(classMethods.asMap()));
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST, new ArrayList<>(typeNames));
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.METHOD_TEST_LIST, EMPTY_ARRAY_PARAM);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST, EMPTY_ARRAY_PARAM);

    String projectName= javaProject.getProject().getName();

    PreferenceStoreUtil storage = TestNGPlugin.getPluginPreferenceStore();
    String preDefinedListeners = storage.getPreDefinedListeners(projectName, false);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PRE_DEFINED_LISTENERS, preDefinedListeners.toString().trim());

    runConfig(workingCopy, mode);
  }

  public static Set<IMethod> findMethodTransitiveClosure(IType[] types, DependencyInfo groupInfo) {
    Set<IMethod> result = new HashSet<>();
    for (IType type : types) {
      result.addAll(findMethodTransitiveClosure(type, groupInfo));
    }

    return result;
  }

  public static Set<IMethod> findMethodTransitiveClosure(IType type, DependencyInfo groupInfo) {
    Set<IMethod> result = new HashSet<>();
    try {
      for (IMethod method : type.getMethods()) {
        result.addAll(findMethodTransitiveClosure(method, groupInfo));
      }
    } catch(JavaModelException ex) {
      TestNGPlugin.log(ex);
    }

    return result;
  }

  public static Set<IMethod> findMethodTransitiveClosure(IMethod startMethod, DependencyInfo groupInfo) {
    Set<IMethod> result = new HashSet<>();
    Set<IMethod> currentMethods = new HashSet<>();
    currentMethods.add(startMethod);
    Set<IMethod> nextMethods = new HashSet<>();
    Set<String> initialGroups = new HashSet<>();

    while (! currentMethods.isEmpty()) {
      for (IMethod method : currentMethods) {
        result.add(method);

        Collection<String> groups = groupInfo.groupDependenciesByMethods.get(method);
        if (groups != null) {
          if (initialGroups.isEmpty()) initialGroups.addAll(groups);
          for (String group : groups) {
            Collection<IMethod> depMethods = groupInfo.methodsByGroups.get(group);
            if (depMethods != null) {
              for (IMethod depMethod : depMethods) {
                if (! result.contains(depMethod)) {
                  result.add(depMethod);
                  nextMethods.add(depMethod);
                }
              }
            } else {
              TestNGPlugin.log("Can't find any method defining the group " + group);
            }
          }
        } else {
          TestNGPlugin.log("No groups depended upon by method: " + method.getElementName());
        }

        Collection<IMethod> depMethods = groupInfo.methodsByMethods.get(method);
        if (depMethods != null) {
          for (IMethod dm : depMethods) {
            if (! result.contains(dm)) {
              result.add(dm);
              nextMethods.add(dm);
            }
          }
        }
      }

      currentMethods.clear();
      currentMethods.addAll(nextMethods);
      nextMethods.clear();
    }

    StringBuilder sb = new StringBuilder();
    for (IMethod m : result) {
      sb.append(m.getDeclaringType().getFullyQualifiedName())
          .append(".").append(m.getElementName()).append(" ");
    }
    TestNGPlugin.trace("Transitive closure for method " + startMethod.getElementName()
        + ": "  + sb.toString());

    return result;
  }

  private static Set<IType> findTypeTransitiveClosure(IType[] types, DependencyInfo groupInfo) {
    Set<IType> result = new HashSet<>();
    Set<IType> currentTypes = new HashSet<>();
    currentTypes.addAll(Arrays.asList(types));
    Set<IType> nextTypes = new HashSet<>();
    Set<String> initialGroups = new HashSet<>();

    while (! currentTypes.isEmpty()) {
      for (IType type : currentTypes) {
        result.add(type);

        Collection<String> groups = groupInfo.groupDependenciesByTypes.get(type);
        if (groups != null) {
          if (initialGroups.isEmpty()) initialGroups.addAll(groups);
          for (String group : groups) {
            Collection<IType> depTypes = groupInfo.typesByGroups.get(group);
            if (depTypes != null) {
              for (IType depType : depTypes) {
                if (! result.contains(depType)) {
                  result.add(depType);
                  nextTypes.add(depType);
                }
              }
            } else {
              TestNGPlugin.log("Can't find any types defining the group " + group);
            }
          }
        } else {
          TestNGPlugin.log("No groups depended upon by type: " + type.getElementName());
        }
      }

      currentTypes.clear();
      currentTypes.addAll(nextTypes);
      nextTypes.clear();
    }

    StringBuilder sb = new StringBuilder();
    for (IType type : result) {
      sb.append(type.getFullyQualifiedName()).append(" ");
    }
    TestNGPlugin.trace("Transitive closure for groups \"" + initialGroups + "\":"  + sb.toString());

    return result;
  }

  public static Object[] findDependenciesBySearch(List<IResource> resources, String q) {
    IResource[] scopeResources= resources.toArray(new IResource[resources.size()]);
    ISearchQuery query= new FileSearchQuery(q, 
        true /*regexp*/ , 
        true /*casesensitive*/, 
        FileTextSearchScope.newSearchScope(scopeResources, new String[] {"*.java"}, false));
    query.run(new NullProgressMonitor());
    FileSearchResult result= (FileSearchResult) query.getSearchResult(); 
    Object[] elements = result.getElements();

    return elements;
  }

  private static Object[] findDependsOnGroups(ICompilationUnit[] units) {
    List<IResource> resources = new ArrayList<>();
    for (ICompilationUnit unit : units) {
      try {
        resources.add(unit.getCorrespondingResource());
      }
      catch(JavaModelException jmex) {
        ;
      }
    }

    return findDependenciesBySearch(resources, "@Test\\(.*\\s*dependsOnGroups\\s*=.*");
  }

  private static Object[] findDependsOnMethodsOrGroups(IType[] types) {
    List<IResource> resources = new ArrayList<>(types.length);
    for(int i= 0; i < types.length; i++) {
      resources.add(types[i].getResource());
    }

    return findDependenciesBySearch(resources, "@Test\\(.*\\s*(dependsOnGroups)|(dependsOnMethods)\\s*=.*");
  }

  /**
   * Initialize a <code>ILaunchConfigurationWorkingCopy</code> either by using an existing one (if found),
   * or using a default configuration (for example, the one used for the most recent launch), 
   * or by creating a new one based on the <code>project</code> name and the <code>confName</code>.
   * 
   * @throws CoreException if getting an working copy from an existing configuration fails
   */
  private static ILaunchConfigurationWorkingCopy createLaunchConfiguration(IProject project, String confName, 
		  RunInfo runInfo ) {
    ILaunchManager launchManager= getLaunchManager();
    ILaunchConfiguration config= ConfigurationHelper.findConfiguration(launchManager, project, confName, runInfo);

    ILaunchConfigurationWorkingCopy configWC= null;
    if(null != config) {
      try {
        configWC= config.getWorkingCopy();
      }
      catch(CoreException cex) {
        TestNGPlugin.log(new Status(IStatus.ERROR, TestNGPlugin.PLUGIN_ID, TestNGPluginConstants.LAUNCH_ERROR,
            "Cannot create working copy of existing launcher " + config.getName(), cex));
      }
    }
    if(null == configWC) { 
      if (confName == null && runInfo != null) {
    	  confName = runInfo.getClassName() + "." + runInfo.getMethodName();    	  
      }
      configWC= ConfigurationHelper.createBasicConfiguration(launchManager, project, confName);
    }

    return configWC;
  }

  private static void runConfig(ILaunchConfigurationWorkingCopy launchConfiguration, String runMode) {
    ILaunchConfiguration conf= save(launchConfiguration);
    if(null != conf) {
//      try {
//        Map attrs= conf.getAttributes();
//        System.out.println("Launch attrs:" + attrs);
//      }
//      catch(CoreException cex) { ; }
      
      DebugUITools.launch(conf, runMode);
    }
  }

  /**
   * Saves the working copy (may return <tt>null</tt> if the <code>launchWorkingCopy</code> is 
   * <tt>null</tt> or the save fails).
   */
  private static ILaunchConfiguration save(ILaunchConfigurationWorkingCopy launchWorkingCopy) {
    if(null == launchWorkingCopy) return null;
    
    try {
      return launchWorkingCopy.doSave();
    }
    catch(CoreException cex) {
      TestNGPlugin.log(cex);
    }
    
    return null;
  }

  private static ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }

  /**
   * Wrapper method over <code>ParameterSolver.solveParameters</code> that never
   * returns <tt>null</tt>s.
   */
  private static Map<String, String> solveParameters(IJavaElement[] javaElements) {
    Map<String, String> result = ParameterSolver.solveParameters(javaElements);
    if (result != null) {
      return result;
    }
    return Collections.emptyMap();
  }

  /**
   * Wrapper method over <code>ParameterSolver.solveParameters</code> that never
   * returns <tt>null</tt>s.
   */
  private static Map<String, String> solveParameters(IJavaElement javaElement) {
    return solveParameters(new IJavaElement[] {javaElement});
  }

  public static ILaunchConfigurationWorkingCopy setFailedTestsJvmArg (String value, 
		  ILaunchConfigurationWorkingCopy config) {
	  try {
		  String key = TestNGPlugin.getFailedTestsKey();
		  String jvmargs = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "");			
		  String newarg = key + "=\"" + value + "\" ";
		  if (!key.startsWith("-D")) newarg = "-D" + newarg;
		  // if there is no value, then remove this jvm arg if there is one.
		  if (value == "") newarg = " "; 
		  else newarg = " " + newarg;
		  if (jvmargs.equals("")) {
			  // simplest case: set the attribute
			  config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, newarg);
		  }
		  else if (jvmargs.indexOf(key) == -1) {
			  // nothing to replace; just add
			  config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, jvmargs + newarg);
		  }
		  else {
			  // find the new arg in the existing jvm args and replace it
			  int start = jvmargs.indexOf(key);
			  int next = jvmargs.indexOf("-D", start + 1);
			  StringBuffer buf = new StringBuffer();
			  buf.append(newarg)
			  .append (jvmargs.substring(0,start));
			  if (next > start) {
				  buf.append(jvmargs.substring(next));
			  }
			  config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, buf.toString());
		  }
	  }
	  catch (CoreException ce) {}
	  return config;
  }
  
  /**
   * Create a working copy from the launcher arg, and set a jvm arg with the supplied 
   * key and value. 
   * @param key
   * @param value
   * @param config
   * @return
   */
  public static ILaunchConfigurationWorkingCopy addJvmArg (String key, String value, 
		  ILaunchConfigurationWorkingCopy config) {	  
	  try {
			String jvmargs = config.getAttribute(
					IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "");			
			String newarg = key + "=\"" + value + "\" ";
			if (!key.startsWith("-D")) newarg = "-D" + newarg;
			// if there is no value, then remove this jvm arg if there is one.
			if (value == "") newarg = " "; 
			else newarg = " " + newarg;
			if (jvmargs.equals("")) {
				// simplest case: set the attribute			
				config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, newarg);
			}
			else if (jvmargs.indexOf(key) == -1) {
				// nothing to replace; just add
				config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, jvmargs + newarg);
			}
			else {
				// find the new arg in the existing jvm args and replace it
				int start = jvmargs.indexOf(key);
				int next = jvmargs.indexOf("-D", start + 1);
				StringBuffer buf = new StringBuffer();
				buf.append(newarg)
				.append (jvmargs.substring(0,start));
				if (next > start) {
					buf.append(jvmargs.substring(next));
				}
				config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, buf.toString());
			}
		}
	  catch (CoreException ce) {}
	  return config;
  }

  public static void errorDialog(String string, Throwable ex) {
    ErrorDialog.openError(Display.getCurrent().getActiveShell(),
        "Fatal error",
        string + (ex.getMessage() != null ? ": " + ex.getMessage() : ""),
        new Status(IStatus.ERROR, TestNGPlugin.PLUGIN_ID, 0,
            "Status Error Message", null));
    TestNGPlugin.log(ex);
  }

}
