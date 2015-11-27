package org.testng.eclipse.maven;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;

public class PreferenceUtils {

  private final static IEclipsePreferences[] preferencesLookup = new IEclipsePreferences[] {
      (InstanceScope.INSTANCE).getNode(Activator.PLUGIN_ID), 
      (DefaultScope.INSTANCE).getNode(Activator.PLUGIN_ID) };

  /**
   * Get specific preference value in instance and default scope
   * 
   * @param prefKey
   *          The preference key
   * @return
   */
  public static String getString(String prefKey) {
    IPreferencesService service = Platform.getPreferencesService();
    String value = service.get(prefKey, null, preferencesLookup);
    return value == null ? null : value.trim();
  }

  public static boolean getBoolean(String prefKey) {
    IPreferencesService service = Platform.getPreferencesService();
    String value = service.get(prefKey, null, preferencesLookup);
    if (value != null) {
      return Boolean.parseBoolean(value);
    }
    return false;
  }
}
