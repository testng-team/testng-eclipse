package org.testng.eclipse.maven;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.testng.eclipse.maven";

    public static final String PREF_USE_PROJECT_SETTINGS = "userprojectsettings";
    public static final String PREF_ARGLINE = PLUGIN_ID + ".argline";
    public static final String PREF_ENVIRON = PLUGIN_ID + ".environ";
    public static final String PREF_SYSPROPERTIES = PLUGIN_ID + ".sysproperties";
    /**
     * surefire/failsafe use configuration/properties as TestNG runtime arguments
     * 
     * @see http://maven.apache.org/surefire/maven-surefire-plugin/examples/testng.html
     */
    public static final String PREF_PROPERTIES = PLUGIN_ID + ".properties";
    public static final String PREF_ADDITION_CLASSPATH = PLUGIN_ID + ".additionalClasspath";

    // The shared instance
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    public static String getId() {
        return PLUGIN_ID;
    }

    public static IStatus createError(String message) {
      return new Status(IStatus.ERROR, getId(), message);
    }

    public static IStatus createError(String msg, Throwable e) {
      return new Status(IStatus.ERROR, getId(), IStatus.ERROR, msg, e);
    }

    public static void log(String msg, Throwable e) {
      log(createError(msg, e));
    }

    public static void log(IStatus status) {
      getDefault().getLog().log(status);
    }
}
