package org.testng.eclipse.util;

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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.util.Util;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.ui.PlatformUI;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;
import org.testng.eclipse.collections.Lists;
import org.testng.eclipse.collections.Maps;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.ui.RunInfo;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.util.JDTUtil.MethodDefinition;
import org.testng.eclipse.util.param.ParameterSolver;
import org.testng.reporters.FailedReporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private static final List EMPTY_ARRAY_PARAM= new ArrayList();
  
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
		  String runMode, ILaunchConfiguration prevConfig, Set failureDescriptions) {
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
		  ILaunchConfiguration prevConfig, Set failureDescriptions) {
    final IProject project= suiteFile.getProject();
    final String fileConfName= suiteFile.getProjectRelativePath().toString().replace('/', '.');
    final String suitePath= suiteFile.getLocation().toOSString();

    launchSuiteConfiguration(project, fileConfName, suitePath, mode, prevConfig, failureDescriptions);
  }
  
    
  private static void launchSuiteConfiguration(IProject project, String fileConfName, String suiteFilePath, 
		  String mode, ILaunchConfiguration prevConfig, Set failureDescriptions) {
    ILaunchConfigurationWorkingCopy configWC= createLaunchConfiguration(project, fileConfName, null);

    configWC.setAttribute(TestNGLaunchConfigurationConstants.SUITE_TEST_LIST,
                          Collections.singletonList(suiteFilePath));
    configWC.setAttribute(TestNGLaunchConfigurationConstants.TYPE, LaunchType.SUITE.ordinal());
    // carry over jvm args from prevConfig
    // set failed test jvm args
    String jargs = ConfigurationHelper.getJvmArgs(prevConfig);
    if (jargs != null) ConfigurationHelper.setJvmArgs(configWC, jargs);
    if (failureDescriptions != null && failureDescriptions.size() > 0) {
    	Iterator it = failureDescriptions.iterator();
    	StringBuffer buf = new StringBuffer();
		boolean first = true;
		while (it.hasNext()) {
			if (first) first = false;
			else buf.append(",");
			buf.append (it.next());
		}
		setFailedTestsJvmArg(buf.toString(), configWC);
    }
    runConfig(configWC, mode);
  }
  
  public static void launchMapConfiguration(IProject project,
                                            String configName,
                                            Map launchAttributes,
                                            ICompilationUnit compilationUnit,
                                            String launchMode) {
    ILaunchConfigurationWorkingCopy workingCopy= createLaunchConfiguration(project, configName, null);

    try {
      launchAttributes.putAll(workingCopy.getAttributes());
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }

    if(null != compilationUnit) {
      Map params= solveParameters(compilationUnit);
      launchAttributes.put(TestNGLaunchConfigurationConstants.PARAMS, params);
    }

    workingCopy.setAttributes(launchAttributes);

    runConfig(workingCopy, launchMode);
  }

  /**
   * Creates a Map containing the basic properties of a new launch configuration,
   * based on types.
   */
  public static Map createClassLaunchConfigurationMap(IType mainType, IType[] types, String annotationType) {
    Map attrs= new HashMap();

    List<String> classNames= Lists.newArrayList();
    Map<String, List<String>> classMethods= Maps.newHashMap();

    for(int i= 0; i < types.length; i++) {
      classNames.add(types[i].getFullyQualifiedName());
      classMethods.put(types[i].getFullyQualifiedName(), EMPTY_ARRAY_PARAM);
    }

    attrs.put(TestNGLaunchConfigurationConstants.TYPE, LaunchType.CLASS.ordinal());
    attrs.put(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST, classNames);
//    attrs.put(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR, annotationType);
    attrs.put(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST, 
        ConfigurationHelper.toClassMethodsMap(classMethods));

    return attrs;
  }
  
  public static void launchPackageConfiguration(IJavaProject ijp, IPackageFragment ipf, String mode) {
    List packageNames= new ArrayList();
    packageNames.add(ipf.getElementName());

    try {
      if(haveGroupsDependency(ipf.getCompilationUnits())) {
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
                             ConfigurationHelper.toClassMethodsMap(new HashMap()));
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PARAMS,
                             solveParameters(ipf));

    runConfig(workingCopy, mode);
  }
  
  public static void launchMethodConfiguration(IJavaProject javaProject,
          IMethod imethod,
          String runMode) {
	  launchMethodConfiguration (javaProject, imethod, runMode, null);
  }
  
  public static void launchMethodConfiguration(IJavaProject javaProject,
          IMethod imethod,
          String runMode,
          RunInfo runInfo) {
	     
    Set/*<String>*/ groups= new HashSet();
    List allmethods= solveDependsOn(imethod);
    IMethod[] methods= new IMethod[allmethods.size()];
    for(int i= 0; i < allmethods.size(); i++) {
      MethodDefinition md= (MethodDefinition) allmethods.get(i);
      methods[i]= md.getMethod();
      groups.addAll(md.getGroups());
    }
    
    if(!groups.isEmpty()) {
      groupDependencyWarning(imethod.getElementName(), groups);
    }

    launchMethodBasedConfiguration(javaProject, methods, runMode, runInfo);
  }
 
  /**
   * @param elementName
   * @param groups
   */
  private static void groupDependencyWarning(String elementName, Set groups) {
    ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
        "WARNING", 
        elementName + " defines group dependencies that will be ignored. To reliably test methods with group dependencies use a suite definition.", 
        new Status(IStatus.WARNING, TestNGPlugin.PLUGIN_ID, 3333, 
            elementName + " uses group dependencies " + (groups != null ? groups.toString() : "")
            + " which due to a plugin limitation will be ignored", null));

  }
  
  
  private static void launchMethodBasedConfiguration(IJavaProject ijp,  
		  IMethod[] methods, String runMode, RunInfo runInfo) {
    Set typesSet= new HashSet();
    for(int i= 0; i < methods.length; i++) {
      typesSet.add(methods[i].getDeclaringType());
    }
    
    List/*<String>*/ methodNames= new ArrayList();
    Map/*<String, List<String>>*/ classMethods= new HashMap();
    for(int i= 0; i < methods.length; i++) {
      methodNames.add(methods[i].getElementName());
      
      List methodList= (List) classMethods.get(methods[i].getDeclaringType().getFullyQualifiedName());
      if(null == methodList) {
        methodList= new ArrayList();
        classMethods.put(methods[i].getDeclaringType().getFullyQualifiedName(), methodList); 
      }
      methodList.add(methods[i].getElementName());
    }
    
    IType[] types= (IType[]) typesSet.toArray(new IType[typesSet.size()]);
    
    List typeNames = new ArrayList();
    for(int i = 0; i < types.length; i++) {
      typeNames.add(types[i].getFullyQualifiedName());
    }
    String name = typeNames.get(0).toString() + "." + methodNames.get(0).toString();
//    final String complianceLevel= annotationType != null ? annotationType : getQuickComplianceLevel(types);
  
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
                             ConfigurationHelper.toClassMethodsMap(classMethods));
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PARAMS,
                             solveParameters(methods));
//    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR,
//                             complianceLevel);
    if (runInfo != null) {
    	// set the class and method
    	
    	// set any jvm args
    	String jargs = runInfo.getJvmArgs();
    	if (jargs != null) workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
    			jargs);
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
    List<IType> types = Lists.newArrayList();
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

  private static void launchTypeBasedConfiguration(IJavaProject ijp, String confName, IType[] types, String mode) {
    Map classMethods= new HashMap();
    List typeNames = new ArrayList();
    
    for(int i = 0; i < types.length; i++) {
      typeNames.add(types[i].getFullyQualifiedName());
      classMethods.put(types[i].getFullyQualifiedName(), EMPTY_ARRAY_PARAM);
    }

    if(haveGroupsDependency(types)) {
      groupDependencyWarning(confName, null);
    }
    
    ILaunchConfigurationWorkingCopy workingCopy = createLaunchConfiguration(ijp.getProject(), confName, null); 
    
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
        LaunchType.CLASS.ordinal());
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST,
                             ConfigurationHelper.toClassMethodsMap(classMethods));
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
                             typeNames);
//    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR,
//                             getQuickComplianceLevel(types));
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PARAMS,
                             solveParameters(types));
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.METHOD_TEST_LIST,
                             EMPTY_ARRAY_PARAM);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST,
                             EMPTY_ARRAY_PARAM);

    runConfig(workingCopy, mode);
  }
  
  private static boolean haveGroupsDependency(ICompilationUnit[] units) {
    List resources= new ArrayList();
    for(int i= 0; i < units.length; i++) {
      try {
        resources.add(units[i].getCorrespondingResource());
      }
      catch(JavaModelException jmex) {
        ;
      }
    }
    IResource[] scopeResources= (IResource[]) resources.toArray(new IResource[resources.size()]);
    ISearchQuery query= new FileSearchQuery("@Test\\(.*\\s*dependsOnGroups\\s*=.*", 
        true /*regexp*/ , 
        true /*casesensitive*/, 
        FileTextSearchScope.newSearchScope(scopeResources, new String[] {"*.java"}, false));
    query.run(new NullProgressMonitor());
    FileSearchResult result= (FileSearchResult) query.getSearchResult(); 
    Object[] elements= result.getElements();
    
    return elements != null && elements.length > 0;
  }
  
  private static boolean haveGroupsDependency(IType[] types) {
    ICompilationUnit[] units= new ICompilationUnit[types.length];
    for(int i= 0; i < types.length; i++) {
      units[i]= types[i].getCompilationUnit();
    }
    
    return haveGroupsDependency(units);
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

  private static List/*<MethodDefinition>*/ solveDependsOn(IMethod imethod) {
    return JDTUtil.solveDependencies(imethod);
  }

  private static ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }

  /**
   * Wrapper method over <code>ParameterSolver.solveParameters</code> that never
   * returns <tt>null</tt>s.
   */
  private static Map solveParameters(IJavaElement[] javaElements) {
    Map result= ParameterSolver.solveParameters(javaElements);
    
    return result != null ? result : new HashMap();
  }
  
  /**
   * Wrapper method over <code>ParameterSolver.solveParameters</code> that never
   * returns <tt>null</tt>s.
   */
  private static Map solveParameters(IJavaElement javaElement) {
    return solveParameters(new IJavaElement[] {javaElement});
  }
  
  /**
   * Uses the Eclipse search support to look for @Test annotation and decide
   * if the compliance level should be set to JDK or JAVADOC.
   */
//  private static String getQuickComplianceLevel(IType[] types) {
//    List resources= new ArrayList();
//    for(int i= 0; i < types.length; i++) {
//      try {
//        resources.add(types[i].getCompilationUnit().getCorrespondingResource());
//      }
//      catch(JavaModelException jmex) {
//        ;
//      }
//    }
//    IResource[] scopeResources= (IResource[]) resources.toArray(new IResource[resources.size()]);
//    ISearchQuery query= new FileSearchQuery("@(Test|Before|After|Factory)(\\(.+)?", 
//        true /*regexp*/ , 
//        true /*casesensitive*/, 
//        FileTextSearchScope.newSearchScope(scopeResources, getJavaLikeExtensions(), false));
//    query.run(new NullProgressMonitor());
//    FileSearchResult result= (FileSearchResult) query.getSearchResult(); 
//    Object[] elements= result.getElements();
//    
//    return elements != null && elements.length > 0 ? TestNG.JDK_ANNOTATION_TYPE : TestNG.JAVADOC_ANNOTATION_TYPE;
//  }
  
  private static String[] getJavaLikeExtensions() {
    char[][] exts = Util.getJavaLikeExtensions();
    if (exts != null && exts.length > 0) {
      String[] extStrs = new String[exts.length];
      for (int i = 0; i < exts.length; i++) {
        extStrs[i] = "*." + String.valueOf(exts[i]);
      }
      return extStrs;
    } else {
      return new String[] {"*.java"};
    }
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

}
