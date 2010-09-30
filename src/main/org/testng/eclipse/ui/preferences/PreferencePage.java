package org.testng.eclipse.ui.preferences;


import org.eclipse.debug.internal.ui.preferences.BooleanFieldEditor2;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;

import java.io.File;

/**
 * Workspace wide preferences for TestNG.
 */
public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
  private FSBrowseDirectoryFieldEditor m_outputdir;
  private BooleanFieldEditor2 m_absolutePath;
  private BooleanFieldEditor2 m_disabledDefaultListeners;
  private FileFieldEditor m_xmlTemplateFile;
  
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
  @Override
  public void createFieldEditors() {
    Composite parentComposite= getFieldEditorParent();
    m_outputdir= new FSBrowseDirectoryFieldEditor(TestNGPluginConstants.S_OUTDIR, 
        "Output directory:", //$NON-NLS-1$ 
        parentComposite);
    m_outputdir.fillIntoGrid(parentComposite, 3);
    Button btn= m_outputdir.getChangeControl(parentComposite);
    btn.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent evt) {
        m_absolutePath.getChangeControl(getFieldEditorParent()).setSelection(true);
      }
    });
    
    m_absolutePath= new BooleanFieldEditor2(TestNGPluginConstants.S_ABSOLUTEPATH, 
        "Absolute output path", //$NON-NLS-1$ 
        SWT.NONE, 
        parentComposite); 
    m_outputdir.setAbsolutePathVerifier(m_absolutePath);

    m_disabledDefaultListeners= new BooleanFieldEditor2(TestNGPluginConstants.S_DISABLEDLISTENERS, 
        "Disable default listeners", //$NON-NLS-1$ 
        SWT.NONE, 
        parentComposite);

    // XML template
    m_xmlTemplateFile = new FileFieldEditor(TestNGPluginConstants.S_XML_TEMPLATE_FILE,
        "Template XML file:", false /* no absolute */,
        StringButtonFieldEditor.VALIDATE_ON_FOCUS_LOST,
        parentComposite);
    m_xmlTemplateFile.setEmptyStringAllowed(true);
    m_xmlTemplateFile.fillIntoGrid(parentComposite, 3);

    addField(m_outputdir);
    addField(m_absolutePath);
    addField(m_disabledDefaultListeners);    
    addField(m_xmlTemplateFile);
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {
  }

  private static class FSBrowseDirectoryFieldEditor extends DirectoryFieldEditor {
    BooleanFieldEditor2 absolutePath;
    
    public FSBrowseDirectoryFieldEditor(String name, String labelText, Composite parent) {
      super(name, labelText, parent);
    }

    protected void setAbsolutePathVerifier(BooleanFieldEditor2 isAbsolute) {
      absolutePath= isAbsolute;
    }

    @Override
    public Button getChangeControl(Composite parent) {
      return super.getChangeControl(parent);
    }

    @Override
    protected boolean doCheckState() {
      String fileName = getTextControl().getText();
      fileName = fileName.trim();
      if (fileName.length() == 0 && isEmptyStringAllowed()) {
        return true;
      }
      if(absolutePath.getBooleanValue()) {
        File file = new File(fileName);
        return file.isDirectory();
      }
      
      return true;
    }
  }
}
