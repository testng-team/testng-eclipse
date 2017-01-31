package org.testng.eclipse.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
   * initializeDefaultPreferences()
   */
  @Override
  public void initializeDefaultPreferences() {
    IPreferenceStore store = TestNGPlugin.getDefault().getPreferenceStore();
    store.setDefault(TestNGPluginConstants.S_OUTDIR, "/test-output");
    store.setDefault(TestNGPluginConstants.S_EXCLUDED_STACK_TRACES,
        "org.testng.internal org.testng.TestRunner org.testng.SuiteRunner "
        + "org.testng.remote.RemoteTestNG org.testng.TestNG sun.reflect java.lang");
    // Set the default to the original behavior, where the view takes focus when
    // tests finish running
    store.setDefault(TestNGPluginConstants.S_SHOW_VIEW_WHEN_TESTS_COMPLETE,
        true);
    store.setDefault(TestNGPluginConstants.S_VIEW_TITLE_SHOW_CASE_NAME,
        true);
    store.setDefault(TestNGPluginConstants.S_APPEND_FAVORITE_STATIC_IMPORT,
        true);
  }

}
