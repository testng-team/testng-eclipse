package org.testng.eclipse.maven;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

public class PreferenceUtils {

  private final static IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[] {
      (InstanceScope.INSTANCE).getNode(Activator.PLUGIN_ID), (DefaultScope.INSTANCE).getNode(Activator.PLUGIN_ID) };

  /**
   * Get specific preference value in instance and default scope
   * 
   * @param prefKey
   *          The preference key
   * @return
   */
  public static String getString(IProject project, String prefKey) {
    IPreferencesService service = Platform.getPreferencesService();
    String value = service.get(prefKey, null, getPreferenceLookup(project));
    return value == null ? null : value.trim();
  }

  public static boolean getBoolean(IProject project, String prefKey) {
    IPreferencesService service = Platform.getPreferencesService();
    String value = service.get(prefKey, null, getPreferenceLookup(project));
    if (value != null) {
      return Boolean.parseBoolean(value);
    }
    return false;
  }

  private static IEclipsePreferences[] getPreferenceLookup(IProject project) {
    IEclipsePreferences prjPref = null;
    if (project != null) {
      prjPref = getEclipsePreferences(project);
    }

    if (prjPref == null) {
      return preferencesLookup;
    }
    if (!prjPref.getBoolean(Activator.PREF_USE_PROJECT_SETTINGS, false)) {
      return preferencesLookup;
    }

    IEclipsePreferences[] prefs = new IEclipsePreferences[preferencesLookup.length + 1];
    prefs[0] = prjPref;
    System.arraycopy(preferencesLookup, 0, prefs, 1, preferencesLookup.length);
    return prefs;
  }

  private static IEclipsePreferences getEclipsePreferences(IProject project) {
    IScopeContext context = new ProjectScope(project);
    IEclipsePreferences eclipsePreferences = context.getNode(Activator.PLUGIN_ID);
    return eclipsePreferences;
  }
}
