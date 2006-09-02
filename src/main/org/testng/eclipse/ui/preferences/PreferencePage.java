package org.testng.eclipse.ui.preferences;


import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import org.testng.eclipse.TestNGPlugin;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
  private StringFieldEditor m_outputDirEditor;
  
  public PreferencePage() {
    super(GRID);
    setPreferenceStore(TestNGPlugin.getDefault().getPreferenceStore());
    setDescription("TestNG workbench preferences"); //$NON-NLS-1$
  }

  /**
   * Creates the field editors. Field editors are abstractions of
   * the common GUI blocks needed to manipulate various types
   * of preferences. Each field editor knows how to save and
   * restore itself.
   */
  public void createFieldEditors() {
    m_outputDirEditor= new StringFieldEditor(PreferenceConstants.P_OUTPUT, "Output directory:", getFieldEditorParent()); //$NON-NLS-1$
    addField(m_outputDirEditor);
  }

  
  public boolean performOk() {
    IPreferenceStore storage= TestNGPlugin.getDefault().getPreferenceStore();
    String output= m_outputDirEditor.getStringValue();
    if(null != output && !"".equals(output) && output.startsWith("/")) {
      output= output.substring(1);
      m_outputDirEditor.setStringValue(output);
    }
    
    storage.setValue(PreferenceConstants.P_OUTPUT, output);
    setMessage("Preferences saved", INFORMATION);
    
    return super.performOk();
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {
  }

}
