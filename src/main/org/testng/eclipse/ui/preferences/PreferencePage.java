package org.testng.eclipse.ui.preferences;


import org.eclipse.debug.internal.ui.preferences.BooleanFieldEditor2;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;

import java.io.File;

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
//  private StringFieldEditor m_reporters;
  private BooleanFieldEditor2 m_disabledDefaultListeners;
//  private ComboFieldEditor m_parallel;
  private Button m_useXmlTemplateFile;
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
        "Output directory", //$NON-NLS-1$ 
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

    createXmlTemplateFileEditor(parentComposite);

    addField(m_outputdir);
    addField(m_absolutePath);
    addField(m_disabledDefaultListeners);    
//    addField(m_reporters);
//    addField(m_parallel);
    addField(m_xmlTemplateFile);
  }

  /**
   * Create the UI to specify an XML template file.
   */
  private void createXmlTemplateFileEditor(final Composite parent) {
    final Group g = new Group(parent, SWT.SHADOW_ETCHED_OUT);
    m_useXmlTemplateFile = new Button(g, SWT.CHECK);
    GridData gridData= new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan= 3;
    g.setLayoutData(gridData);
    m_useXmlTemplateFile.setText("Use an XML template file");
    m_useXmlTemplateFile.setLayoutData(gridData);
    m_useXmlTemplateFile.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
      }

      public void widgetSelected(SelectionEvent e) {
        m_xmlTemplateFile.setEnabled(((Button) e.getSource()).getSelection(), g);
      }
    });
    m_xmlTemplateFile = new FileFieldEditor(TestNGPluginConstants.S_XML_TEMPLATE_FILE,
        "Template XML file", g) {
      
      // Only verify if the XML file exists if the button is checked
      @Override
      protected boolean checkState() {
        if (m_useXmlTemplateFile.getSelection()) return super.checkState();
        else return true;
      }
    };
    IPreferenceStore storage = TestNGPlugin.getDefault().getPreferenceStore();
    boolean value = storage.getBoolean(TestNGPluginConstants.S_USE_XML_TEMPLATE_FILE);
    m_xmlTemplateFile.setEnabled(value, g);
    m_useXmlTemplateFile.setSelection(value);
  }

  @Override
  public boolean performOk() {
    IPreferenceStore storage = TestNGPlugin.getDefault().getPreferenceStore();
    storage.setValue(TestNGPluginConstants.S_USE_XML_TEMPLATE_FILE,
        m_useXmlTemplateFile.getSelection());
    return super.performOk();
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
