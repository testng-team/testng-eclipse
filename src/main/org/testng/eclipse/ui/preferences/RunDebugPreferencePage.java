package org.testng.eclipse.ui.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;

public class RunDebugPreferencePage extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

  private StringFieldEditor m_jvmArgs;

  public RunDebugPreferencePage() {
    setPreferenceStore(TestNGPlugin.getDefault().getPreferenceStore());
    setDescription("TestNG Run/Debug preferences");
  }

  public void init(IWorkbench workbench) {
  }

  @Override
  protected void createFieldEditors() {
    Composite parent = getFieldEditorParent();
    m_jvmArgs = new StringFieldEditor(TestNGPluginConstants.S_JVM_ARGS, 
        "JVM args:",
        StringFieldEditor.UNLIMITED, 
        parent);
    m_jvmArgs.setEmptyStringAllowed(true);
    m_jvmArgs.fillIntoGrid(parent, 2);
    addField(m_jvmArgs);
  }

}
