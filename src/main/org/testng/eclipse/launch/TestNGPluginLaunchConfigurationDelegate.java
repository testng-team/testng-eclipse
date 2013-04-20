package org.testng.eclipse.launch;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.ClasspathHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.launching.PDEMessages;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.EclipsePluginValidationOperation;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchConfigurationHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchPluginValidator;
import org.eclipse.pde.internal.launching.launcher.LauncherUtils;
import org.eclipse.pde.internal.launching.launcher.VMHelper;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.osgi.framework.Version;

/** A launcher for testng plugin test configurations
 * 
 * @author Florian Hackenberger (florian.hackenberger@acoveo.com)
 *
 */
@SuppressWarnings("restriction")
public class TestNGPluginLaunchConfigurationDelegate extends TestNGLaunchConfigurationDelegate {
	protected static String TestNGProgramBlock_headless = "No application [Headless]";
	protected static String CORE_TEST_APPLICATION = "org.testng.eclipse.runtime.coretestapplication"; //$NON-NLS-1$
	protected static String UI_TEST_APPLICATION = "org.testng.eclipse.runtime.uitestapplication"; //$NON-NLS-1$
	protected static String LEGACY_UI_TEST_APPLICATION = "org.testng.eclipse.runtime.legacytestapplication"; //$NON-NLS-1$
	private static String[] REQUIRED_PLUGINS = { "org.testng", "org.testng.eclipse.runtime" }; //$NON-NLS-1$ //$NON-NLS-2$

	protected File fConfigDir = null;

	private Map<String, IPluginModelBase> fPluginMap;
	
	// key is a model, value is startLevel:autoStart
	private Map<?,?> fModels;

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

  @Override
	protected void preLaunchCheck(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		super.preLaunchCheck(configuration, launch, monitor);
		// Get the list of plug-ins to run
		fModels = BundleLauncherHelper.getMergedBundleMap(configuration, false);
		fPluginMap = new HashMap<String,IPluginModelBase>(fModels.size());
		Iterator<?> iter = fModels.keySet().iterator();
		while (iter.hasNext()) {
			IPluginModelBase model = (IPluginModelBase) iter.next();
			fPluginMap.put(model.getPluginBase().getId(), model);
		}

		// implicitly add the plug-ins required for JUnit testing if necessary
		for (int i = 0; i < REQUIRED_PLUGINS.length; i++) {
			String id = REQUIRED_PLUGINS[i];
			if (!fPluginMap.containsKey(id)) {
				fPluginMap.put(id, findPlugin(id));
			}
		}

		boolean autoValidate = configuration.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false);
		monitor.beginTask("", autoValidate ? 3 : 4); //$NON-NLS-1$
		if (autoValidate)
			validatePluginDependencies(configuration, new SubProgressMonitor(monitor, 1));
		validateProjectDependencies(configuration, new SubProgressMonitor(monitor, 1));
		clear(configuration, new SubProgressMonitor(monitor, 1));
		launch.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, getConfigurationDirectory(configuration).toString());
		synchronizeManifests(configuration, new SubProgressMonitor(monitor, 1));
	}

  @Override
	protected void collectExecutionArguments(ILaunchConfiguration configuration, ILaunch launch,
			List<String> vmArguments, List<String> programArguments) throws CoreException {
		// Specify the JUnit Plug-in test application to launch
		programArguments.add("-application"); //$NON-NLS-1$
		String application = null;
		try {
			// if application is set, it must be a headless app.
			application = configuration.getAttribute(IPDELauncherConstants.APPLICATION, (String) null);
		} catch (CoreException e) {
		}

		// if application is not set, we should launch the default UI test app
		// Check to see if we should launch the legacy UI app
		if (application == null) {
			IPluginModelBase model = (IPluginModelBase) fPluginMap.get("org.testng.eclipse.runtime"); //$NON-NLS-1$
			BundleDescription desc = model != null ? model.getBundleDescription() : null;
			if (desc != null) {
				Version version = desc.getVersion();
				int major = version.getMajor();
				// launch legacy UI app only if we are launching a target that
				// does
				// not use the new application model and we are launching with a
				// org.eclipse.pde.junit.runtime whose version is >= 3.3
				if (major >= 3 && version.getMinor() >= 3 && !TargetPlatformHelper.usesNewApplicationModel()) {
					application = LEGACY_UI_TEST_APPLICATION;
				}
			}
		}

		// launch the UI test application
		if (application == null)
			application = "org.testng.eclipse.runtime.nonuithreadtestapplication";

		programArguments.add(application);

		// If a product is specified, then add it to the program args
		if (configuration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
			programArguments.add("-product"); //$NON-NLS-1$
			programArguments.add(configuration.getAttribute(IPDELauncherConstants.PRODUCT, "")); //$NON-NLS-1$
		} else {
			// Specify the application to test
			String defaultApplication = CORE_TEST_APPLICATION.equals(application) ? null : TargetPlatform
					.getDefaultApplication();
			String testApplication = configuration.getAttribute(IPDELauncherConstants.APP_TO_TEST, defaultApplication);
			if (testApplication != null) {
				programArguments.add("-testApplication"); //$NON-NLS-1$
				programArguments.add(testApplication);
			}
		}

		// Specify the location of the runtime workbench
		String targetWorkspace = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
		if (targetWorkspace.length() > 0) {
			programArguments.add("-data"); //$NON-NLS-1$
			programArguments.add(targetWorkspace);
		}

		// Create the platform configuration for the runtime workbench
		String productID = LaunchConfigurationHelper.getProductID(configuration);
		LaunchConfigurationHelper.createConfigIniFile(configuration, productID, fPluginMap,
				fModels, getConfigurationDirectory(configuration));
		String brandingId = LaunchConfigurationHelper.getContributingPlugin(productID);
		TargetPlatform.createPlatformConfiguration(getConfigurationDirectory(configuration),
				(IPluginModelBase[]) fPluginMap.values().toArray(new IPluginModelBase[fPluginMap.size()]),
				brandingId != null ? (IPluginModelBase) fPluginMap.get(brandingId) : null);
		TargetPlatformHelper.checkPluginPropertiesConsistency(fPluginMap, getConfigurationDirectory(configuration));

		programArguments.add("-configuration"); //$NON-NLS-1$
		programArguments
				.add("file:" + new Path(getConfigurationDirectory(configuration).getPath()).addTrailingSeparator().toString()); //$NON-NLS-1$

		// Specify the output folder names
		programArguments.add("-dev"); //$NON-NLS-1$
		programArguments.add(ClasspathHelper.getDevEntriesProperties(getConfigurationDirectory(configuration)
				.toString()
				+ "/dev.properties", fPluginMap)); //$NON-NLS-1$

		// necessary for PDE to know how to load plugins when target platform =
		// host platform
		// see PluginPathFinder.getPluginPaths()
		IPluginModelBase base = findPlugin(PDECore.PLUGIN_ID);
		if (base != null
				&& VersionUtil.compareMacroMinorMicro(base.getBundleDescription().getVersion(), new Version("3.3.1")) < 0) //$NON-NLS-1$
			programArguments.add("-pdelaunch"); //$NON-NLS-1$				

		// Create the .options file if tracing is turned on
		if (configuration.getAttribute(IPDELauncherConstants.TRACING, false)
				&& !IPDELauncherConstants.TRACING_NONE.equals(configuration.getAttribute(
						IPDELauncherConstants.TRACING_CHECKED, (String) null))) {
			programArguments.add("-debug"); //$NON-NLS-1$
			String path = getConfigurationDirectory(configuration).getPath() + IPath.SEPARATOR + ".options"; //$NON-NLS-1$
			programArguments.add(LaunchArgumentsHelper.getTracingFileArgument(configuration, path));
		}

		// add the program args specified by the user
		String[] userArgs = LaunchArgumentsHelper.getUserProgramArgumentArray(configuration);
		for (int i = 0; i < userArgs.length; i++) {
			// be forgiving if people have tracing turned on and forgot
			// to remove the -debug from the program args field.
			if (userArgs[i].equals("-debug") && programArguments.contains("-debug")) //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			programArguments.add(userArgs[i]);
		}

		if (!configuration.getAttribute(IPDEUIConstants.APPEND_ARGS_EXPLICITLY, false)) {
			if (!programArguments.contains("-os")) { //$NON-NLS-1$
				programArguments.add("-os"); //$NON-NLS-1$
				programArguments.add(TargetPlatform.getOS());
			}
			if (!programArguments.contains("-ws")) { //$NON-NLS-1$
				programArguments.add("-ws"); //$NON-NLS-1$
				programArguments.add(TargetPlatform.getWS());
			}
			if (!programArguments.contains("-arch")) { //$NON-NLS-1$
				programArguments.add("-arch"); //$NON-NLS-1$
				programArguments.add(TargetPlatform.getOSArch());
			}
		}

		programArguments.add("-testpluginname"); //$NON-NLS-1$
		programArguments.add(getTestPluginId(configuration));
		
		super.collectExecutionArguments(configuration, launch, vmArguments, programArguments);
	}
  
  @Override
  public String[] getClasspath(ILaunchConfiguration configuration)
      throws CoreException {
    String[] classpath = LaunchArgumentsHelper.constructClasspath(configuration);
    if (classpath == null) {
      abort(PDEMessages.WorkbenchLauncherConfigurationDelegate_noStartup, null, IStatus.OK);
    }
    return classpath;
  }

	private String getTestPluginId(ILaunchConfiguration configuration) throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel(javaProject.getProject());
		if (model == null)
			abort(
					NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_notaplugin, javaProject.getProject()
							.getName()), null, IStatus.OK);
		if (model instanceof IFragmentModel)
			return ((IFragmentModel) model).getFragment().getPluginId();

		return model.getPluginBase().getId();
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

	/**
	 * Checks for old-style plugin.xml files that have become stale since the
	 * last launch. For any stale plugin.xml files found, the corresponding
	 * MANIFEST.MF is deleted from the runtime configuration area so that it
	 * gets regenerated upon startup.
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
	 * Clears the workspace prior to launching if the workspace exists and the
	 * option to clear it is turned on. Also clears the configuration area if
	 * that option is chosen.
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
		String workspace = LaunchArgumentsHelper.getWorkspaceLocation(configuration);
		// Clear workspace and prompt, if necessary
		if (!LauncherUtils.clearWorkspace(configuration, workspace, new SubProgressMonitor(monitor, 1))) {
			monitor.setCanceled(true);
			return;
		}

		// clear config area, if necessary
		if (configuration.getAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, false))
			CoreUtility.deleteContent(getConfigurationDirectory(configuration));
	}

	/**
	 * Checks if the Automated Management of Dependencies option is turned on.
	 * If so, it makes sure all manifests are updated with the correct
	 * dependencies.
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
	 * Validates inter-bundle dependencies automatically prior to launching if
	 * that option is turned on.
	 * 
	 * @param configuration
	 *            the launch configuration
	 * @param monitor
	 *            a progress monitor
	 */
	protected void validatePluginDependencies(ILaunchConfiguration configuration, IProgressMonitor monitor)
			throws CoreException {
		EclipsePluginValidationOperation op = new EclipsePluginValidationOperation(configuration);
		LaunchPluginValidator.runValidationOperation(op, monitor);
	}

	private IPluginModelBase findPlugin(String id) throws CoreException {
		IPluginModelBase model = PluginRegistry.findModel(id);
		if (model == null)
			model = PDECore.getDefault().findPluginInHost(id);
		if (model == null)
			abort(NLS.bind(PDEMessages.JUnitLaunchConfiguration_error_missingPlugin, id), null, IStatus.OK);
		return model;
	}
}
