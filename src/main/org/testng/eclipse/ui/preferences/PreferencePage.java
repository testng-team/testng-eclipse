package org.testng.eclipse.ui.preferences;


import org.eclipse.debug.internal.ui.preferences.BooleanFieldEditor2;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.SWTUtil;

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
  private FSBrowseDirectoryFieldEditor m_outputdir;
  private BooleanFieldEditor2 m_absolutePath;
  
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
    Composite parentComposite= getFieldEditorParent();
    m_outputdir= new FSBrowseDirectoryFieldEditor(PreferenceConstants.P_OUTPUT, "Output directory", parentComposite);
    m_outputdir.fillIntoGrid(parentComposite, 3);
    Button btn= m_outputdir.getChangeControl(parentComposite);
    btn.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent evt) {
        m_absolutePath.getChangeControl(getFieldEditorParent()).setSelection(true);
      }
    });
    m_absolutePath= new BooleanFieldEditor2(PreferenceConstants.P_ABSOLUTEPATH, "Absolute output path", SWT.NONE, parentComposite); //$NON-NLS-1$
    addField(m_outputdir);
    addField(m_absolutePath);
  }

  
  public boolean performOk() {
    IPreferenceStore storage= TestNGPlugin.getDefault().getPreferenceStore();
    storage.setValue(PreferenceConstants.P_OUTPUT, m_outputdir.getStringValue());
    storage.setValue(PreferenceConstants.P_ABSOLUTEPATH, m_absolutePath.getBooleanValue());
    setMessage("Preferences saved", INFORMATION);
    
    return super.performOk();
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {
  }

  private static class FSBrowseDirectoryFieldEditor extends DirectoryFieldEditor {
    public FSBrowseDirectoryFieldEditor(String name, String labelText, Composite parent) {
      super(name, labelText, parent);
    }

    public Button getChangeControl(Composite parent) {
      return super.getChangeControl(parent);
    }
  }
}
