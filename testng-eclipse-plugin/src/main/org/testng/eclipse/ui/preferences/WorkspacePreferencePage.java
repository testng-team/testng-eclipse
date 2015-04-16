package org.testng.eclipse.ui.preferences;


import java.io.File;

import org.eclipse.debug.internal.ui.preferences.BooleanFieldEditor2;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;
import org.testng.eclipse.util.ResourceUtil;

/**
 * Workspace wide preferences for TestNG.
 */
public class WorkspacePreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage
{
  private FSBrowseDirectoryFieldEditor m_outputdir;
  private BooleanFieldEditor2 m_absolutePath;
  private BooleanFieldEditor2 m_disabledDefaultListeners;
  private BooleanFieldEditor2 m_showViewWhenTestsComplete;
  private BooleanFieldEditor2 m_useProjectJar;
  private FileFieldEditor m_xmlTemplateFile;
  private StringFieldEditor m_excludedStackTraces;
  private StringFieldEditor m_preDefinedListeners;
  
  public WorkspacePreferencePage() {
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

    m_showViewWhenTestsComplete = new BooleanFieldEditor2(
        TestNGPluginConstants.S_SHOW_VIEW_WHEN_TESTS_COMPLETE,
        "Show view when tests complete", //$NON-NLS-1$ 
        SWT.NONE, parentComposite);

    m_useProjectJar = new BooleanFieldEditor2(
        TestNGPluginConstants.S_USEPROJECTJAR_GLOBAL, 
        ResourceUtil.getString("TestNGPropertyPage.useProjectTestNGJar"), SWT.NONE, parentComposite);
    
    // XML template
    m_xmlTemplateFile = new FileFieldEditor(TestNGPluginConstants.S_XML_TEMPLATE_FILE,
        "Template XML file:", false /* no absolute */,
        StringButtonFieldEditor.VALIDATE_ON_FOCUS_LOST,
        parentComposite);
    m_xmlTemplateFile.setEmptyStringAllowed(true);
    m_xmlTemplateFile.fillIntoGrid(parentComposite, 3);

    // Excluded stack traces
    m_excludedStackTraces = new StringFieldEditor(TestNGPluginConstants.S_EXCLUDED_STACK_TRACES,
        "Excluded stack traces:", parentComposite);
    m_excludedStackTraces.fillIntoGrid(parentComposite, 3);
    GridDataFactory.fillDefaults().grab(true, false)
        .hint(convertWidthInCharsToPixels(36), SWT.DEFAULT)
        .applyTo(m_excludedStackTraces.getTextControl(parentComposite));

    m_preDefinedListeners = new StringFieldEditor(TestNGPluginConstants.S_PRE_DEFINED_LISTENERS,
        "Pre Defined Listeners", parentComposite);
    
    
    addField(m_outputdir);
    addField(m_absolutePath);
    addField(m_disabledDefaultListeners);
    addField(m_showViewWhenTestsComplete);
    addField(m_useProjectJar);
    addField(m_xmlTemplateFile);
    addField(m_excludedStackTraces);
    addField(m_preDefinedListeners);
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
