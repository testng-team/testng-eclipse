package org.testng.eclipse.ui.preferences;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;

import com.google.common.base.Joiner;

public class JdtPreferenceInjector implements IStartup, IPropertyChangeListener {

  private static final String assertClassName = "org.testng.Assert";

  @Override
  public void earlyStartup() {
    initializeFavoriteStatic();
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS.equals(event.getProperty())) {
      // check if user manully removed testng assert
      if (event.getOldValue().toString().contains(assertClassName) 
          && !event.getNewValue().toString().contains(assertClassName)) {
        IPreferenceStore store = TestNGPlugin.getDefault().getPreferenceStore();
        store.setValue(TestNGPluginConstants.S_APPEND_FAVORITE_STATIC_IMPORT, false);
        try {
          ((ScopedPreferenceStore) store).save();
        } catch (IOException e) {
        }
      }
    }
  }

  private void initializeFavoriteStatic() {
    IPreferenceStore store = TestNGPlugin.getDefault().getPreferenceStore();
    boolean appendFavorite = store.getBoolean(TestNGPluginConstants.S_APPEND_FAVORITE_STATIC_IMPORT);
    if (!appendFavorite) {
      return;
    }

    IPreferenceStore jdtPrefStore = PreferenceConstants.getPreferenceStore();
    jdtPrefStore.addPropertyChangeListener(this);

    Set<String> favorites = new LinkedHashSet<>();
    String existingFavorites = jdtPrefStore.getString(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
    if (!existingFavorites.isEmpty()) {
      favorites.addAll(Arrays.asList(existingFavorites.split(";")));
    }

    favorites.add(assertClassName + ".*");

    jdtPrefStore.setValue(PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS,
                          Joiner.on(";").join(favorites));
    try {
      ((ScopedPreferenceStore) jdtPrefStore).save();
    } catch (IOException e) {
    }
  }
}
