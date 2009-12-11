package org.testng.eclipse.launch;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.testng.TestNG;
import org.testng.TestNGCommandLineArgs;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.ListenerContributorUtil;
import org.testng.eclipse.util.PreferenceStoreUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.remote.RemoteTestNG;
import org.testng.xml.LaunchSuite;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;


public class TestNGLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

  public void launch(ILaunchConfiguration configuration,
                     String mode,
                     ILaunch launch,
                     IProgressMonitor monitor) 
    throws CoreException 
  {

    IJavaProject javaProject = getJavaProject(configuration);
    if((javaProject == null) || !javaProject.exists()) {
      abort(ResourceUtil.getString("TestNGLaunchConfigurationDelegate.error.invalidproject"), //$NON-NLS-1$
            null,
            IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT);
    }


    IVMInstall install = getVMInstall(configuration);
    IVMRunner  runner = install.getVMRunner(mode);
    if(runner == null) {
      abort(ResourceUtil.getFormattedString("TestNGLaunchConfigurationDelegate.error.novmrunner", //$NON-NLS-1$
                                            new String[] { install.getId() }),
            null,
            IJavaLaunchConfigurationConstants.ERR_VM_RUNNER_DOES_NOT_EXIST);
    }

    int port = SocketUtil.findFreePort();
    VMRunnerConfiguration runConfig = launchTypes(configuration,
                                                  launch,
                                                  javaProject,
                                                  port,
                                                  mode);
    setDefaultSourceLocator(launch, configuration);

    launch.setAttribute(TestNGLaunchConfigurationConstants.PORT, Integer.toString(port));
    launch.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                        javaProject.getElementName());
    launch.setAttribute(TestNGLaunchConfigurationConstants.TESTNG_RUN_NAME_ATTR,
                        getRunNameAttr(configuration));

    runner.run(runConfig, launch, monitor);
  }

  private static void ppp(String s) {
      System.out.println("[TestNGLaunchConfigurationDelegate] " + s);
  }

  protected VMRunnerConfiguration launchTypes(final ILaunchConfiguration configuration,
                                              ILaunch launch,
                                              final IJavaProject jproject,
                                              final int port,
                                              final String mode) throws CoreException {

    File   workingDir = verifyWorkingDirectory(configuration);
    String workingDirName = null;
    if(workingDir != null) {
      workingDirName = workingDir.getAbsolutePath();
    }

    // Program & VM args
    String                vmArgs = getVMArguments(configuration) + " "
      + TestNGLaunchConfigurationConstants.VM_ENABLEASSERTION_OPTION; // $NON-NLS-1$
    ExecutionArguments    execArgs = new ExecutionArguments(vmArgs, ""); //$NON-NLS-1$
    String[]              envp = DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);

    VMRunnerConfiguration runConfig = createVMRunner(configuration,
                                                     launch,
                                                     jproject,
                                                     port,
                                                     mode);
    runConfig.setVMArguments(execArgs.getVMArgumentsArray());
    runConfig.setWorkingDirectory(workingDirName);
    runConfig.setEnvironment(envp);

    Map vmAttributesMap = getVMSpecificAttributesMap(configuration);
    runConfig.setVMSpecificAttributesMap(vmAttributesMap);

    String[] bootpath = getBootpath(configuration);
    runConfig.setBootClassPath(bootpath);

    return runConfig;
  }

  /**
   * Add a VMRunner with a class path that includes org.eclipse.jdt.junit plugin.
   * In addition it adds the port for the RemoteTestRunner as an argument
   */
  protected VMRunnerConfiguration createVMRunner(final ILaunchConfiguration configuration,
                                                 ILaunch launch,
                                                 final IJavaProject jproject,
                                                 final int port,
                                                 final String runMode)
  throws CoreException {

    String javaVersion = ConfigurationHelper.getComplianceLevel(jproject, configuration);
    String[] classPath = createClassPath(configuration, javaVersion);
    String progArgs = getProgramArguments(configuration);
    VMRunnerConfiguration vmConfig = new VMRunnerConfiguration(RemoteTestNG.class.getName(), //$NON-NLS-1$
                                                               classPath);

    // insert the program arguments
    Vector                argv = new Vector(10);
    ExecutionArguments    execArgs = new ExecutionArguments("", progArgs); //$NON-NLS-1$
    String[]              pa = execArgs.getProgramArgumentsArray();
    for(int i = 0; i < pa.length; i++) {
      argv.add(pa[i]);
    }

    argv.add(TestNGCommandLineArgs.PORT_COMMAND_OPT);
    argv.add(Integer.toString(port));
    
    IProject project = jproject.getProject();

    if(!isJDK15(javaVersion)) {
      List     sourceDirs = JDTUtil.getSourceDirFileList(jproject);
      if(null != sourceDirs) {
        argv.add(TestNGCommandLineArgs.SRC_COMMAND_OPT);
        argv.add(Utils.toSinglePath(sourceDirs, ";")); //$NON-NLS-1$
      }
    }

    PreferenceStoreUtil storage= TestNGPlugin.getPluginPreferenceStore();
    argv.add(TestNGCommandLineArgs.OUTDIR_COMMAND_OPT);
    argv.add(storage.getOutputAbsolutePath(jproject).toOSString());
    
    String reporters= storage.getReporters(project.getName(), false);
    if(null != reporters && !"".equals(reporters)) {
      argv.add(TestNGCommandLineArgs.LISTENER_COMMAND_OPT);
      argv.add(reporters.replace(' ', ';'));
    }
    
    List contributors = ListenerContributorUtil.findReporterContributors();
    contributors.addAll( ListenerContributorUtil.findTestContributors() );
    StringBuffer reportersContributors = new StringBuffer();
    boolean isFirst = true;
    for (Iterator iterator = contributors.iterator(); iterator.hasNext();) {
    	Object contributor = iterator.next();
    	if( isFirst ) {
    		reportersContributors.append(contributor.getClass().getName());
    	} else {
    		reportersContributors.append(";" + contributor.getClass().getName());
    	}
    	isFirst = false;
    }
    if(!reportersContributors.toString().trim().equals("")) {
    	if( !argv.contains(TestNGCommandLineArgs.LISTENER_COMMAND_OPT) ) {
    		argv.add(TestNGCommandLineArgs.LISTENER_COMMAND_OPT);
            argv.add(reportersContributors.toString().trim());
    	} else {
    		String listeners = (String)argv.get(argv.size()-1);
    		listeners += (";" + reportersContributors.toString().trim());
    		argv.set(argv.size()-1, listeners);
    	}
    }
    
    boolean disabledReporters= storage.hasDisabledListeners(project.getName(), false);
    if(disabledReporters) {
      argv.add(TestNGCommandLineArgs.USE_DEFAULT_LISTENERS);
      argv.add("false");
    }
    
    List launchSuiteList = ConfigurationHelper.getLaunchSuites(jproject, configuration);
    List suiteList  = new ArrayList();
    List tempSuites = new ArrayList();

    File suiteDir =
        new File(System.getProperty("java.io.tmpdir") + File.separatorChar + "testng-eclipse");
    for(Iterator it = launchSuiteList.iterator(); it.hasNext();) {
      LaunchSuite launchSuite = (LaunchSuite) it.next();
      File suiteFile = launchSuite.save(suiteDir);

      suiteList.add(suiteFile.getAbsolutePath());

      if(launchSuite.isTemporary()) {
        suiteFile.deleteOnExit();
        tempSuites.add(suiteFile.getAbsolutePath());
      }
    }

    if(null != suiteList) {
      String suites = toPath(suiteList, " "); //$NON-NLS-1$
      argv.add(suites); //$NON-NLS-1$
      
      launch.setAttribute(TestNGLaunchConfigurationConstants.TEMP_SUITE_LIST, Utils.listToString(tempSuites));
    }

    ppp(argv);
    ppp(Arrays.asList(classPath));
    vmConfig.setProgramArguments((String[]) argv.toArray(new String[argv.size()]));

    return vmConfig;
  }

  /**
   * @param javaVersion
   * @return
   */
  private boolean isJDK15(String javaVersion) {
    return TestNG.JDK_ANNOTATION_TYPE.equalsIgnoreCase(javaVersion) || javaVersion.startsWith("1.5") || javaVersion.startsWith("1.6");
  }

  private String[] createClassPath(ILaunchConfiguration configuration, String javaVersion)
  throws CoreException {
    URL      url = Platform.getBundle(TestNGPlugin.PLUGIN_ID).getEntry("/"); //$NON-NLS-1$

    String[] cp = getClasspath(configuration);
    String[] classPath = null;
    String testngJarLocation= getTestNGLibraryVersion(javaVersion);
    String testngJarName= testngJarLocation.indexOf('/') != -1 ? testngJarLocation.substring(testngJarLocation.indexOf('/') + 1) : testngJarLocation;
    boolean donotappendjar= false;
    String projectName= getJavaProjectName(configuration);
    if(null != projectName) {
       donotappendjar= TestNGPlugin.getPluginPreferenceStore().getUseProjectJar(projectName);
    }
    
    int addedSize= 2;
    if(donotappendjar) {
      addedSize= 1;
    }
    else {
      for(int i= 0; i< cp.length; i++) {
        if(cp[i].endsWith(testngJarName)) {
          addedSize= 1;
          break;
        }
      }
    }

    try {
      if(Platform.inDevelopmentMode()) {

        // we first try the bin output folder
        List entries = new ArrayList();

        try {
          entries.add(FileLocator.toFileURL(new URL(url, "build/classes")).getFile()); //$NON-NLS-1$
        }
        catch(IOException e3) {
          try {
            entries.add(FileLocator.toFileURL(new URL(url, "eclipse-testng.jar")).getFile()); //$NON-NLS-1$
          }
          catch(IOException e4) {
            ;
          }
        }
        if(addedSize == 2) {
          entries.add(FileLocator.toFileURL(new URL(url, testngJarLocation)).getFile()); //$NON-NLS-1$
        }

        Assert.isTrue(entries.size() == addedSize, "Required JARs available"); //$NON-NLS-1$

        classPath = new String[cp.length + entries.size()];

        Object[] jea = entries.toArray();
        System.arraycopy(cp, 0, classPath, addedSize, cp.length);
        System.arraycopy(jea, 0, classPath, 0, jea.length);
      }
      else {
        classPath = new String[cp.length + addedSize];
        System.arraycopy(cp, 0, classPath, addedSize, cp.length);
        classPath[0] = FileLocator.toFileURL(new URL(url, "eclipse-testng.jar")).getFile(); //$NON-NLS-1$
        
        if(addedSize == 2) {
          classPath[1] = FileLocator.toFileURL(new URL(url, testngJarLocation)).getFile();
        }
      }
    }
    catch(IOException ioe) {
      TestNGPlugin.log(ioe);
      abort("Cannot create runtime classpath", ioe, 1000); //$NON-NLS-1$
    }

    return classPath;
  }

  private String getRunNameAttr(ILaunchConfiguration configuration) throws CoreException {
    LaunchType runType = ConfigurationHelper.getType(configuration);
    
    switch(runType) {
      case CLASS:
        return "test class";
      case GROUP:
        return "groups";
      case SUITE:
        return "suite";
      case PACKAGE:
          return "package";  
      default:
        return "from context";
    }
  }

  private String getTestNGLibraryVersion(final String javaVersion) {
    String testngLib = null;
    if(isJDK15(javaVersion)) {
      testngLib = ResourceUtil.getString("TestNG.jdk15.library"); //$NON-NLS-1$
    }
    else {
      testngLib = ResourceUtil.getString("TestNG.jdk14.library"); //$NON-NLS-1$
    }

    return testngLib;
  }

  private String toPath(final List content,
                        final String separator) {
    if((null == content) || content.isEmpty()) {
      return ""; //$NON-NLS-1$
    }

    final StringBuffer buf = new StringBuffer();

    for(int i = 0; i < content.size(); i++) {
      buf.append((String) content.get(i));

      if(i < (content.size() - 1)) {
        buf.append(separator);
      }
    }

    return buf.toString();
  }

  private static void ppp(final Object msg) {
    TestNGPlugin.log(new Status(IStatus.INFO, 
                                TestNGPlugin.PLUGIN_ID, 
                                1, 
                                String.valueOf(msg), 
                                null)
    );
  }
}
