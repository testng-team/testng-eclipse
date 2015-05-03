package org.testng.eclipse.launch;


import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.components.Filters;
import org.testng.eclipse.launch.components.ProjectBrowserComposite;
import org.testng.eclipse.launch.components.SelectionTableComposite;
import org.testng.eclipse.launch.components.SelectionTableComposite.AbstractSelectionTableProvider;
import org.testng.eclipse.runner.RemoteTestNG;
import org.testng.eclipse.ui.util.ProjectChooserDialog;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;


public class AlternateLaunchConfigurationTab extends AbstractLaunchConfigurationTab
implements ILaunchConfigurationTab {
  protected ScrolledComposite m_scrolledComposite;
  protected Composite m_parent;
  protected Text m_projectNameText;
  protected Button m_useSourceDirsRadio;
  
  protected ProjectBrowserComposite m_testProjectBrowser;
  protected SelectionTableComposite m_testComposite;
  protected ProjectBrowserComposite m_sourceProjectBrowser;
  protected SelectionTableComposite m_sourceComposite;

  protected IJavaProject m_workingProject;
  
  
  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    ppp(".createControl");
    parent.setLayout(new FillLayout());

    m_scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);

    Composite m_parent = new Composite(m_scrolledComposite, SWT.NONE);
    m_parent.setLayout(new GridLayout(3, false));

    createProjectGroup(m_parent);
    
    Composite selectionComposite = new Composite(m_parent, SWT.NONE);
    selectionComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
    selectionComposite.setLayout(new FillLayout(SWT.VERTICAL));
    
    createTestGroup(selectionComposite);
    
    createSourceSelectionGroup(selectionComposite);

    // Set the child as the scrolled content of the ScrolledComposite
    m_scrolledComposite.setContent(m_parent);

    // Set the minimum size
    m_scrolledComposite.setMinSize(400, 400);

    // Expand both horizontally and vertically
    m_scrolledComposite.setExpandVertical(true);
    m_scrolledComposite.setExpandHorizontal(true);
    setControl(m_scrolledComposite);
  }

  protected void createProjectGroup(Composite parent) {
    Group projectGroup = createGroup(parent, 
                                     ResourceUtil.getString("AlternateLaunchConfigurationTab.project")); //$NON-NLS-1$
    
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan   = 2;
    m_projectNameText = new Text(projectGroup, SWT.SINGLE | SWT.BORDER);
    m_projectNameText.setLayoutData(gd);
    m_projectNameText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent evt) {
        updateLaunchConfigurationDialog();        
      }
    });
    
    Button projectSearchButton = new Button(projectGroup, SWT.PUSH);
    projectSearchButton.setText(ResourceUtil.getString("TestNGMainTab.label.browse")); //$NON-NLS-1$
    projectSearchButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent evt) {
        handleProjectButtonSelected();
      }
    });
    SWTUtil.setButtonGridData(projectSearchButton);
    
    /*Group projectGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
    projectGroup.setText(ResourceUtil.getString("AlternateLaunchConfigurationTab.project"));  //$NON-NLS-1$

    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 3;
    projectGroup.setLayoutData(gd);
    projectGroup.setLayout(new FillLayout());
    
    m_projectNameText= new Text(projectGroup, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
    IJavaProject selectedProject = JDTUtil.getJavaProjectContext();
    if(null != selectedProject) {
      m_projectNameText.setText(selectedProject.getElementName());
    }*/
  }

  protected void handleProjectButtonSelected() {
    IJavaProject project = ProjectChooserDialog.getSelectedProject(getShell());
    
    if (project == null) {
      return;
    }
    
    m_workingProject = project;
    m_projectNameText.setText(project.getElementName());
    m_testProjectBrowser.setInput(m_workingProject);
    m_sourceProjectBrowser.setInput(m_workingProject);
  }
  
  protected void createTestGroup(Composite parent) {
    Group group = createGroup(parent, 
                              ResourceUtil.getString("AlternateLaunchConfigurationTab.testSelectionGroup"));  //$NON-NLS-1$
    
    m_testProjectBrowser = new ProjectBrowserComposite(group);

    GridData gd = new GridData();
    gd.horizontalAlignment = GridData.FILL;
    gd.grabExcessHorizontalSpace = true;
    gd.grabExcessVerticalSpace = true;
    gd.verticalAlignment = GridData.FILL;
    m_testProjectBrowser.getTree().setLayoutData(gd);
//    IJavaProject projectSelection = JDTUtil.getJavaProjectContext();
//    if(null != projectSelection) {
//      m_testProjectBrowser.setInput(projectSelection.getProject().getParent());
//    }
    m_testProjectBrowser.addViewerFilter(Filters.TEST_FILTER);
    
    Composite buttonsComposite = new Composite(group, SWT.NONE);
    buttonsComposite.setLayout(new GridLayout(1, true));
    buttonsComposite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL));
    
    Button btnUpdate = new Button(buttonsComposite, SWT.PUSH | SWT.BORDER);
    btnUpdate.setText("Update");
    SWTUtil.setButtonGridData(btnUpdate);
    btnUpdate.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        handleUpdateAction(m_testComposite, m_testProjectBrowser.getCheckedElements());
      }
      
    });
    
    Button btnSearchTests = new Button(buttonsComposite, SWT.PUSH | SWT.BORDER);
    btnSearchTests.setText("Tests");
    btnSearchTests.setEnabled(false);
    SWTUtil.setButtonGridData(btnSearchTests);
    
    Button btnSearchSuites = new Button(buttonsComposite, SWT.PUSH | SWT.BORDER);
    btnSearchSuites.setText("Suites");
    btnSearchSuites.setEnabled(false);
    SWTUtil.setButtonGridData(btnSearchSuites);
    
    m_testComposite = new SelectionTableComposite(group, 
        new String[] {"T", "Name", "Path"},
        new boolean[] {true, true, false},
        new SelectionTableComposite.AbstractSelectionTableProvider() {
          public String getColumnText(Object element, int columnIndex) {
            IResource ires = (IResource) element;
            
            switch(columnIndex) {
              case 0:
                return getResourceType(ires);
              case 1:
                return ires.getName();
              case 2:
                return ires.getFullPath().toOSString();
            }
            
            return ""; //$NON-NLS-1$
          }
        }
    );
    gd = new GridData();
    gd.horizontalAlignment = GridData.FILL;
    gd.grabExcessHorizontalSpace = true;
    gd.grabExcessVerticalSpace = true;
    gd.verticalAlignment = GridData.FILL;
    m_testComposite.getTable().setLayoutData(gd);
  }
  
  private void handleUpdateAction(final SelectionTableComposite selectionComposite,
                                  final Object[] elements) {
    selectionComposite.setInput(new StructuredSelection(elements));
    
    if(selectionComposite == m_testComposite) { 
        if(null != elements && elements.length > 0) {
          setErrorMessage(null);
        }
        else {
          setErrorMessage("No tests selected");
        }
    }
    updateLaunchConfigurationDialog();
  }
  
  protected void createSourceSelectionGroup(final Composite parent) {
    Group group = createGroup(parent, 
                              ResourceUtil.getString("AlternateLaunchConfigurationTab.jdk14.sourcedir")); //$NON-NLS-1$
    
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 3;
    m_useSourceDirsRadio = new Button(group, SWT.CHECK);
    m_useSourceDirsRadio.setText(ResourceUtil.getString("AlternateLaunchConfigurationTab.jdk14.selection")); //$NON-NLS-1$
    m_useSourceDirsRadio.setLayoutData(gd);
    m_useSourceDirsRadio.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        boolean enabled = m_useSourceDirsRadio.getSelection();
      }
    });
    
    m_sourceProjectBrowser = new ProjectBrowserComposite(group);
    gd = new GridData();
    gd.horizontalAlignment = GridData.FILL;
    gd.grabExcessHorizontalSpace = true;
    gd.grabExcessVerticalSpace = true;
    gd.verticalAlignment = GridData.FILL;
    m_sourceProjectBrowser.getTree().setLayoutData(gd);
//    IJavaProject projectSelection = JDTUtil.getJavaProjectContext();
//    if(null != projectSelection) {
//      m_sourceProjectBrowser.setInput(projectSelection.getProject().getParent());
//    }
    m_sourceProjectBrowser.addViewerFilter(Filters.SOURCE_DIRECTORY_FILTER);
    
    Button btnUpdate = new Button(group, SWT.PUSH | SWT.BORDER);
    btnUpdate.setText(ResourceUtil.getString("AlternateLaunchConfigurationTab.label.update"));
    gd = new GridData();
    gd.verticalAlignment = GridData.BEGINNING;
    btnUpdate.setLayoutData(gd);
    SWTUtil.setButtonDimensionHint(btnUpdate);
    btnUpdate.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        handleUpdateAction(m_sourceComposite, m_sourceProjectBrowser.getCheckedElements());
      }
    });
    
    m_sourceComposite = new SelectionTableComposite(group, 
        new String[] {"Path"},
        new boolean[] {false},
        new SelectionTableComposite.AbstractSelectionTableProvider() {
          public String getColumnText(Object element, int columnIndex) {
            return ((IResource) element).getFullPath().toOSString();
          }
        }
    );
    gd = new GridData();
    gd.horizontalAlignment = GridData.FILL;
    gd.grabExcessHorizontalSpace = true;
    gd.grabExcessVerticalSpace = true;
    gd.verticalAlignment = GridData.FILL;
    m_sourceComposite.getTable().setLayoutData(gd);
  }
  
  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
   */
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    ppp(".setDefaults");
    if(null == m_workingProject) {
      m_workingProject = JDTUtil.getJavaProjectContext();
    }
    String projectName = m_workingProject == null ? "" : m_workingProject.getElementName();
    
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                               projectName);
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                               RemoteTestNG.class.getName());
  }

  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public void initializeFrom(ILaunchConfiguration configuration) {
    ppp(".initializeFrom");
    try {
      IJavaProject ijp = LaunchConfigurationHelper.getProject(configuration);
      IResource[] initialSelection = LaunchConfigurationHelper.findTestResources(configuration);
      IResource[] sourceDirSelection = null; /*LaunchConfigurationHelper.findResources(configuration,
          TestNGLaunchConfigurationConstants.SOURCE_DIR_LIST);*/
      
      m_workingProject = ijp;
      updateUI(ijp, initialSelection, sourceDirSelection);
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }
  }

  private void updateUI(IJavaProject ijp, IResource[] testSelection, IResource[] sourceSelection) {
    if(!validateForm()) {
      return;
    }
    
    setErrorMessage(null);
    setMessage(null);
    
    m_projectNameText.setText(ijp.getElementName());
    
    m_testProjectBrowser.setInput(ijp);
    m_testProjectBrowser.setSelection(testSelection);
    handleUpdateAction(m_testComposite, testSelection);
    
    m_sourceProjectBrowser.setInput(ijp);
    if(sourceSelection.length > 0) {
      m_useSourceDirsRadio.setSelection(true);
      m_sourceProjectBrowser.setSelection(sourceSelection);
      handleUpdateAction(m_sourceComposite, sourceSelection);
    }
  }

  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
   */
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    ppp(".performApply");
    if(!validateForm()) {
      return;
    }
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                               m_workingProject.getElementName());
    configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                               RemoteTestNG.class.getName());
    
    Object[] checkedElements = m_testComposite.getCheckedElements();
    configuration.setAttribute(TestNGLaunchConfigurationConstants.DIRECTORY_TEST_LIST,
                               extractResources(checkedElements, JDTUtil.DIRECTORY_TYPE));
    configuration.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
                               extractResources(checkedElements, JDTUtil.CLASS_TYPE));
    configuration.setAttribute(TestNGLaunchConfigurationConstants.SOURCE_TEST_LIST,
                               extractResources(checkedElements, JDTUtil.SOURCE_TYPE));
    configuration.setAttribute(TestNGLaunchConfigurationConstants.SUITE_TEST_LIST,
                               extractResources(checkedElements, JDTUtil.SUITE_TYPE));
    
  }

  protected boolean validateForm() {
    if(null == m_workingProject) {
      setErrorMessage("No project selected"); //$NON-NLS-1$
    }
    
    return m_workingProject != null;
  }
  
  public boolean isValid(ILaunchConfiguration launchConfig) {
    return getErrorMessage() == null;
  }

  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
   */
  public String getName() {
    return ResourceUtil.getString("AlternateLaunchConfigurationTab.name");
  }

  protected List extractResources(Object[] elements, final String type) {
    List result = new ArrayList();
    
    for(int i = 0; i < elements.length; i++) {
      IResource iresource = (IResource) elements[i];
      if(type.equals(JDTUtil.getResourceType(iresource))) {
        result.add(iresource.getFullPath().toOSString());
      }
    }
    
    return result;
  }
  
  protected Group createGroup(Composite parent, String groupTitle) {
    Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
    group.setText(groupTitle);

    GridData gd = new GridData();
    gd.grabExcessHorizontalSpace = true;
    gd.grabExcessVerticalSpace = true;
    gd.horizontalAlignment = GridData.FILL;
    gd.verticalAlignment = GridData.BEGINNING;
    gd.horizontalSpan = 3;
    gd.horizontalIndent = 0;
    group.setLayoutData(gd);
    group.setLayout(new GridLayout(3, false));
    
    return group;
  }
  
  private static void ppp(final Object msg) {
    System.out.println("[AlternateLaunchConfigurationTab]: " + msg);
  }
}
