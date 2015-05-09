package org.testng.eclipse.ui.preferences;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.internal.ui.actions.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.FolderSelectionDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.ui.util.Utils.Widgets;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.PreferenceStoreUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.reporters.XMLReporter;

/**
 * Project specific properties.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class ProjectPropertyPage extends PropertyPage {
  private Text m_outputdir;
  private Button m_absolutePath;
  private Button m_disabledDefaultListeners;
  private Text m_xmlTemplateFile;
  private Text m_preDefinedListeners;
  private IProject m_workingProject;
  private Button m_projectJar;
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
      Group g = new Group(parentComposite, SWT.SHADOW_ETCHED_IN);
      g.setLayout(new GridLayout());
      SelectionAdapter buttonListener = new SelectionAdapter() {
        public void widgetSelected(SelectionEvent evt) {
          DirectoryDialog dlg= new DirectoryDialog(m_outputdir.getShell());
          dlg.setMessage("Select TestNG output directory");
          String selectedDir= dlg.open();
          m_outputdir.setText(selectedDir != null ? selectedDir : "");
          m_absolutePath.setSelection(true);
        }
      };
      Widgets w = Utils.createTextBrowseControl(g, null,
          "TestNGPropertyPage.outputDir", buttonListener, null, null, true);
      m_outputdir = w.text;

      m_absolutePath = new Button(g, SWT.CHECK);
      m_absolutePath.setText("Absolute output path");
      m_absolutePath.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
      g.setLayoutData(SWTUtil.createGridData());
    }

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
            selectedDir = selectedDir + File.separator + XMLReporter.FILE_NAME;
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

    //
    // XML template file
    //

    {
      SelectionAdapter buttonListener = new SelectionAdapter() {
        public void widgetSelected(SelectionEvent evt) {
          FileDialog dlg= new FileDialog(m_xmlTemplateFile.getShell());
          dlg.setText("Select Template XML file");
          String[] filterExt = {"*.xml"};
          dlg.setFilterExtensions(filterExt);
          String selectedFile= dlg.open();
          m_xmlTemplateFile.setText(selectedFile != null ? selectedFile : "");
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
    m_disabledDefaultListeners.setText("Disable default listeners");
//    m_disabledDefaultListeners.setLayoutData(SWTUtil.createGridData());//new GridData(SWT.FILL, SWT.NONE, true, false, 4, 1));
//    m_disabledDefaultListeners.setBackground(new Color(parent.getDisplay(), 0xcc, 0, 0));

    
    //
    // Project jar
    //
    m_projectJar= new Button(parentComposite, SWT.CHECK);
    m_projectJar.setText(ResourceUtil.getString("TestNGPropertyPage.useProjectTestNGJar"));
//    m_projectJar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, SWT.NONE,
//        false /* don't grab excess horizontal */,
//        false /* don't grab excess vertical */,
//        2, 1));
    
    //Create a string editor control: A label and a text area
    {
      Widgets w = Utils.createStringEditorControl(parentComposite, "TestNGPropertyPage.preDefinedListeners", null, true);
      m_preDefinedListeners = w.text;
      m_preDefinedListeners.setToolTipText("Split multi listener using ;");
    }
    loadDefaults();

    return parentComposite;
  }

  
  public void dispose() {
    m_projectJar.dispose();
    m_outputdir.dispose();
    m_absolutePath.dispose();
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
    m_outputdir.setText(storage.getOutputDir(projectName, true));
    m_absolutePath.setSelection(storage.isOutputAbsolutePath(projectName, true));
    m_disabledDefaultListeners.setSelection(storage.hasDisabledListeners(projectName, true));
    m_xmlTemplateFile.setText(storage.getXmlTemplateFile(projectName, true));
    m_projectJar.setSelection(storage.getUseProjectJar(projectName));
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
    storage.storeOutputDir(projectName, m_outputdir.getText(), m_absolutePath.getSelection());
    storage.storeDisabledListeners(projectName, m_disabledDefaultListeners.getSelection());
    storage.storeXmlTemplateFile(projectName, m_xmlTemplateFile.getText());
    storage.storePreDefinedListeners(projectName, m_preDefinedListeners.getText());
    storage.storeUseProjectJar(projectName, m_projectJar.getSelection());
    storage.storeWatchResults(projectName, m_watchResultRadio.getSelection());
    storage.storeWatchResultLocation(projectName, m_watchResultText.getText());

    if(super.performOk()) {
      setMessage("Project preferences are saved", INFORMATION);
      return true;
    }
    
    return false;
  }

  private void handleBrowseAction() {
    Class[] acceptedClasses = new Class[] { IProject.class, IFolder.class };
    ISelectionStatusValidator validator = new ISelectionStatusValidator() {
        public IStatus validate(Object[] selection) {
          if ((null == selection) || (selection.length == 0)) {
            return new StatusInfo(IStatus.ERROR, "empty selection is not allowed");
          }

          if (selection.length > 1) {
            return new StatusInfo(IStatus.ERROR, "multiple selection is not allowed");
          }

          if (IFolder.class.isInstance(selection[0]) || IProject.class.isInstance(selection[0])) {
            return new StatusInfo();
          }

          return new StatusInfo(IStatus.ERROR, "not accepted type");
        }
      };

    IWorkspaceRoot workspaceRoot = JDTUtil.getWorkspaceRoot();
    IProject[] allProjects = workspaceRoot.getProjects();
    ArrayList rejectedElements = new ArrayList(allProjects.length);
    IProject currProject = m_workingProject.getProject();
    for (int i = 0; i < allProjects.length; i++) {
      if (!allProjects[i].equals(currProject)) {
        rejectedElements.add(allProjects[i]);
      }
    }
    ViewerFilter filter = new TypedViewerFilter(acceptedClasses, rejectedElements.toArray());

    ILabelProvider lp = new WorkbenchLabelProvider();
    ITreeContentProvider cp = new ProjectContentProvider();

    IResource initSelection = null;
//    if (!"".equals(m_outdirPath.getText())) {
//      initSelection= workspaceRoot.findMember(new Path(m_outdirPath.getText()));
//    }

    FolderSelectionDialog dialog = new FolderSelectionDialog(getShell(), lp, cp);
    dialog.setTitle("Select TestNG output artifacts directory");
    dialog.setValidator(validator);
    dialog.setMessage("a message");
    dialog.addFilter(filter);
    dialog.setInput(workspaceRoot);
    dialog.setInitialSelection(initSelection);
    dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));

    if (dialog.open() == Window.OK) {
      m_outputdir.setText(((IContainer) dialog.getFirstResult()).getProjectRelativePath()
                           .toPortableString());
      m_absolutePath.setSelection(false);
    }

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