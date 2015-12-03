package org.testng.eclipse.maven;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.osgi.service.prefs.BackingStoreException;

public abstract class OptionsConfigurationBlock {

  public static final class Key {

    private String fQualifier;
    private String fKey;

    public Key(String qualifier, String key) {
      fQualifier = qualifier;
      fKey = key;
    }

    public String getName() {
      return fKey;
    }

    private IEclipsePreferences getNode(IScopeContext context) {
      return context.getNode(fQualifier);
    }

    public String getStoredValue(IScopeContext context) {
      return getNode(context).get(fKey, null);
    }

    public String getStoredValue(IScopeContext[] lookupOrder, boolean ignoreTopScope) {
      for (int i = ignoreTopScope ? 1 : 0; i < lookupOrder.length; i++) {
        String value = getStoredValue(lookupOrder[i]);
        if (value != null) {
          return value;
        }
      }
      return null;
    }

    public void setStoredValue(IScopeContext context, String value) {
      IEclipsePreferences preference = getNode(context);
      if (value != null) {
        preference.put(fKey, value);
      } else {
        preference.remove(fKey);
      }
      // Dump changes
      try {
        preference.flush();
      } catch (BackingStoreException e) {
        // problem with pref store - quietly ignore
      }
    }

    public String toString() {
      return fQualifier + '/' + fKey;
    }

    public String getQualifier() {
      return fQualifier;
    }

  }

  private IScopeContext[] fLookupOrder;

  protected final IProject fProject;

  protected OptionsConfigurationBlock(IProject project) {
    fProject = project;

    if (fProject != null) {
      fLookupOrder = new IScopeContext[] { new ProjectScope(fProject), InstanceScope.INSTANCE, DefaultScope.INSTANCE };
    } else {
      fLookupOrder = new IScopeContext[] { InstanceScope.INSTANCE, DefaultScope.INSTANCE };
    }
  }

  protected String getValue(Key key) {
    return key.getStoredValue(fLookupOrder, false);
  }

  protected boolean getBooleanValue(Key key) {
    return Boolean.valueOf(getValue(key)).booleanValue();
  }

  protected String setValue(Key key, String value) {
    String oldValue = getValue(key);
    key.setStoredValue(fLookupOrder[0], value);
    return oldValue;
  }

  protected String setValue(Key key, boolean value) {
    return setValue(key, String.valueOf(value));
  }

  public static Key getKey(String key) {
    return new Key(Activator.PLUGIN_ID, key);
  }

  public boolean hasProjectSpecificOptions(IProject project) {
    if (project != null) {
      String val = getValue(getKey(Activator.PREF_USE_PROJECT_SETTINGS));
      if (val != null) {
        return Boolean.parseBoolean(val);
      }
      return false;
    }
    return false;
  }

  public void useProjectSpecificSettings(boolean useProjectSpecificSettings) {
    if (fProject != null) {
      setValue(getKey(Activator.PREF_USE_PROJECT_SETTINGS), useProjectSpecificSettings);
    }
  }

  protected abstract Control createContents(Composite parent);

  public boolean performOk() {
    return true;
  }

  public boolean performDefaults() {
    return true;
  }

  public boolean performApply() {
    return performOk();
  }
}
