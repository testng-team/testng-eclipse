package org.testng.eclipse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.testng.eclipse.launch.ITestNGLaunchConfigurationProvider;
import org.testng.eclipse.ui.TestRunnerViewPart;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.PreferenceStoreUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.remote.RemoteTestNG;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The plug-in runtime class for the TestNG plug-in.
 * 
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class TestNGPlugin extends AbstractUIPlugin implements ILaunchListener {
  /**
   * The single instance of this plug-in runtime class.
   */
  private static TestNGPlugin m_pluginInstance = null;

  public static final String PLUGIN_ID = "org.testng.eclipse"; //$NON-NLS-1$
  private static URL m_fgIconBaseURL;
  
  private static boolean m_isStopped = false;

  /**
   * Use to track new launches. We need to do this
   * so that we only attach a TestRunner once to a launch.
   * Once a test runner is connected it is removed from the set.
   */
  private AbstractSet<ILaunch> n_trackedLaunches = new HashSet<>(20);

  private BundleContext fBundleContext;
  private PreferenceStoreUtil m_preferenceUtil;

  public TestNGPlugin() {
    m_pluginInstance = this;
    try {
      m_fgIconBaseURL = new URL(Platform.getBundle(PLUGIN_ID).getEntry("/"), "icons/full/"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    catch(MalformedURLException e) {
      ;
    }
  }

  public static TestNGPlugin getDefault() {
    return m_pluginInstance;
  }

  public static String getPluginId() {
    return PLUGIN_ID;
  }

  public static PreferenceStoreUtil getPluginPreferenceStore() {
    return m_pluginInstance.m_preferenceUtil; 
  }
  
  public static void log(String s) {
    log(new Status(IStatus.INFO, getPluginId(), IStatus.OK, s, null)); //$NON-NLS-1$
  }

  public static void log(Throwable e) {
    log(new Status(IStatus.ERROR, getPluginId(), IStatus.ERROR, "Error", e)); //$NON-NLS-1$
  }

  public static void log(IStatus status) {
    getDefault().getLog().log(status);
  }

  /**
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    fBundleContext= context;
    ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
    launchManager.addLaunchListener(this);
    m_isStopped = false;
    m_preferenceUtil= new PreferenceStoreUtil(getPreferenceStore());
  }

  /**
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    try {
      // Dispose the font
      if (null != BOLD_FONT) {
        BOLD_FONT.dispose();
      }
      m_isStopped = true;
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      launchManager.removeLaunchListener(this);
    }
    finally {
      super.stop(context);
      fBundleContext= null;
    }
  }

  public Object getService(String serviceName) {
    ServiceReference reference= fBundleContext.getServiceReference(serviceName);
    if (reference == null)
      return null;
    return fBundleContext.getService(reference);
  }

  public static boolean isStopped() {
    return m_isStopped;
  }
  
  /**
   * @see org.eclipse.debug.core.ILaunchListener#launchRemoved(org.eclipse.debug.core.ILaunch)
   */
  public void launchRemoved(final ILaunch launch) {
    n_trackedLaunches.remove(launch);
    asyncExec(new Runnable() {
        public void run() {
          TestRunnerViewPart testRunnerViewPart = findTestRunnerViewPartInActivePage();
          if((testRunnerViewPart != null)
             && testRunnerViewPart.isCreated()
             && launch.equals(testRunnerViewPart.getLastLaunch())) {

            testRunnerViewPart.reset();
          }
        }
      });
  }

  public static void asyncExec(Runnable runnable) {
    SWTUtil.getDisplay().asyncExec(runnable);
  }

  /**
   * @see org.eclipse.debug.core.ILaunchListener#launchAdded(org.eclipse.debug.core.ILaunch)
   */
  public void launchAdded(final ILaunch launch) {
    n_trackedLaunches.add(launch);
  }

  /**
   * @see org.eclipse.debug.core.ILaunchListener#launchChanged(org.eclipse.debug.core.ILaunch)
   */
  public void launchChanged(final ILaunch launch) {
    if(!n_trackedLaunches.contains(launch)) {
      return;
    }

    int port = -1;
    String projectName = "";
    String subName = "";
    
    try {
      // test whether the launch defines the JUnit attributes
      port = ConfigurationHelper.getPort(launch);
      projectName = ConfigurationHelper.getProjectName(launch);
      subName = ConfigurationHelper.getSubName(launch);
    }
    catch(Exception ex) {
      log(ex);
      return;
    }

//  log(new Status(IStatus.WARNING, getPluginId(), IStatus.WARNING, 
//  "launch a project with NULL name. Ignoring", null));
    if(null == projectName) {
      return;
    }

    final int   finalPort = isDebug() ? Integer.parseInt(RemoteTestNG.DEBUG_PORT) : port;
    final IJavaProject ijp = JDTUtil.getJavaProject(projectName);
    final String name = subName;
    
    n_trackedLaunches.remove(launch);

    asyncExec(new Runnable() {
        public void run() {
          connectTestRunner(launch, ijp, name, finalPort);
        }
      });
  }

  public void connectTestRunner(ILaunch launch, 
                                IJavaProject launchedProject,
                                String subName,
                                int port) {
    IViewPart viewPart = showTestRunnerViewPartInActivePage(findTestRunnerViewPartInActivePage());
    // The returned view part could be an ErrorViewPart or null if something went wrong in the init
    if (viewPart instanceof TestRunnerViewPart) {
      ((TestRunnerViewPart) viewPart).startTestRunListening(launchedProject, subName, port, launch);
    }
  }

  private IViewPart showTestRunnerViewPartInActivePage(TestRunnerViewPart testRunner) {
    IWorkbenchPart activePart = null;
    IWorkbenchPage page = null;
    try {

      // TODO: have to force the creation of view part contents
      // otherwise the UI will not be updated
      if((testRunner != null) && testRunner.isCreated()) {
        return testRunner;
      }

      page = SWTUtil.getActivePage(getWorkbench());

      if(page == null) {
        return null;
      }

      activePart = page.getActivePart();

      //  show the result view if it isn't shown yet
      return page.showView(TestRunnerViewPart.NAME);
    }
    catch(PartInitException pie) {
      log(pie);

      return null;
    }
    finally {

      //restore focus stolen by the creation of the result view
      if((page != null) && (activePart != null)) {
        page.activate(activePart);
      }
    }
  }

  private TestRunnerViewPart findTestRunnerViewPartInActivePage() {
    IWorkbenchPage page = SWTUtil.getActivePage(getWorkbench());
    if(null == page) {
      return null;
    }

    // If something went wrong, the JDT will return an ErrorViewPart, which cannot
    // be case into TestRunnerViewPart
    IViewPart view = page.findView(TestRunnerViewPart.NAME);
    if (view instanceof TestRunnerViewPart) {
      return (TestRunnerViewPart) view;
    } else {
      return null;
    }
  }

  public static URL makeIconFileURL(String name) throws MalformedURLException {
    if(m_fgIconBaseURL == null) {
      throw new MalformedURLException();
    }

    return new URL(m_fgIconBaseURL, name);
  }

  public static ImageDescriptor getImageDescriptor(String relativePath) {
    try {
      return ImageDescriptor.createFromURL(makeIconFileURL(relativePath));
    }
    catch(MalformedURLException e) {

      // should not happen
      return ImageDescriptor.getMissingImageDescriptor();
    }
  }

  // Assume that all the controls that go through bold() have the same font, 
  // which is probably a safe assumption.  The BOLD font will be disposed
  // in the stop() method
  public static Font BOLD_FONT = null;
  public static Font REGULAR_FONT = null;

  /**
   * Make the passed label bold or not based on the boolean.  This method
   * is put here because it initializes BOLD_FONT, which is later disposed
   * in the stop method of this class
   */
  public static void bold(Control label, boolean bold) {
    if (null == BOLD_FONT) {
      REGULAR_FONT = label.getFont();
      FontData data = REGULAR_FONT.getFontData() [0];
      data.setStyle(data.getStyle() | SWT.BOLD);
      BOLD_FONT = new Font(label.getDisplay(), data);
    }
    
    label.setFont(bold ? BOLD_FONT : REGULAR_FONT);
  }

  /**
   * Returns a String that can be used as an identifying key for 
   * a system property set when tests fail. The value of the property
   * is the comma-separated results, if any, of calling getTestName() on the failed
   * tests. This can be used by @Factory implementations to decide 
   * which instances to construct.
   * @return
   */
  public static String getFailedTestsKey() {
	  return TestNGPluginConstants.S_FAILED_TESTS;
  }

  public static boolean isVerbose() {
    return RemoteTestNG.isVerbose();
  }

  public static boolean isDebug() {
    return RemoteTestNG.isDebug();
  }

  public static IStatus createError(Throwable e) {
    return new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e);
  }
  
  public static IStatus createError(String message) {
    return new Status(IStatus.ERROR, PLUGIN_ID, message);
  }

  public static IStatus createStatus(int severity, String message) {
    return new Status(severity, PLUGIN_ID, message);
  }

  public static List<ITestNGLaunchConfigurationProvider> getLaunchConfigurationProviders()
      throws CoreException {
    List<ITestNGLaunchConfigurationProvider> result = new ArrayList<>();
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registry
        .getExtensionPoint("org.testng.eclipse.launchConfigurationProvider");
    IExtension extensions[] = extensionPoint.getExtensions();
    for (IExtension ext : extensions) {
      IConfigurationElement elements[] = ext.getConfigurationElements();
      for (IConfigurationElement element : elements) {
        ITestNGLaunchConfigurationProvider provider = (ITestNGLaunchConfigurationProvider) element
            .createExecutableExtension("class");
        result.add(provider);
      }
    }

    return result;
  }

}
