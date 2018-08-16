package org.testng.eclipse.ui.preferences;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.ui.util.Utils.Widgets;
import org.testng.eclipse.util.PreferenceStoreUtil;
import org.testng.eclipse.util.ResourceUtil;

/**
 * Project specific properties.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class ProjectPropertyPage extends PropertyPage {
  private Text m_outputdir;
  private Button m_disabledDefaultListeners;
  private Text m_xmlTemplateFile;
  private Text m_preDefinedListeners;
  private IProject m_workingProject;
  private Text m_watchResultText;
  private Button m_watchResultRadio;

  public void createControl(Composite parent) {
    setDescription("Project TestNG settings");
    super.createControl(parent);
  }

  /**
   * @see PreferencePage#createContents(Composite)
   */
  protected Control createContents(Composite parent) {
    Composite parentComposite = new Composite(parent, SWT.NONE);
    GridLayoutFactory.fillDefaults().applyTo(parentComposite);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(parentComposite);

    //
    // Output directory
    //
    {
      Widgets w = Utils.createStringEditorControl(parentComposite, "TestNGPropertyPage.outputDir", null, true);
      m_outputdir = w.text;
      m_outputdir.setToolTipText(ResourceUtil.getString("TestNGPropertyPage.outputDir.tips"));
    }

    //
    // XML template file
    //

    {
      SelectionAdapter buttonListener = new SelectionAdapter() {
        public void widgetSelected(SelectionEvent evt) {
          String result = Utils.selectTemplateFile(getShell());
          if (result != null) {
            m_xmlTemplateFile.setText(result);
          }
        }
      };
      Widgets w = Utils.createTextBrowseControl(parentComposite, null,
          "TestNGPropertyPage.templateXml", buttonListener, null, null, true);
      m_xmlTemplateFile = w.text;

    }

    //
    // Disable default listeners
    //
    m_disabledDefaultListeners = new Button(parentComposite, SWT.CHECK);
    m_disabledDefaultListeners.setText(ResourceUtil.getString("TestNGPropertyPage.disableDefaultListeners"));
//    m_disabledDefaultListeners.setLayoutData(SWTUtil.createGridData());//new GridData(SWT.FILL, SWT.NONE, true, false, 4, 1));
//    m_disabledDefaultListeners.setBackground(new Color(parent.getDisplay(), 0xcc, 0, 0));


    //Create a string editor control: A label and a text area
    {
      Widgets w = Utils.createStringEditorControl(parentComposite, "TestNGPropertyPage.preDefinedListeners", null, true);
      m_preDefinedListeners = w.text;
      m_preDefinedListeners.setToolTipText(ResourceUtil.getString("TestNGPropertyPage.disableDefaultListeners.tips"));
    }

    Label sepLabel = new Label(parentComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridDataFactory.fillDefaults().applyTo(sepLabel);

    //
    // Watch testng-results.xml
    //
    {
//      Group g = new Group(parentComposite, SWT.SHADOW_ETCHED_IN);
//      m_watchResults = new Button(g, SWT.CHECK);
//      m_watchResults.setText("Watch testng-results.xml");

      SelectionAdapter listener = new SelectionAdapter() {
        public void widgetSelected(SelectionEvent evt) {
          DirectoryDialog dlg= new DirectoryDialog(m_xmlTemplateFile.getShell());
          dlg.setMessage("Select TestNG Output Directory");
          String selectedDir = dlg.open();
          if (new File(selectedDir).isDirectory()) {
            // use xml reporter file name as String literal rather than constant
            // since XMLReporter.FILE_NAME was moved in https://github.com/cbeust/testng/pull/1785
            selectedDir = selectedDir + File.separator + "testng-results.xml";
          }
          m_watchResultText.setText(selectedDir != null ? selectedDir : "");
        }
      };

      Widgets w = Utils.createTextBrowseControl(parentComposite,
          "TestNGPropertyPage.watchResultXml",
          "TestNGPropertyPage.resultXmlDirectory",
          listener, null, null,
          true);
      m_watchResultText = w.text;
      m_watchResultRadio = w.radio;
    }

    loadDefaults();

    return parentComposite;
  }

  public void dispose() {
    m_outputdir.dispose();
    m_disabledDefaultListeners.dispose();
    m_xmlTemplateFile.dispose();
    m_preDefinedListeners.dispose();
    
    super.dispose();
  }

  private void loadDefaults() {
    m_workingProject = (IProject) getElement().getAdapter(IProject.class);

    // Populate the owner text field with the default value
    PreferenceStoreUtil storage= TestNGPlugin.getPluginPreferenceStore();
    String projectName = m_workingProject.getName();
    m_outputdir.setText(storage.getOutputDir(projectName, false));
    m_disabledDefaultListeners.setSelection(storage.hasDisabledListeners(projectName, true));
    m_xmlTemplateFile.setText(storage.getXmlTemplateFile(projectName, true));
    m_watchResultRadio.setSelection(storage.getWatchResults(projectName));
    String dir = storage.getWatchResultDirectory(projectName);
    m_watchResultText.setText(dir);
    m_preDefinedListeners.setText(storage.getPreDefinedListeners(projectName, false));
  }

  protected void performDefaults() {
    loadDefaults();
    super.performDefaults();
  }

  public boolean performOk() {
    PreferenceStoreUtil storage= TestNGPlugin.getPluginPreferenceStore();
    String projectName = m_workingProject.getName();
    storage.storeOutputDir(projectName, m_outputdir.getText());
    storage.storeDisabledListeners(projectName, m_disabledDefaultListeners.getSelection());
    storage.storeXmlTemplateFile(projectName, m_xmlTemplateFile.getText());
    storage.storePreDefinedListeners(projectName, m_preDefinedListeners.getText());
    storage.storeWatchResults(projectName, m_watchResultRadio.getSelection());
    storage.storeWatchResultLocation(projectName, m_watchResultText.getText());

    if(super.performOk()) {
      setMessage("Project preferences are saved", INFORMATION);
      return true;
    }
    
    return false;
  }

  public static class ProjectContentProvider implements ITreeContentProvider {

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof IContainer) {
        try {
          return ((IContainer) parentElement).members();
        }
        catch (CoreException ce) {
          ;
        }
      }

      return null;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent(Object element) {
      return ((IResource) element).getParent();
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
      IResource resource = (IResource) element;

      if (IResource.FILE == resource.getType()) {
        return false;
      }
      else {
        return true;
      }
    }

    /**
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement) {
      return getChildren(inputElement);
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
    
  }
  
}