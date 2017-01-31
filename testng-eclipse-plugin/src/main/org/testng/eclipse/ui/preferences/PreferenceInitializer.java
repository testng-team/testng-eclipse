package org.testng.eclipse.ui.preferences;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;

import com.google.common.base.Joiner;

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

    initializeFavoriteStatic();
  }

  private void initializeFavoriteStatic() {
    IPreferenceStore jdtPrefStore = PreferenceConstants.getPreferenceStore();

    Set<String> favorites = new LinkedHashSet<>();
    String existingFavorites = jdtPrefStore.getString(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
    if (!existingFavorites.isEmpty()) {
      favorites.addAll(Arrays.asList(existingFavorites.split(";")));
    }

    favorites.add("org.testng.Assert.*");

    jdtPrefStore.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS,
                          Joiner.on(";").join(favorites));
    try {
      ((ScopedPreferenceStore) jdtPrefStore).save();
    } catch (IOException e) {
    }
  }
}
