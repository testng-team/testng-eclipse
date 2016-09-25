package org.testng.eclipse.maven;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class MavenTestNGPreferenceInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {
    Map<String, String> defaultMap = new HashMap<>();
    defaultMap.put(Activator.PREF_ARGLINE, Boolean.TRUE.toString());
    defaultMap.put(Activator.PREF_SYSPROPERTIES, Boolean.TRUE.toString());
    defaultMap.put(Activator.PREF_ENVIRON, Boolean.TRUE.toString());

    // Store default values to default core preferences
    IEclipsePreferences defaultPreferences = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
    for (Entry<String, String> entry : defaultMap.entrySet()) {
      String key = entry.getKey();
      String val = entry.getValue();
      try {
        defaultPreferences.put(key, val);
      } catch (Exception e) {
        Activator.log("unknown error occurs when saving preferences", e); //$NON-NLS-1$
      }
    }
  }

}
