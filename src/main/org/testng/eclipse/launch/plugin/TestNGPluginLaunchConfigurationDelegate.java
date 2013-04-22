package org.testng.eclipse.launch.plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.PDEMessages;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.EclipsePluginValidationOperation;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchConfigurationHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchPluginValidator;
import org.eclipse.pde.internal.launching.launcher.LauncherUtils;
import org.eclipse.pde.internal.launching.launcher.VMHelper;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.launching.PDESourcePathProvider;
import org.osgi.framework.Version;
import org.testng.eclipse.launch.TestNGLaunchConfigurationDelegate;

/** A launcher for testng plugin test configurations
 * 
 * @author Florian Hackenberger (florian.hackenberger@acoveo.com)
 *
 */
@SuppressWarnings({"restriction", "rawtypes", "unchecked"})
public class TestNGPluginLaunchConfigurationDelegate extends TestNGLaunchConfigurationDelegate {
  
  /**
   * To avoid duplicating variable substitution (and duplicate prompts)
   * this variable will store the substituted workspace location.
   */
  private String fWorkspaceLocation;

  protected File fConfigDir = null;

  // used to generate the dev classpath entries
  // key is bundle ID, value is a model
  private Map<String, IPluginModelBase> fAllBundles;
  // key is a model, value is startLevel:autoStart
  private Map<IPluginModelBase, String> fModels;

  @Override
  public IVMRunner getVMRunner(ILaunchConfiguration configuration, String mode) throws CoreException {
    IVMInstall launcher = VMHelper.createLauncher(configuration);
    return launcher.getVMRunner(mode);
  }

  @Override
  public String getMainTypeName(ILaunchConfiguration configuration) throws CoreException {
    if (TargetPlatformHelper.getTargetVersion() >= 3.3)
      return "org.eclipse.equinox.launcher.Main"; //$NON-NLS-1$
    return "org.eclipse.core.launcher.Main"; //$NON-NLS-1$
  }

  private String getTestPluginId(ILaunchConfiguration configuration) throws CoreException {
    IPluginModelBase model = PluginRegistry.findModel(javaProject.getProject());
    if (model == null)
      abort(NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_notaplugin, javaProject.getProject().getName()), null, IStatus.OK);
    if (model instanceof IFragmentModel)
      return ((IFragmentModel) model).getFragment().getPluginId();

    return model.getPluginBase().getId();
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.jdt.internal.junit.launcher.JUnitBaseLaunchConfiguration#abort(java.lang.String, java.lang.Throwable, int)
   */
  protected void abort(String message, Throwable exception, int code) throws CoreException {
    throw new CoreException(new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, code, message, exception));
  }

  @Override
  protected void collectExecutionArguments(ILaunchConfiguration configuration, ILaunch launch,
      List<String> vmArguments, List<String> programArgs, String runMode) throws CoreException {
    // Specify the TestNG Plug-in test application to launch
    programArgs.add("-application"); //$NON-NLS-1$
    String application = getApplication(configuration);
  
    programArgs.add(application);

    // If a product is specified, then add it to the program args
    if (configuration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
      programArgs.add("-product"); //$NON-NLS-1$
      programArgs.add(configuration.getAttribute(IPDELauncherConstants.PRODUCT, "")); //$NON-NLS-1$
    } else {
      // Specify the application to test
      String defaultApplication = TargetPlatform.getDefaultApplication();
      if (ITestNGPluginLauncherConstants.CORE_TEST_APPLICATION.equals(application)) {
        // If we are launching the core test application we don't need a test app
        defaultApplication = null;
      } else if (ITestNGPluginLauncherConstants.NON_UI_THREAD_APPLICATION.equals(application)) {
        // When running in a non-UI thread, run the core test app to avoid opening the workbench
        defaultApplication = ITestNGPluginLauncherConstants.CORE_TEST_APPLICATION;
      }
      String testApplication = configuration.getAttribute(IPDELauncherConstants.APP_TO_TEST, defaultApplication);
      if (testApplication != null) {
        programArgs.add("-testApplication"); //$NON-NLS-1$
        programArgs.add(testApplication);
      }
    }

    // Specify the location of the runtime workbench
    if (fWorkspaceLocation == null) {
      fWorkspaceLocation = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
    }
    if (fWorkspaceLocation.length() > 0) {
      programArgs.add("-data"); //$NON-NLS-1$
      programArgs.add(fWorkspaceLocation);
    }

    // Create the platform configuration for the runtime workbench
    String productID = LaunchConfigurationHelper.getProductID(configuration);
    LaunchConfigurationHelper.createConfigIniFile(configuration, productID, fAllBundles, fModels, getConfigurationDirectory(configuration));
    TargetPlatformHelper.checkPluginPropertiesConsistency(fAllBundles, getConfigurationDirectory(configuration));

    programArgs.add("-configuration"); //$NON-NLS-1$
    programArgs.add("file:" + new Path(getConfigurationDirectory(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$

    // Specify the output folder names
    programArgs.add("-dev"); //$NON-NLS-1$
    programArgs.add(ClasspathHelper.getDevEntriesProperties(getConfigurationDirectory(configuration).toString() + "/dev.properties", fAllBundles)); //$NON-NLS-1$

    // necessary for PDE to know how to load plugins when target platform = host platform
    // see PluginPathFinder.getPluginPaths()
    IPluginModelBase base = findPlugin(PDECore.PLUGIN_ID);
    if (base != null && VersionUtil.compareMacroMinorMicro(base.getBundleDescription().getVersion(), new Version("3.3.1")) < 0) //$NON-NLS-1$
      programArgs.add("-pdelaunch"); //$NON-NLS-1$        

    // Create the .options file if tracing is turned on
    if (configuration.getAttribute(IPDELauncherConstants.TRACING, false) && !IPDELauncherConstants.TRACING_NONE.equals(configuration.getAttribute(IPDELauncherConstants.TRACING_CHECKED, (String) null))) {
      programArgs.add("-debug"); //$NON-NLS-1$
      String path = getConfigurationDirectory(configuration).getPath() + IPath.SEPARATOR + ICoreConstants.OPTIONS_FILENAME;
      programArgs.add(LaunchArgumentsHelper.getTracingFileArgument(configuration, path));
    }

    // add the program args specified by the user
    String[] userArgs = LaunchArgumentsHelper.getUserProgramArgumentArray(configuration);
    for (int i = 0; i < userArgs.length; i++) {
      // be forgiving if people have tracing turned on and forgot
      // to remove the -debug from the program args field.
      if (userArgs[i].equals("-debug") && programArgs.contains("-debug")) //$NON-NLS-1$ //$NON-NLS-2$
        continue;
      programArgs.add(userArgs[i]);
    }

    if (!configuration.getAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, false)) {
      if (!programArgs.contains("-os")) { //$NON-NLS-1$
        programArgs.add("-os"); //$NON-NLS-1$
        programArgs.add(TargetPlatform.getOS());
      }
      if (!programArgs.contains("-ws")) { //$NON-NLS-1$
        programArgs.add("-ws"); //$NON-NLS-1$
        programArgs.add(TargetPlatform.getWS());
      }
      if (!programArgs.contains("-arch")) { //$NON-NLS-1$
        programArgs.add("-arch"); //$NON-NLS-1$
        programArgs.add(TargetPlatform.getOSArch());
      }
    }

    programArgs.add("-testpluginname"); //$NON-NLS-1$
    programArgs.add(getTestPluginId(configuration));
    
    if (runMode.equals(ILaunchManager.DEBUG_MODE) && configuration.getAttribute(ITestNGPluginLauncherConstants.KEEP_RUNNING, false)) {
      programArgs.add("-keeprunning");
    }
    
    super.collectExecutionArguments(configuration, launch, vmArguments, programArgs, runMode);
  }
  
  /**
   * Returns the application to launch plug-in tests with
   * 
   * @since 3.5
   * 
   * @param configuration
   * @return the application
   */
  protected String getApplication(ILaunchConfiguration configuration) {
    String application = null;

    boolean shouldRunInUIThread = true;
    try {
      shouldRunInUIThread = configuration.getAttribute(IPDELauncherConstants.RUN_IN_UI_THREAD, true);
    } catch (CoreException e) {
    }

    if (!shouldRunInUIThread) {
      return ITestNGPluginLauncherConstants.NON_UI_THREAD_APPLICATION;
    }

    try {
      // if application is set, it must be a headless app.
      application = configuration.getAttribute(IPDELauncherConstants.APPLICATION, (String) null);
    } catch (CoreException e) {
    }

    // if application is not set, we should launch the default UI test app
    // Check to see if we should launch the legacy UI app
    if (application == null) {
      IPluginModelBase model = (IPluginModelBase) fAllBundles.get("org.testng.eclipse.pde.runtime"); //$NON-NLS-1$
      BundleDescription desc = model != null ? model.getBundleDescription() : null;
      if (desc != null) {
        Version version = desc.getVersion();
        int major = version.getMajor();
        // launch legacy UI app only if we are launching a target that does 
        // not use the new application model and we are launching with a 
        // org.eclipse.pde.junit.runtime whose version is >= 3.3
        if (major >= 3 && version.getMinor() >= 3 && !TargetPlatformHelper.usesNewApplicationModel()) {
          application = ITestNGPluginLauncherConstants.LEGACY_UI_TEST_APPLICATION;
        }
      }
    }

    // launch the UI test application
    if (application == null)
      application = ITestNGPluginLauncherConstants.UI_TEST_APPLICATION;
    return application;
  }
  
  private IPluginModelBase findPlugin(String id) throws CoreException {
    IPluginModelBase model = PluginRegistry.findModel(id);
    if (model == null)
      model = PDECore.getDefault().findPluginInHost(id);
    if (model == null)
      abort(NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_missingPlugin, id), null, IStatus.OK);
    return model;
  }
  
  /*
   * (non-Javadoc)
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getProgramArguments(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public String getProgramArguments(ILaunchConfiguration configuration) throws CoreException {
    return LaunchArgumentsHelper.getUserProgramArguments(configuration);
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getVMArguments(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
    String vmArgs = LaunchArgumentsHelper.getUserVMArguments(configuration);

    // necessary for PDE to know how to load plugins when target platform = host platform
    IPluginModelBase base = (IPluginModelBase) fAllBundles.get(PDECore.PLUGIN_ID);
    if (base != null && VersionUtil.compareMacroMinorMicro(base.getBundleDescription().getVersion(), new Version("3.3.1")) >= 0) { //$NON-NLS-1$
      vmArgs = concatArg(vmArgs, "-Declipse.pde.launch=true"); //$NON-NLS-1$
    }
    // For p2 target, add "-Declipse.p2.data.area=@config.dir/p2" unless already specified by user
    if (fAllBundles.containsKey("org.eclipse.equinox.p2.core")) { //$NON-NLS-1$
      if (vmArgs.indexOf("-Declipse.p2.data.area=") < 0) { //$NON-NLS-1$
        vmArgs = concatArg(vmArgs, "-Declipse.p2.data.area=@config.dir" + File.separator + "p2"); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
    return vmArgs;
  }

  /**
   * Returns the result of concatenating the given argument to the
   * specified vmArgs.
   * 
   * @param vmArgs existing VM arguments
   * @param arg argument to concatenate
   * @return result of concatenation
   */
  private String concatArg(String vmArgs, String arg) {
    if (vmArgs.length() > 0 && !vmArgs.endsWith(" ")) //$NON-NLS-1$
      vmArgs = vmArgs.concat(" "); //$NON-NLS-1$
    return vmArgs.concat(arg);
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getEnvironment(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public String[] getEnvironment(ILaunchConfiguration configuration) throws CoreException {
    return DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getClasspath(org.eclipse.debug.core.ILaunchConfiguration)
   */
  @Override
  public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
    String[] classpath = LaunchArgumentsHelper.constructClasspath(configuration);
    if (classpath == null) {
      abort(PDEMessages.WorkbenchLauncherConfigurationDelegate_noStartup, null, IStatus.OK);
    }
    return classpath;
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getWorkingDirectory(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public File getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
    return LaunchArgumentsHelper.getWorkingDirectory(configuration);
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getVMSpecificAttributesMap(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public Map getVMSpecificAttributesMap(ILaunchConfiguration configuration) throws CoreException {
    return LaunchArgumentsHelper.getVMSpecificAttributesMap(configuration);
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#setDefaultSourceLocator(org.eclipse.debug.core.ILaunch, org.eclipse.debug.core.ILaunchConfiguration)
   */
  protected void setDefaultSourceLocator(ILaunch launch, ILaunchConfiguration configuration) throws CoreException {
    ILaunchConfigurationWorkingCopy wc = null;
    if (configuration.isWorkingCopy()) {
      wc = (ILaunchConfigurationWorkingCopy) configuration;
    } else {
      wc = configuration.getWorkingCopy();
    }
    String id = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, (String) null);
    if (!PDESourcePathProvider.ID.equals(id)) {
      wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, PDESourcePathProvider.ID);
      wc.doSave();
    }

    manageLaunch(launch);
  }

  /**
   * Returns the location of the configuration area
   * 
   * @param configuration
   *            the launch configuration
   * @return a directory where the configuration area is located
   */
  protected File getConfigurationDirectory(ILaunchConfiguration configuration) {
    if (fConfigDir == null)
      fConfigDir = LaunchConfigurationHelper.getConfigurationArea(configuration);
    return fConfigDir;
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getBuildOrder(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
   */
  protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
    return computeBuildOrder(LaunchPluginValidator.getAffectedProjects(configuration));
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#getProjectsForProblemSearch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String)
   */
  protected IProject[] getProjectsForProblemSearch(ILaunchConfiguration configuration, String mode) throws CoreException {
    return LaunchPluginValidator.getAffectedProjects(configuration);
  }
    /**
     * Adds a listener to the launch to be notified at interesting launch lifecycle
     * events such as when the launch terminates.
     * 
     * @param launch
     *      the launch      
     */
    protected void manageLaunch(ILaunch launch) {
      PDELaunchingPlugin.getDefault().getLaunchListener().manage(launch);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate#preLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
     */
  @Override
  protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor) throws CoreException {
    super.preLaunchCheck(configuration, launch, monitor);
    fWorkspaceLocation = null;
    fModels = BundleLauncherHelper.getMergedBundleMap(configuration, false);
    fAllBundles = new HashMap<String,IPluginModelBase>(fModels.size());
    Iterator<?> iter = fModels.keySet().iterator();
    while (iter.hasNext()) {
      IPluginModelBase model = (IPluginModelBase) iter.next();
      fAllBundles.put(model.getPluginBase().getId(), model);
    }

    // implicitly add the plug-ins required for JUnit testing if necessary
    String[] requiredPlugins = getRequiredPlugins(configuration);
    for (int i = 0; i < requiredPlugins.length; i++) {
      String id = requiredPlugins[i];
      if (!fAllBundles.containsKey(id)) {
        IPluginModelBase model = findPlugin(id);
        fAllBundles.put(id, model);
        fModels.put(model, "default:default"); //$NON-NLS-1$
      }
    }

    boolean autoValidate = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false);
    monitor.beginTask("", autoValidate ? 3 : 4); //$NON-NLS-1$
    if (autoValidate)
      validatePluginDependencies(configuration, new SubProgressMonitor(monitor, 1));
    validateProjectDependencies(configuration, new SubProgressMonitor(monitor, 1));
    LauncherUtils.setLastLaunchMode(launch.getLaunchMode());
    clear(configuration, new SubProgressMonitor(monitor, 1));
    launch.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, getConfigurationDirectory(configuration).toString());
    synchronizeManifests(configuration, new SubProgressMonitor(monitor, 1));
  }
  
  private String[] getRequiredPlugins(ILaunchConfiguration configuration) {
    return new String[] {"org.testng", "org.testng.eclipse.pde.runtime"}; //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Checks for old-style plugin.xml files that have become stale since the last launch.
   * For any stale plugin.xml files found, the corresponding MANIFEST.MF is deleted 
   * from the runtime configuration area so that it gets regenerated upon startup.
   * 
   * @param configuration
   *            the launch configuration
   * @param monitor
   *            the progress monitor
   */
  protected void synchronizeManifests(ILaunchConfiguration configuration, IProgressMonitor monitor) {
    LaunchConfigurationHelper.synchronizeManifests(configuration, getConfigurationDirectory(configuration));
    monitor.done();
  }

  /**
   * Clears the workspace prior to launching if the workspace exists and the option to 
   * clear it is turned on.  Also clears the configuration area if that option is chosen.
   * 
   * @param configuration
   *            the launch configuration
   * @param monitor
   *            the progress monitor
   * @throws CoreException
   *             if unable to retrieve launch attribute values
   * @since 3.3
   */
  protected void clear(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    if (fWorkspaceLocation == null) {
      fWorkspaceLocation = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
    }

    SubMonitor subMon = SubMonitor.convert(monitor, 50);

    // Clear workspace and prompt, if necessary
    if (!LauncherUtils.clearWorkspace(configuration, fWorkspaceLocation, subMon.newChild(25))) {
      throw new CoreException(Status.CANCEL_STATUS);
    }

    subMon.setWorkRemaining(25);
    if (subMon.isCanceled()) {
      throw new CoreException(Status.CANCEL_STATUS);
    }

    // clear config area, if necessary
    if (configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false))
      CoreUtility.deleteContent(getConfigurationDirectory(configuration), subMon.newChild(25));

    subMon.done();
  }

  /**
   * Checks if the Automated Management of Dependencies option is turned on.
   * If so, it makes aure all manifests are updated with the correct dependencies.
   * 
   * @param configuration
   *            the launch configuration
   * @param monitor
   *            a progress monitor
   */
  protected void validateProjectDependencies(ILaunchConfiguration configuration, IProgressMonitor monitor) {
    LauncherUtils.validateProjectDependencies(configuration, monitor);
  }

  /**
   * Validates inter-bundle dependencies automatically prior to launching
   * if that option is turned on.
   * 
   * @param configuration
   *            the launch configuration
   * @param monitor
   *            a progress monitor
   */
  protected void validatePluginDependencies(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
    EclipsePluginValidationOperation op = new EclipsePluginValidationOperation(configuration);
    LaunchPluginValidator.runValidationOperation(op, monitor);
  }

}
