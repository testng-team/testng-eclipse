package org.testng.eclipse.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
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
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.testng.TestNG;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.TypeParser;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.param.ParameterSolver;
import org.testng.remote.RemoteTestNG;
import org.testng.reporters.FailedReporter;

/**
 * An utility class that centralize the work about configuration launchers.
 * TODO: refactor it
 * 
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class LaunchUtil {

  /**
   * @param class1
   * @param mainType
   * @param types
   * @return
   */
  public static Map createClassLaunchConfiguration(IType mainType,
                                                   IType[] types,
                                                   String annotationType) {
    Map attrs= new HashMap();

    List classNames= new ArrayList();
    Map classMethods= new HashMap();
    List emptyMethods= new ArrayList();

    for(int i= 0; i < types.length; i++) {
      classNames.add(types[i].getFullyQualifiedName());
      classMethods.put(types[i].getFullyQualifiedName(), emptyMethods);
    }

    attrs.put(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST, classNames);
    attrs.put(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR, annotationType);
    attrs.put(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST,
              ConfigurationHelper.toClassMethodsMap(classMethods));

    return attrs;
  }

  /**
   * @param project
   * @param configName
   * @param launchAttributes
   * @param launchMode <tt>run</tt> or <tt>debug</tt>
   */
  public static void launchConfiguration(IProject project,
                                         String configName,
                                         Map launchAttributes,
                                         ICompilationUnit compilationUnit,
                                         String launchMode) {
    ILaunchManager launchManager= getLaunchManager();
    ILaunchConfiguration config= ConfigurationHelper.findConfiguration(launchManager,
                                                                       project,
                                                                       configName);
    if(null == config) {
      try {
        ILaunchConfigurationWorkingCopy workingCopy= ConfigurationHelper.createBasicConfiguration(launchManager,
                                                                                                  project,
                                                                                                  configName);
        launchAttributes.putAll(workingCopy.getAttributes());

        if(null != compilationUnit) {
          Map params= solveParameters(compilationUnit);
          launchAttributes.put(TestNGLaunchConfigurationConstants.PARAMS, params);
        }

        workingCopy.setAttributes(launchAttributes);

        config= workingCopy.doSave();
      }
      catch(CoreException ce) {
        TestNGPlugin.log(ce);
      }
    }

    if(null != config) {
      launchConfiguration(config, launchMode);
    }

  }

  /**
   * @param javaProject
   * @param imethod
   * @param complianceLevel
   * @param runMode
   */
  public static void launchConfiguration(IJavaProject javaProject,
                                         List classNames,
                                         List methodNames,
                                         IMethod imethod,
                                         String complianceLevel,
                                         String runMode) {
    Map parameters= solveParameters(imethod);
    List allMethodNames= solveDependencies(imethod);

    final String confName= imethod.getDeclaringType().getElementName() + "."
      + imethod.getElementName();
    ILaunchConfigurationWorkingCopy workingCopy= ConfigurationHelper.createBasicConfiguration(getLaunchManager(), 
        javaProject.getProject(), confName);

    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST, classNames);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.METHOD_TEST_LIST, allMethodNames);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
                             TestNGLaunchConfigurationConstants.METHOD);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PARAMS, parameters);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR,
                             complianceLevel);

    if(null != workingCopy) {
      try {
        launchConfiguration(workingCopy.doSave(), runMode);
      }
      catch(CoreException ce) {
        TestNGPlugin.log(ce);
      }
    }
  }

  /**
   * 
   */
  public static void launchFailedSuite(IJavaProject javaProject, String runMode) {
    String suiteFileName= TestNGPlugin.getDefault().getOutputDir(javaProject.getProject().getName()) + "/" + FailedReporter.TESTNG_FAILED_XML; 
    IFile suiteFile= javaProject.getProject().getFile(suiteFileName);
    final String launchConfName = suiteFile.getFullPath().toOSString();
    ILaunchConfiguration conf = findConfiguration(suiteFile, runMode);
        
    if(null == conf) {
      ILaunchConfigurationWorkingCopy wCopy = 
            ConfigurationHelper.createBasicConfiguration(getLaunchManager(),
                                                         suiteFile.getProject(),
                                                         launchConfName);
      wCopy.setAttribute(TestNGLaunchConfigurationConstants.SUITE_TEST_LIST,
                         Utils.stringToList(suiteFile.getProjectRelativePath().toOSString()));
      wCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
                           TestNGLaunchConfigurationConstants.SUITE);
      conf = wCopy;
    }
        
    launchConfiguration(conf, runMode);
  }
  
  private static ILaunchConfiguration findConfiguration(IFile file, String mode) {
    ILaunchConfigurationType confType = getLaunchManager().getLaunchConfigurationType(TestNGLaunchConfigurationConstants.ID_TESTNG_APPLICATION);
    ILaunchConfiguration resultConf = null;
    try {
      ILaunchConfiguration[] availConfs = getLaunchManager().getLaunchConfigurations(confType);
      
      String projectName = file.getProject().getName();
      String suitePath = file.getFullPath().toOSString();
      String main = RemoteTestNG.class.getName();
      
      for(int i = 0; i < availConfs.length; i++) {
        String confProjectName = ConfigurationHelper.getProjectName(availConfs[i]);
        String confMainName = ConfigurationHelper.getMain(availConfs[i]);
        
        if(projectName.equals(confProjectName) && main.equals(confMainName)) {
          List suiteList = ConfigurationHelper.getSuites(availConfs[i]);
          if(null != suiteList 
              && suiteList.size() == 1 
              && suitePath.equals(suiteList.get(0))) {
            if(null == resultConf) {
              resultConf = availConfs[i];
            }
          }
        }
      }
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }
    
    return resultConf;
  }

  /**
   * @param suiteFile
   * @param mode
   */
  public static void launchConfiguration(IFile suiteFile, String mode) {
    final String fileConfName= suiteFile.getProjectRelativePath().toString().replace('/', '.');
    ILaunchConfiguration config= ConfigurationHelper.findConfiguration(getLaunchManager(), suiteFile.getProject(), fileConfName);
    if(null == config) {
      ILaunchConfigurationWorkingCopy wCopy = 
        ConfigurationHelper.createBasicConfiguration(getLaunchManager(),
                                                     suiteFile.getProject(),
                                                     fileConfName);
      wCopy.setAttribute(TestNGLaunchConfigurationConstants.SUITE_TEST_LIST,
                        Utils.stringToList(suiteFile.getProjectRelativePath().toOSString()));
      wCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
                         TestNGLaunchConfigurationConstants.SUITE);
    
      try {
        config= wCopy.doSave();
      }
      catch(CoreException cex) {
        TestNGPlugin.log(cex);
      }
    }
    
    if(null != config) {
      launchConfiguration(config, mode);
    }
  }

  /**
   * @param configuration
   */
  public static void updateLaunchConfiguration(ILaunchConfigurationWorkingCopy configuration, LaunchInfo launchInfo) {
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
        launchInfo.m_projectName);
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
            RemoteTestNG.class.getName());
    configuration.setAttribute(TestNGLaunchConfigurationConstants.TYPE, launchInfo.m_launchType);
    configuration.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
            Utils.stringToNullList(launchInfo.m_className));
    configuration.setAttribute(TestNGLaunchConfigurationConstants.GROUP_LIST,
            new ArrayList(launchInfo.m_groupMap.keySet()));
    configuration.setAttribute(TestNGLaunchConfigurationConstants.GROUP_CLASS_LIST,
            Utils.uniqueMergeList(launchInfo.m_groupMap.values()));
    configuration.setAttribute(TestNGLaunchConfigurationConstants.SUITE_TEST_LIST,
            Utils.stringToNullList(launchInfo.m_suiteName));
    configuration.setAttribute(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR,
            launchInfo.m_complianceLevel);
    configuration.setAttribute(TestNGLaunchConfigurationConstants.LOG_LEVEL,
            launchInfo.m_logLevel);
  }
  
  /**
   * TODO: doesn't solve the dependsOnGroups
   */
  public static void launchPackageConfiguration(IJavaProject ijp, IPackageFragment ipf, String mode) {
    final String confName= "package " + ipf.getElementName();
    ILaunchConfiguration config= ConfigurationHelper.findConfiguration(getLaunchManager(), ijp.getProject(), confName);
    List packageNames= new ArrayList();
    packageNames.add(ipf.getElementName());
    
    if(null == config) {    
      ILaunchConfigurationWorkingCopy workingCopy = ConfigurationHelper.createBasicConfiguration(
          getLaunchManager(), ijp.getProject(), confName);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
                               new ArrayList());
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.METHOD_TEST_LIST,
                               new ArrayList());
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST,
                               packageNames);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
                               TestNGLaunchConfigurationConstants.PACKAGE);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST,
                               ConfigurationHelper.toClassMethodsMap(new HashMap()));
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PARAMS,
                               solveParameters(ipf));
    
      try {
        config= workingCopy.doSave();
      }
      catch(CoreException cex) {
        TestNGPlugin.log(cex);
      }
    }
    
    if(null != config) {
      launchConfiguration(config, mode);
    }
  }

  /**
   * @param ijp
   * @param type
   * @param mode
   */
  public static void launchTypeConfiguration(IJavaProject ijp, IType type, String mode) {
    IType[] types = new IType[] {type};
    final String confName= type.getElementName();
    Map classMethods= new HashMap();
    classMethods.put(type.getFullyQualifiedName(), new ArrayList());

    List typeNames = new ArrayList();
    for(int i = 0; i < types.length; i++) {
      typeNames.add(types[i].getFullyQualifiedName());
    }
        
    ILaunchConfiguration config= ConfigurationHelper.findConfiguration(getLaunchManager(), ijp.getProject(), confName);
    
    if(null == config) {    
      ILaunchConfigurationWorkingCopy workingCopy = ConfigurationHelper.createBasicConfiguration(
          getLaunchManager(), ijp.getProject(), confName);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
                               typeNames);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.METHOD_TEST_LIST,
                               new ArrayList());
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST,
                               new ArrayList());
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
                               TestNGLaunchConfigurationConstants.CLASS);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST,
                               ConfigurationHelper.toClassMethodsMap(classMethods));
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR,
                               getQuickComplianceLevel(types));
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PARAMS,
                               solveParameters(type));
  
      try {
        config= workingCopy.doSave();
      }
      catch(CoreException cex) {
        TestNGPlugin.log(cex);
      }
    }
    
    if(null != config) {
      launchConfiguration(config, mode);
    }
  }

  /**
   * @param ijp
   * @param method
   * @param mode
   */
  public static void launchMethodConfiguration(IJavaProject ijp, IMethod imethod, String mode) {
    Map methods = new HashMap();
    methods.put(imethod.getElementName(), imethod);
    
    JDTUtil.solveDependencies(imethod, methods);
    
    List methodNames= new ArrayList(methods.size());
    for(Iterator it= methods.values().iterator(); it.hasNext(); ) {
      IMethod m= (IMethod) it.next();
      methodNames.add(m.getElementName());
    }
    
    Map classMethods= new HashMap(); // Map<String, List<String>>
    for(Iterator it= methods.values().iterator(); it.hasNext(); ) {
      IMethod m= (IMethod) it.next();
      List methodList= (List) classMethods.get(m.getDeclaringType().getElementName());
      if(null == methodList) {
        methodList= new ArrayList();
        classMethods.put(m.getDeclaringType().getFullyQualifiedName(), methodList); 
      }
      methodList.add(m.getElementName());
    }
    
    IType[] types= new IType[] {imethod.getDeclaringType()};
    final String confName= imethod.getDeclaringType().getElementName() + "." + imethod.getElementName();
    
    List typeNames = new ArrayList();
    for(int i = 0; i < types.length; i++) {
      typeNames.add(types[i].getFullyQualifiedName());
    }
        
    Map parameters= solveParameters(imethod);
    
    ILaunchConfiguration config= ConfigurationHelper.findConfiguration(getLaunchManager(), ijp.getProject(), confName);
    
    if(null == config) {    
      ILaunchConfigurationWorkingCopy workingCopy = ConfigurationHelper.createBasicConfiguration(
          getLaunchManager(), ijp.getProject(), confName);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
                               typeNames);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.METHOD_TEST_LIST,
                               methodNames);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST,
                               new ArrayList());
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
                               TestNGLaunchConfigurationConstants.METHOD);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST,
                               ConfigurationHelper.toClassMethodsMap(classMethods));
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PARAMS,
                               parameters);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR,
                               getQuickComplianceLevel(types));
  
      try {
        config= workingCopy.doSave();
      }
      catch(CoreException cex) {
        TestNGPlugin.log(cex);
      }
    }
    
    if(null != config) {
      launchConfiguration(config, mode);
    }
  }


  /**
   * @param ijp
   * @param unit
   * @param mode
   */
  public static void launchCompilationUnitConfiguration(IJavaProject ijp, ICompilationUnit icu, String mode) {
    IType mainType= icu.findPrimaryType();
    final String confName= mainType != null ? mainType.getElementName() : icu.getElementName();
    Map classMethods= new HashMap();
    
    IType[] types= null;
    try {
      types = icu.getTypes();
      classMethods.put(mainType != null ? mainType.getElementName() : icu.getElementName(), new ArrayList());
    }
    catch(JavaModelException jme) {
      ; // nothing
    }

    List typeNames = new ArrayList();
    for(int i = 0; i < types.length; i++) {
      typeNames.add(types[i].getFullyQualifiedName());
    }
        
    ILaunchConfiguration config= ConfigurationHelper.findConfiguration(getLaunchManager(), ijp.getProject(), confName);
    
    if(null == config) {    
      ILaunchConfigurationWorkingCopy workingCopy = ConfigurationHelper.createBasicConfiguration(
          getLaunchManager(), ijp.getProject(), confName);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
                               typeNames);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.METHOD_TEST_LIST,
                               new ArrayList());
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST,
                               new ArrayList());
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
                               TestNGLaunchConfigurationConstants.CLASS);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.ALL_METHODS_LIST,
                               ConfigurationHelper.toClassMethodsMap(classMethods));
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR,
                               getQuickComplianceLevel(types));
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PARAMS,
                               solveParameters(icu));
  
      try {
        config= workingCopy.doSave();
      }
      catch(CoreException cex) {
        TestNGPlugin.log(cex);
      }
    }
    
    if(null != config) {
      launchConfiguration(config, mode);
    }
  }
  
  private static List /*<String>*/ solveDependencies(IMethod imethod) {
    Map methods= new HashMap();
    methods.put(imethod.getElementName(), imethod);

    JDTUtil.solveDependencies(imethod, methods);

    List methodNames= new ArrayList(methods.size());
    for(Iterator it= methods.values().iterator(); it.hasNext();) {
      IMethod m= (IMethod) it.next();
      methodNames.add(m.getElementName());
    }

    return methodNames;
  }

  private static ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }

  private static void launchConfiguration(ILaunchConfiguration config, String mode) {
    if(null != config) {
      DebugUITools.launch(config, mode);
    }
  }

  /**
   * Wrapper method over <code>ParameterSolver.solveParameters</code> that never
   * returns <tt>null</tt>s.
   */
  private static Map solveParameters(IJavaElement javaElement) {
    Map result= ParameterSolver.solveParameters(javaElement);
    
    return result != null ? result : new HashMap();
  }
  
  /**
   * Uses the Eclipse search support to look for @Test annotation and decide
   * if the compliance level should be set to JDK or JAVADOC.
   */
  private static String getQuickComplianceLevel(IType[] types) {
    List resources= new ArrayList();
    for(int i= 0; i < types.length; i++) {
      try {
        resources.add(types[i].getCompilationUnit().getCorrespondingResource());
      }
      catch(JavaModelException jmex) {
        ;
      }
    }
    IResource[] scopeResources= (IResource[]) resources.toArray(new IResource[resources.size()]);
    ISearchQuery query= new FileSearchQuery("@Test(\\(.+)?", 
        true /*regexp*/ , 
        true /*casesensitive*/, 
        FileTextSearchScope.newSearchScope(scopeResources, new String[] {"*.java"}, false));
    query.run(new NullProgressMonitor());
    FileSearchResult result= (FileSearchResult) query.getSearchResult(); 
    Object[] elements= result.getElements();
    
    return elements != null && elements.length > 0 ? TestNG.JDK_ANNOTATION_TYPE : TestNG.JAVADOC_ANNOTATION_TYPE;
  }

  public static class LaunchInfo {
    private String m_projectName;
    private int m_launchType;
    private String m_className;
    private String m_suiteName;
    private Map m_groupMap;
    private String m_complianceLevel;
    private String m_logLevel;
    
    public LaunchInfo(String projectName,
                      int launchType,
                      String className,
                      Map groupMap,
                      String suiteName,
                      String complianceLevel,
                      String logLevel) {
      m_projectName= projectName;
      m_launchType= launchType;
      m_className= className.trim();
      m_groupMap= groupMap;
      m_suiteName= suiteName.trim();
      m_complianceLevel= complianceLevel;
      m_logLevel= logLevel;
    }
  }
}
