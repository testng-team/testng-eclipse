package org.testng.eclipse.maven;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

public class MavenTestNGOptionsConfigurationBlock extends OptionsConfigurationBlock {

  private static final Key KEY_PROPERTIES = getKey(Activator.PREF_PROPERTIES);
  private static final Key KEY_ARGLINE = getKey(Activator.PREF_ARGLINE);
  private static final Key KEY_ENVIRON = getKey(Activator.PREF_ENVIRON);
  private static final Key KEY_SYSPROPERTIES = getKey(Activator.PREF_SYSPROPERTIES);
  private static final Key KEY_ADDITION_CLASSPATH = getKey(Activator.PREF_ADDITION_CLASSPATH);

  private Button argLineBtn;
  private Button environBtn;
  private Button propertiesBtn;
  private Button syspropsBtn;
  private Button additionalCpBtn;

  protected MavenTestNGOptionsConfigurationBlock(IProject project) {
    super(project);
  }

  @Override
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().applyTo(composite);

    Group mavenGroup = new Group(composite, SWT.NONE);
    GridDataFactory.fillDefaults().grab(true, false).applyTo(mavenGroup);
    GridLayoutFactory.fillDefaults().applyTo(mavenGroup);
    mavenGroup.setText(Messages.prefPrefixFromPomGroupName);

    argLineBtn = new Button(mavenGroup, SWT.CHECK);
    argLineBtn.setText(Messages.prefArgLineBtnName);

    syspropsBtn = new Button(mavenGroup, SWT.CHECK);
    syspropsBtn.setText(Messages.prefSysPropsBtnName);

    environBtn = new Button(mavenGroup, SWT.CHECK);
    environBtn.setText(Messages.prefEnvironBtnName);

    additionalCpBtn = new Button(mavenGroup, SWT.CHECK);
    additionalCpBtn.setText(Messages.prefAdditionalClasspathBtnName);

    propertiesBtn = new Button(mavenGroup, SWT.CHECK);
    propertiesBtn.setText(Messages.prefPropertiesBtnName);

    initWidgetValues();

    return composite;
  }

  private void initWidgetValues() {
    argLineBtn.setSelection(getBooleanValue(KEY_ARGLINE));
    environBtn.setSelection(getBooleanValue(KEY_ENVIRON));
    syspropsBtn.setSelection(getBooleanValue(KEY_SYSPROPERTIES));
    additionalCpBtn.setSelection(getBooleanValue(KEY_ADDITION_CLASSPATH));
    propertiesBtn.setSelection(getBooleanValue(KEY_PROPERTIES));
  }

  public boolean performApply(boolean useDefaults) {
    boolean fArgLine = argLineBtn.getSelection();
    boolean fEnviron = environBtn.getSelection();
    boolean fProperties = propertiesBtn.getSelection();
    boolean fSysProps = syspropsBtn.getSelection();
    boolean fAdditionalCp = additionalCpBtn.getSelection();
    if (useDefaults) {
      IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
      fArgLine = prefStore.getDefaultBoolean(Activator.PREF_ARGLINE);
      fEnviron = prefStore.getDefaultBoolean(Activator.PREF_ENVIRON);
      fProperties = prefStore.getDefaultBoolean(Activator.PREF_PROPERTIES);
      fSysProps = prefStore.getDefaultBoolean(Activator.PREF_SYSPROPERTIES);
      fAdditionalCp = prefStore.getDefaultBoolean(Activator.PREF_ADDITION_CLASSPATH);
    }

    setValue(KEY_ARGLINE, fArgLine);
    setValue(KEY_ENVIRON, fEnviron);
    setValue(KEY_PROPERTIES, fProperties);
    setValue(KEY_SYSPROPERTIES, fSysProps);
    setValue(KEY_ADDITION_CLASSPATH, fAdditionalCp);

    return true;
  }

  @Override
  public boolean performDefaults() {
    boolean ret = performApply(true);
    initWidgetValues();
    return ret;
  }

  @Override
  public boolean performOk() {
    return performApply(false);
  }

  private static Key[] getKeys() {
    return new Key[] { KEY_ARGLINE, KEY_ENVIRON, KEY_SYSPROPERTIES, KEY_ADDITION_CLASSPATH, KEY_PROPERTIES };
  }
}
