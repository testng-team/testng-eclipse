package org.testng.eclipse.launch;


import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.collections.Lists;
import org.testng.eclipse.collections.Maps;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.launch.components.Filters;
import org.testng.eclipse.ui.Images;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.ProjectChooserDialog;
import org.testng.eclipse.ui.util.TestSelectionDialog;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.TestSearchEngine;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * TestNG specific launcher tab.
 *
 * @author cbeust
 */
public class TestNGMainTab extends AbstractLaunchConfigurationTab implements ILaunchConfigurationTab
{
  private static ImageRegistry m_imageRegistry = null;
  private static final String UNKNOWN_CONSTANT = "Unknown TestNGLaunchConfigurationConstants: ";

  private Text m_projectText;
  private IJavaProject m_selectedProject;

  // Single test class
  private TestngTestSelector m_classSelector;

  //method
  private TestngTestSelector m_methodSelector;

  // Group
  private GroupSelector m_groupSelector;

  // Suite
  private TestngTestSelector m_suiteSelector;

  // Package
  private TestngTestSelector m_packageSelector;

  private LaunchType m_typeOfTestRun = LaunchType.UNDEFINED;

  // Runtime group
  private Combo m_logLevelCombo;

  private List <TestngTestSelector> m_launchSelectors = Lists.newArrayList();
  private Map<String, List<String>> m_classMethods;

  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;

    Composite comp = new Composite(parent, SWT.NONE);
    comp.setLayout(layout);
    setControl(comp);

    createProjectSelectionGroup(comp);

    Group group = createGroup(comp, "TestNGMainTab.label.run"); //$NON-NLS-1$

    createSelectors(group);
    createRuntimeGroup(comp);
  }

  private void createSelectors(Composite comp) {

    // classSelector
    TestngTestSelector.ButtonHandler handler = new TestngTestSelector.ButtonHandler() {
      public void handleButton() {
        handleSearchButtonSelected(LaunchType.CLASS);
      }
    };
    m_classSelector = new TestngTestSelector(this, handler, LaunchType.CLASS,
        comp, "TestNGMainTab.label.test") {
      @Override
      public void initializeFrom(ILaunchConfiguration configuration) {
        List<String> testClassNames = ConfigurationHelper.getClasses(configuration);
        setText(Utils.listToString(testClassNames));
      }
    };
    m_launchSelectors.add(m_classSelector);

    // methodSelector
    handler = new TestngTestSelector.ButtonHandler() {
      public void handleButton() {
        handleSearchButtonSelected(LaunchType.METHOD);
      }
    };

    m_methodSelector = new TestngTestSelector(this, handler,
        LaunchType.METHOD, comp, "TestNGMainTab.label.method") {
      @Override
      public void initializeFrom(ILaunchConfiguration configuration) {
        List<String> names = ConfigurationHelper.getMethods(configuration);
        setText(Utils.listToString(names));
      }
    };
    m_launchSelectors.add(m_methodSelector);

    // groupSelector
    m_groupSelector = new GroupSelector(this, comp);
    m_launchSelectors.add(m_groupSelector);

    //packageSelector
    handler = new TestngTestSelector.ButtonHandler() {
      public void handleButton() {
        handleSearchButtonSelected(LaunchType.PACKAGE);
      }
    };
    m_packageSelector = new TestngTestSelector(this, handler,
        LaunchType.PACKAGE, comp, "TestNGMainTab.label.package") {
      @Override
      public void initializeFrom(ILaunchConfiguration configuration) {
        List<String> names = ConfigurationHelper.getPackages(configuration);
        setText(Utils.listToString(names));
      }
    };
    m_launchSelectors.add(m_packageSelector);

    // Work in progress
    if (false) {
      // suiteSelector
      handler = new TestngTestSelector.ButtonHandler() {
        public void handleButton() {
          handleSearchButtonSelected(LaunchType.SUITE);
        }
      };

      m_suiteSelector = new SuiteSelector(this, handler, comp);
    } else {
      m_suiteSelector = new SuiteSelector2(this, comp);
    }
//    m_suiteSelector = new SuiteSelector(this, handler, comp);
    m_launchSelectors.add(m_suiteSelector);

  }

  /**
   * @see ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
   */
  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    if(null == m_selectedProject) {
      m_selectedProject = JDTUtil.getJavaProjectContext();
    }
    ConfigurationHelper.createBasicConfiguration(m_selectedProject, config);
  }

  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public void initializeFrom(ILaunchConfiguration configuration) {
    updateProjectFromConfig(configuration);

    dettachModificationListeners();
    for (TestngTestSelector sel : m_launchSelectors) {
      sel.initializeFrom(configuration);
    }

    int logLevel = ConfigurationHelper.getLogLevel(configuration);
    m_logLevelCombo.select(logLevel);

    LaunchType type = ConfigurationHelper.getType(configuration);
    setType(type);
    if(LaunchType.METHOD == type) {
      m_classMethods = ConfigurationHelper.getClassMethods(configuration);
    }

    attachModificationListeners();
  }

  private void dettachModificationListeners() {
    for (TestngTestSelector sel : m_launchSelectors) {
      sel.detachModificationListener();
    }
  }

  private void attachModificationListeners() {
    for (TestngTestSelector sel : m_launchSelectors) {
      sel.attachModificationListener();
    }
  }

  protected void updateProjectFromConfig(ILaunchConfiguration configuration) {
    String projectName = ConfigurationHelper.getProjectName(configuration);
    if(null != projectName) {
      m_selectedProject = JDTUtil.getJavaProject(projectName);
      m_projectText.setText(projectName);
    }
  }

  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
   */
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    ConfigurationHelper.updateLaunchConfiguration(configuration,
        new ConfigurationHelper.LaunchInfo(m_projectText.getText(), 
              m_typeOfTestRun, 
              Utils.stringToList(m_classSelector.getText().trim()), 
              Utils.stringToList(m_packageSelector.getText().trim()), 
              m_classMethods, 
              m_groupSelector.getValueMap(),
              m_suiteSelector.getText(),
              TestNGLaunchConfigurationConstants.JDK15_COMPLIANCE, 
              m_logLevelCombo.getText()));
  }

  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
   */
  @Override
  public boolean isValid(ILaunchConfiguration launchConfig) {
    boolean result = getErrorMessage() == null;

    return result;
  }

  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
   */
  public String getName() {
    return ResourceUtil.getString("TestNGMainTab.tab.label"); //$NON-NLS-1$
  }

  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
   */
  @Override
  public Image getImage() {
    return Images.getTestNGImage();
  }

  public void validatePage() {
    setErrorMessage(null);
    setMessage(null);

    if(null == m_selectedProject) {
      setErrorMessage(ResourceUtil.getString("TestNGMainTab.error.projectnotdefined")); // $NON-NLS-1$

      return;
    }

    if(!m_selectedProject.getProject().exists()) {
      setErrorMessage(ResourceUtil.getFormattedString(
          "TestNGMainTab.error.projectnotexists", m_projectText.getText())); //$NON-NLS-1$

      return;
    }
    else if(!m_selectedProject.getProject().isOpen()) {
      setErrorMessage(ResourceUtil.getFormattedString(
          "TestNGMainTab.error.projectnotopen", m_projectText.getText())); //$NON-NLS-1$

      return;
    }

    if (getType() != LaunchType.UNDEFINED) {
      switch(getType()) {
        case CLASS:
          if(m_classSelector.getText().trim().length() < 1) {
            setErrorMessage(ResourceUtil.getString("TestNGMainTab.error.testclassnotdefined")); //$NON-NLS-1$
          }
          break;
        case SUITE:
          if(m_suiteSelector.getText().trim().length() < 1) {
            setErrorMessage(ResourceUtil.getString("TestNGMainTab.error.suitenotdefined")); //$NON-NLS-1$
          }
          break;
        case METHOD:
          if(m_methodSelector.getText().trim().length() < 1) {
            setErrorMessage(ResourceUtil.getString("TestNGMainTab.error.methodnotdefined")); //$NON-NLS-1$
          }
          break;
        case GROUP:
          if(m_groupSelector.getText().trim().length() < 1) {
            setErrorMessage(ResourceUtil.getString("TestNGMainTab.error.groupnotdefined")); //$NON-NLS-1$
          }
          break;
        case PACKAGE:
          if(m_packageSelector.getText().trim().length() < 1) {
            setErrorMessage(ResourceUtil.getString("TestNGMainTab.error.packagenotdefined")); //$NON-NLS-1$
          }
          break;
        default:
          throw new IllegalArgumentException(UNKNOWN_CONSTANT + getType());

      }
    }

  }

  /**
   * Displays the selection dialog for the specified launch type.
   * 
   * Package access for callbacks.
   * @param testngType - one of TestNGLaunchConfigurationConstants
   */
  void handleSearchButtonSelected(LaunchType testngType) {
    Object[] types = new Object[0];
    SelectionDialog dialog = null;

    IJavaProject selectedProject = getSelectedProject();
    if (selectedProject == null) {
      MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
          "No project", "Please select a project");
      return;
    }

    try {
      switch(testngType) {
        case CLASS:
          types = TestSearchEngine.findTests(getLaunchConfigurationDialog(),
              new Object[] {m_selectedProject}, Filters.SINGLE_TEST);
          dialog = TestSelectionDialog.createTestTypeSelectionDialog(getShell(), m_selectedProject,
              types, Filters.SINGLE_TEST);
          break;
        case METHOD:
          types = TestSearchEngine.findMethods(getLaunchConfigurationDialog(),
              new Object[] {m_selectedProject}, m_classSelector.getText());
          dialog = TestSelectionDialog.createMethodSelectionDialog(getShell(), m_selectedProject,
              types);
          break;
        case PACKAGE:
          types = TestSearchEngine.findPackages(getLaunchConfigurationDialog(),
              new Object[] {m_selectedProject});
          dialog = TestSelectionDialog.createPackageSelectionDialog(getShell(), m_selectedProject,
              types);
          break;
        default:
          throw new IllegalArgumentException(UNKNOWN_CONSTANT + testngType);
      }
    }
    catch(InterruptedException e) {
      TestNGPlugin.log(e);
    }
    catch(InvocationTargetException e) {
      TestNGPlugin.log(e.getTargetException());
    }
    dialog.setBlockOnOpen(true);
    dialog.setTitle(ResourceUtil.getString("TestNGMainTab.testdialog.title")); //$NON-NLS-1$
    if(dialog.open() == Window.CANCEL) {
      return;
    }

    Object[] results = dialog.getResult();
    if((results == null) || (results.length < 1)) {
      return;
    }
    Object type = results[0];

    if(type != null) {
      switch(testngType) {
        case CLASS:
          m_classSelector.setText((((IType) type).getFullyQualifiedName()).trim());
          m_selectedProject = ((IType) type).getJavaProject();
          break;
        case METHOD:
          String fullName = ((String) type);
          int index = fullName.lastIndexOf('.');
          String className = fullName.substring(0, index);
          String methodName = fullName.substring(index + 1);
          m_classSelector.setText(className);
          m_methodSelector.setText(methodName);
          m_classMethods = Maps.newHashMap();
          List<String> methods = Lists.newArrayList();
          methods.add(methodName);
          m_classMethods.put(className, methods);
          break;
        case SUITE:
          IFile file = (IFile) type;
          m_suiteSelector.setText(file.getProjectRelativePath().toOSString().trim());
          break;
        case PACKAGE:
          m_packageSelector.setText((String) type);
          break;
        default:
          throw new IllegalArgumentException(UNKNOWN_CONSTANT + testngType);
      }
    }

    updateDialog();
  }

  private void handleProjectTextModified() {
    String projectName = m_projectText.getText().trim();
    m_selectedProject = JDTUtil.getJavaProject(projectName);

    updateDialog();
  }

  private void handleProjectButtonSelected() {
    IJavaProject project = ProjectChooserDialog.getSelectedProject(getShell());

    if(project == null) {
      return;
    }

    m_selectedProject = project;
    m_projectText.setText(project.getElementName());

    updateDialog();
  }

  private void createRuntimeGroup(Composite parent) {
    //
    // Compliance
    //
    Group group = createGroup(parent, "TestNGMainTab.runtime.type"); //$NON-NLS-1$

    //
    // Log level
    //
    {
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.horizontalSpan = 2;
      Label label = new Label(group, SWT.LEFT);
      label.setLayoutData(gd);
      label.setText(ResourceUtil.getString("TestNGMainTab.testng.loglevel")); // $NON-NLS-1$

      m_logLevelCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
      gd = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
      gd.widthHint = 70;
      m_logLevelCombo.setLayoutData(gd);
      for(int i = 0; i < 11; i++) {
        m_logLevelCombo.add("" + i);
      }
      m_logLevelCombo.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent evt) {
          updateLaunchConfigurationDialog();
        }
      });
    }
  }

  private void createProjectSelectionGroup(Composite comp) {
    Group projectGroup = createGroup(comp, "TestNGMainTab.label.project"); //$NON-NLS-1$

    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 2;
    m_projectText = new Text(projectGroup, SWT.SINGLE | SWT.BORDER);
    m_projectText.setLayoutData(gd);
    m_projectText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        handleProjectTextModified();
      }
    });
    Button projectSearchButton = new Button(projectGroup, SWT.PUSH);
    projectSearchButton.setText(ResourceUtil.getString("TestNGMainTab.label.browse")); //$NON-NLS-1$
    projectSearchButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent evt) {
        handleProjectButtonSelected();
      }
    });
    SWTUtil.setButtonGridData(projectSearchButton);
  }

  private Group createGroup(Composite parent, String groupTitleKey) {
    Group group = new Group(parent, SWT.SHADOW_ETCHED_IN);
    group.setText(ResourceUtil.getString(groupTitleKey));

    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 3;
    group.setLayoutData(gd);

    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    group.setLayout(layout);

    return group;
  }

  // package access for callbacks
  void setEnabledRadios(boolean state) {
    for (TestngTestSelector sel : m_launchSelectors) {
      sel.enableRadio(state);
    }
  }

  // package not private, for callback access
  void setType(LaunchType type) {
    if(type != m_typeOfTestRun) {
      //      ppp("SET TYPE TO " + type + " (WAS " + m_typeOfTestRun + ")");
      m_typeOfTestRun = type;
      //////m_classMethods = null; // we reset it here, because the user has changed settings on front page
      for (TestngTestSelector sel : m_launchSelectors) {
        boolean select = (type == sel.getTestngType());
        sel.setRadioSelected(select);
        TestNGPlugin.bold(sel.getRadioButton(), select);
      }
    }
    updateDialog();
  }

  private LaunchType getType() {
    return m_typeOfTestRun;
  }

  public void updateDialog() {
    validatePage();
    updateLaunchConfigurationDialog();
  }

  public static void ppp(String s) {
    System.out.println("[TestNGMainTab] " + s);
  }

  public IJavaProject getSelectedProject() {
    return m_selectedProject;
  }

  @Override
  protected Shell getShell() {
    return super.getShell();
  }

  @Override
  protected ILaunchConfigurationDialog getLaunchConfigurationDialog() {
    return super.getLaunchConfigurationDialog();
  }

}
