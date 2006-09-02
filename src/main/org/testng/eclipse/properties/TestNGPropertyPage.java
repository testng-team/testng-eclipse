package org.testng.eclipse.properties;


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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;

import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.SWTUtil;

public class TestNGPropertyPage extends PropertyPage {
  private static final IStatus ERROR = new StatusInfo(IStatus.ERROR, "");
  private static final String PATH_TITLE = "Path:";
  private Text m_outdirPath;
  private Text m_reportersText;
  private IProject m_workingProject;

  public void createControl(Composite parent) {
    setDescription("General TestNG settings:");
    super.createControl(parent);
  }

  /**
   * @see PreferencePage#createContents(Composite)
   */
  protected Control createContents(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout(3, false));

    composite.setLayoutData(new GridData(GridData.FILL | GridData.GRAB_HORIZONTAL));

    //Label for path field
    Label pathLabel = new Label(composite, SWT.NONE);
    pathLabel.setText(PATH_TITLE);

    // Owner text field
    m_outdirPath = new Text(composite, SWT.SINGLE | SWT.BORDER);
    m_outdirPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Button browse = new Button(composite, SWT.PUSH);
    browse.setText("Browse");
    SWTUtil.setButtonGridData(browse);
    browse.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          handleBrowseAction();
        }
      });

    Label pathDetailsLabel = new Label(composite, SWT.WRAP);
    pathDetailsLabel.setText("Set the directory for the tests output. "
        + "This location is used by TestNG to generate "
        + "the output artifacts (including the file testng-failures.xml).");
    GridData gd= new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan= 3;
    pathDetailsLabel.setLayoutData(gd);
    
    //Label for path field
    Label listenersLabel = new Label(composite, SWT.NONE);
    listenersLabel.setText("Listeners/Reporters:");

    // Owner text field
    m_reportersText = new Text(composite, SWT.SINGLE | SWT.BORDER);
    m_reportersText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Label listenersDetailsLabel = new Label(composite, SWT.WRAP);
    listenersDetailsLabel.setText("Set the additional listeners/reporters (separated by space or ;) to be used when running the TestNG tests.");
    gd= new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan= 3;
    listenersDetailsLabel.setLayoutData(gd);
    
    loadDefaults();

    return composite;
  }

  private void loadDefaults() {
    m_workingProject = (IProject) getElement().getAdapter(IProject.class);

    // Populate the owner text field with the default value
    m_outdirPath.setText(TestNGPlugin.getDefault().getOutputDir(m_workingProject.getName()));
    m_reportersText.setText(TestNGPlugin.getDefault().getReporters(m_workingProject.getName()));
  }

  protected void performDefaults() {
    loadDefaults();
    super.performDefaults();
  }

  public boolean performOk() {
    TestNGPlugin.getDefault().storeOutputDir(m_workingProject.getName(), m_outdirPath.getText());
    TestNGPlugin.getDefault().storeReporters(m_workingProject.getName(), m_reportersText.getText());

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
      m_outdirPath.setText(((IContainer) dialog.getFirstResult()).getProjectRelativePath()
                           .toPortableString());
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
