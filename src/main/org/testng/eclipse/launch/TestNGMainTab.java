package org.testng.eclipse.launch;


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.components.CheckBoxTable;
import org.testng.eclipse.launch.components.Filters;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.ProjectChooserDialog;
import org.testng.eclipse.ui.util.TestSelectionDialog;
import org.testng.eclipse.ui.util.TypeParser;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.TestSearchEngine;
import org.testng.remote.RemoteTestNG;

/**
 * TestNG specific launcher tab.
 *
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 * @author cedric
 */
public class TestNGMainTab extends AbstractLaunchConfigurationTab implements ILaunchConfigurationTab
{
  private static ImageRegistry m_imageRegistry = null;
  
  private Text m_projectText;
  private IJavaProject m_selectedProject;

  // Single test class
  private Button m_classRadio;
  private Text m_classText;
  private Button m_classSearchButton;
  
  // Group
  private Button m_groupRadio;
  private Text m_groupText;
  private Button m_groupSearchButton;
  private Map m_groupMap = new HashMap();

  // Suite
  private Button m_suiteRadio;
  private Text m_suiteText;
  private Button m_suiteSearchButton;
  private Button m_suiteBrowseButton;

  private int m_typeOfTestRun = -1;
  
  // Runtime group
  private Combo m_complianceLevelCombo;
  private Combo m_logLevelCombo;
  
  private IJavaElement m_javaElement;
  private IResource[]  m_selection = new IResource[0];
  private ModifyListener m_classTextAdapter;
  private ModifyListener m_groupTextAdapter;
  private ModifyListener m_suiteTextAdapter;


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

    createClassGroup(group);
    createGroupGroup(group);
    createSuiteGroup(group);
    createRuntimeGroup(comp);  
  }


  /**
   * @see ILaunchConfigurationTab#setDefaults(ILaunchConfigurationWorkingCopy)
   */
  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    if(null == m_selectedProject) {
      m_selectedProject = JDTUtil.getJavaProjectContext();
    }
    ConfigurationHelper.createBasicConfiguration(m_selectedProject, config);
//    String projectName = (m_selectedProject == null) ? "" : m_selectedProject.getElementName();
//
//    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
//                        projectName);
//    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
//                        RemoteTestNG.class.getName());
//    config.setAttribute(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR,
//                        JDTUtil.getProjectVMVersion(m_selectedProject));
//    config.setAttribute(TestNGLaunchConfigurationConstants.TYPE, TestNGLaunchConfigurationConstants.CLASS);
//    config.setAttribute(TestNGLaunchConfigurationConstants.LOG_LEVEL, "2"); 
  }


  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
   */
  public void initializeFrom(ILaunchConfiguration configuration) {
    updateProjectFromConfig(configuration);

    dettachModificationListeners();
    List testClassNames = ConfigurationHelper.getClasses(configuration);
    m_classText.setText(Utils.listToString(testClassNames));

    List groupNames = ConfigurationHelper.getGroups(configuration);
    m_groupText.setText(Utils.listToString(groupNames));
    m_groupMap.clear();
    List groupClassNames = ConfigurationHelper.getGroupClasses(configuration);
    if(null != groupNames) { 
      for(int i = 0; i < groupNames.size(); i++) {
        m_groupMap.put(groupNames.get(i), groupClassNames);
      }
    }
    
    List suites = ConfigurationHelper.getSuites(configuration);
    m_suiteText.setText(Utils.listToString(suites));
    
    int logLevel = ConfigurationHelper.getLogLevel(configuration);
    m_logLevelCombo.select(logLevel);
    
    updateComplianceLevel(configuration);

    setType(ConfigurationHelper.getType(configuration));
    
    attachModificationListeners();
  }

  private void dettachModificationListeners() {
    m_classText.removeModifyListener(m_classTextAdapter);
    m_groupText.removeModifyListener(m_groupTextAdapter);
    m_suiteText.removeModifyListener(m_suiteTextAdapter);
  }
  
  private void attachModificationListeners() {
    m_classText.addModifyListener(m_classTextAdapter);
    m_groupText.addModifyListener(m_groupTextAdapter);
    m_suiteText.addModifyListener(m_suiteTextAdapter);
  }
  
  protected void updateProjectFromConfig(ILaunchConfiguration configuration) {
    String projectName = ConfigurationHelper.getProjectName(configuration);
    if(null != projectName) {
      m_selectedProject = JDTUtil.getJavaProject(projectName);
      m_projectText.setText(projectName);
    }
  }

  protected void updateComplianceLevel(ILaunchConfiguration configuration) {
    final String complianceLevel = ConfigurationHelper.getComplianceLevel(configuration);
    
    String[] options = m_complianceLevelCombo.getItems();
    for(int i = 0; i < options.length; i++) {
      if(options[i].equals(complianceLevel)) {
        m_complianceLevelCombo.select(i);
      }
    }
  }
  
  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
   */
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    ConfigurationHelper.updateLaunchConfiguration(configuration,
        new ConfigurationHelper.LaunchInfo(m_projectText.getText(),
            m_typeOfTestRun,
            Utils.stringToList(m_classText.getText().trim()),
            m_groupMap,
            m_suiteText.getText(),
            m_complianceLevelCombo.getText(),
            m_logLevelCombo.getText()));
  }

  /**
   * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
   */
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
  public Image getImage() {
    return getTestNGImage();
  }
  
  /**
   * Method to retreive TestNG icon Image object.
   * <p>
   * Code adopted from {@link org.eclipse.jdt.internal.debug.ui.JavaDebugImages}
   * and {@link org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin} classes.
   * 
   * @return
   */
  public static Image getTestNGImage() {
    final String key = "icon";
    if (m_imageRegistry == null) {
      Display display = Display.getCurrent();
      if (display == null) {
        display= Display.getDefault();
      }
      m_imageRegistry= new ImageRegistry(display);
      m_imageRegistry.put(key, TestNGPlugin.getImageDescriptor("main16/testng.gif"));
    }
    return m_imageRegistry.get(key);
  }

  public void validatePage() {
    setErrorMessage(null);
    setMessage(null);

    if(null == m_selectedProject) {
      setErrorMessage(ResourceUtil.getString("TestNGMainTab.error.projectnotdefined")); // $NON-NLS-1$

      return;
    }

    if(!m_selectedProject.getProject().exists()) {
      setErrorMessage(ResourceUtil.getFormattedString("TestNGMainTab.error.projectnotexists", m_projectText.getText())); //$NON-NLS-1$

      return;
    } else if (!m_selectedProject.getProject().isOpen()) {
      setErrorMessage(ResourceUtil.getFormattedString("TestNGMainTab.error.projectnotopen", m_projectText.getText())); //$NON-NLS-1$

      return;
    }

    if(getType() == TestNGLaunchConfigurationConstants.SUITE) {
      if(m_suiteText.getText().trim().length() < 1) {
        setErrorMessage(ResourceUtil.getString("TestNGMainTab.error.suitenotdefined")); //$NON-NLS-1$
      }
    }

    if(getType() == TestNGLaunchConfigurationConstants.CLASS) {
      if(m_classText.getText().trim().length() < 1) {
        setErrorMessage(ResourceUtil.getString("TestNGMainTab.error.testclassnotdefined")); //$NON-NLS-1$
      }
    }

    if(getType() == TestNGLaunchConfigurationConstants.GROUP) {
      if(m_groupText.getText().trim().length() < 1) {
        setErrorMessage(ResourceUtil.getString("TestNGMainTab.error.groupnotdefined")); //$NON-NLS-1$
      }
    }
  }

  /**
   * Invoked when the Search button for groups is pressed.
   */
  public void handleGroupSearchButtonSelected() {
    Map groups = new HashMap();

    try {
      IJavaProject[] dependencies= new IJavaProject[0];
      String[] dependencyPrjNames= null;
      try {
        dependencyPrjNames= m_selectedProject.getRequiredProjectNames();
        if(null != dependencyPrjNames) {
          dependencies= new IJavaProject[dependencyPrjNames.length];
          for(int i= 0; i < dependencyPrjNames.length; i++) {
            dependencies[i]= JDTUtil.getJavaProject(dependencyPrjNames[i]);
          }
        }
      }
      catch(JavaModelException jmex) {
        ; // ignore for the moment
      }
      
      Object[] projects= new Object[1 + dependencies.length];
      projects[0]= m_selectedProject;
      System.arraycopy(dependencies, 0, projects, 1, dependencies.length);
      Object[] types = TestSearchEngine.findTests(getLaunchConfigurationDialog(),
          projects,
          Filters.SINGLE_TEST);
      
      for (int i = 0; i < types.length; i++) {
        Object t = types[i];
        if (t instanceof IType) {
          IType type = (IType) t;
          ITestContent content = TypeParser.parseType(type);
          Collection groupNames = content.getGroups();
          if(!groupNames.isEmpty()) {
            for(Iterator it = groupNames.iterator(); it.hasNext(); ) {
              String groupName = (String) it.next();
              List rtypes = (List) groups.get(groupName);
              if(null == rtypes) {
                rtypes = new ArrayList();
                groups.put(groupName, rtypes);
              }
              
              rtypes.add(type.getFullyQualifiedName());
            }
          }
        }
      }
    }
    catch (InvocationTargetException e) {
      TestNGPlugin.log(e);
    }
    catch (InterruptedException e) {
      TestNGPlugin.log(e);
    }

    
    String[] uniqueGroups = (String[]) groups.keySet().toArray(new String[groups.size()]);
    Arrays.sort(uniqueGroups);
    final CheckBoxTable cbt = new CheckBoxTable(getShell(), uniqueGroups);
    String content = m_groupText.getText();
    if (! Utils.isEmpty(content)) {
      List s = Utils.stringToList(content);
      String[] existingGroups = (String[]) s.toArray(new String[s.size()]);
      cbt.checkElements(existingGroups);
    }
    if(SelectionStatusDialog.CANCEL != cbt.open()) {
      String[] selectedGroups = cbt.getSelectedElements();
      
      m_groupMap = new HashMap();
      for(int i = 0; i < selectedGroups.length; i++) {
        m_groupMap.put(selectedGroups[i], groups.get(selectedGroups[i]));
      }
      
      m_groupText.setText(Utils.listToString(Arrays.asList(selectedGroups)));
    }
    
    updateDialog();
  }
  
  private void handleSuiteSearchButtonSelected() {
    handleTestClassSearchButtonSelected(null);
  }

  private void handleTestClassSearchButtonSelected(Filters.ITypeFilter filter) {
    Object[]  types = new Object[0];

    try {
      // Temporary hack to look up test classes or suites.  Eventually, we
      // should invoke the same method with two different filters.
      if(null != filter) {
        types = TestSearchEngine.findTests(getLaunchConfigurationDialog(),
                                           new Object[] { m_selectedProject },
                                           filter);
      }
      else {
        types = TestSearchEngine.findSuites(getLaunchConfigurationDialog(),
                                            new Object[] { m_selectedProject });
      }
    }
    catch(InterruptedException e) {
      TestNGPlugin.log(e);
    }
    catch(InvocationTargetException e) {
      TestNGPlugin.log(e.getTargetException());
    }

    SelectionDialog dialog = null;

    if(null != filter) {
      dialog = TestSelectionDialog.createTestTypeSelectionDialog(getShell(), m_selectedProject, types, filter);
    }
    else {
      dialog = TestSelectionDialog.createSuiteSelectionDialog(getShell(), m_selectedProject, types);
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
      if(type instanceof IType) {
        m_classText.setText((((IType) type).getFullyQualifiedName()).trim());
        m_selectedProject = ((IType) type).getJavaProject();
      }
      else if(type instanceof IFile) {
        IFile file = (IFile) type;
        m_suiteText.setText(file.getProjectRelativePath().toOSString().trim());
      }
      m_projectText.setText(m_selectedProject.getElementName());
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
    
    {
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.horizontalSpan = 2;
      Label label = new Label(group, SWT.LEFT);
      label.setLayoutData(gd);
      label.setText(ResourceUtil.getString("TestNGMainTab.testng.compliance")); // $NON-NLS-1$
      
      m_complianceLevelCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
      m_complianceLevelCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
      m_complianceLevelCombo.add(TestNGLaunchConfigurationConstants.JDK15_COMPLIANCE);
      m_complianceLevelCombo.add(TestNGLaunchConfigurationConstants.JDK14_COMPLIANCE);
      m_complianceLevelCombo.select(0);
      GridData gd2 = new GridData(GridData.HORIZONTAL_ALIGN_END  | GridData.GRAB_HORIZONTAL);
      gd2.widthHint = 50; // HINT: originally minimumWidth (widthHint is supported in older API version)
      m_complianceLevelCombo.setLayoutData(gd2);
      m_complianceLevelCombo.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent evt) {
          updateLaunchConfigurationDialog();
        }
      });
    }
    
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
      gd.widthHint = 50;
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
    Group    projectGroup = createGroup(comp, "TestNGMainTab.label.project"); //$NON-NLS-1$

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
        public void widgetSelected(SelectionEvent evt) {
          handleProjectButtonSelected();
        }
      });
    SWTUtil.setButtonGridData(projectSearchButton);
  }

  private void createClassGroup(Composite comp) {
    m_classTextAdapter = new TextAdapter(TestNGLaunchConfigurationConstants.CLASS);
    SelectionAdapter radioAdapter = new RadioAdapter(TestNGLaunchConfigurationConstants.CLASS);
    SelectionAdapter buttonAdapter = new ButtonAdapter(TestNGLaunchConfigurationConstants.CLASS) {
      public void handleButton() {
        handleTestClassSearchButtonSelected(Filters.SINGLE_TEST);
      }
    };
    
    Utils.Widgets wt = Utils.createWidgetTriple(comp,
                                                "TestNGMainTab.label.test",
                                                radioAdapter, 
                                                buttonAdapter, 
                                                m_classTextAdapter);
        
    m_classRadio = wt.radio;
    m_classText = wt.text;
    m_classSearchButton = wt.button;
  }
  
  private void createGroupGroup(Composite group) {
    m_groupTextAdapter = new TextAdapter(TestNGLaunchConfigurationConstants.GROUP);    
    SelectionAdapter radioAdapter = new RadioAdapter(TestNGLaunchConfigurationConstants.GROUP);
    SelectionAdapter buttonAdapter = new ButtonAdapter(TestNGLaunchConfigurationConstants.GROUP) {
      public void handleButton() {
        handleGroupSearchButtonSelected();
      }
    };
    
    Utils.Widgets wt = Utils.createWidgetTriple(group,
                                                "TestNGMainTab.label.group",
                                                radioAdapter, 
                                                buttonAdapter, 
                                                m_groupTextAdapter);
        
    m_groupRadio = wt.radio;
    m_groupText = wt.text;
    m_groupText.setEditable(false);
    m_groupSearchButton = wt.button;
  }
  
  private void createSuiteGroup(Composite comp) {
    m_suiteTextAdapter = new TextAdapter(TestNGLaunchConfigurationConstants.SUITE);
    SelectionAdapter radioAdapter = new RadioAdapter(TestNGLaunchConfigurationConstants.SUITE);
    SelectionAdapter buttonAdapter = new ButtonAdapter(TestNGLaunchConfigurationConstants.SUITE) {
      public void handleButton() {
        handleSuiteSearchButtonSelected();
      }
    };
    
    Utils.Widgets wt = Utils.createWidgetTriple(comp,
                                                "TestNGMainTab.label.suiteTest",
                                                radioAdapter, 
                                                buttonAdapter, 
                                                m_suiteTextAdapter);
        
    m_suiteRadio = wt.radio;
    m_suiteText = wt.text;
    m_suiteSearchButton = wt.button;
    
    Composite fill= new Composite(comp, SWT.NONE);
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
    gd.horizontalSpan= 2;
    gd.verticalIndent= 0;
    gd.heightHint= 1;
    fill.setLayoutData(gd);

    //
    // Search button
    //
    m_suiteBrowseButton = new Button(comp, SWT.PUSH);
    m_suiteBrowseButton.setText(ResourceUtil.getString("TestNGMainTab.label.browsefs")); //$NON-NLS-1$
    m_suiteBrowseButton.addSelectionListener(new ButtonAdapter(TestNGLaunchConfigurationConstants.SUITE) {
      public void handleButton() {
        FileDialog fileDialog= new FileDialog(m_suiteBrowseButton.getShell(), SWT.OPEN);
        m_suiteText.setText(fileDialog.open());
      }
    });
    gd= new GridData();
    gd.verticalIndent= 0;
    m_suiteBrowseButton.setLayoutData(gd);
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
  
  private void setEnabledRadios(boolean state) {
    m_classRadio.setEnabled(state);
    m_groupRadio.setEnabled(state);
    m_suiteRadio.setEnabled(state);
  }

  private void setType(int type) {
    if (type != m_typeOfTestRun) {
//      ppp("SET TYPE TO " + type + " (WAS " + m_typeOfTestRun + ")");
      m_typeOfTestRun = type;
      m_classRadio.setSelection(type == TestNGLaunchConfigurationConstants.CLASS);
      m_groupRadio.setSelection(type == TestNGLaunchConfigurationConstants.GROUP);
      m_suiteRadio.setSelection(type == TestNGLaunchConfigurationConstants.SUITE);
      
      TestNGPlugin.bold(m_classRadio, type == TestNGLaunchConfigurationConstants.CLASS);
      TestNGPlugin.bold(m_groupRadio, type == TestNGLaunchConfigurationConstants.GROUP);
      TestNGPlugin.bold(m_suiteRadio, type == TestNGLaunchConfigurationConstants.SUITE);
    }

    updateDialog();
  }
  
  private int getType() {
    return m_typeOfTestRun;
  }

  public void updateDialog() {
    validatePage();
    updateLaunchConfigurationDialog();
  }

  public static void ppp(String s) {
    System.out.println("[TestNGMainTab] " + s);
  }
  
  /////
  // RadioAdapter
  //
  
  class RadioAdapter extends SelectionAdapter {
    private int m_type;
    
    public RadioAdapter(int type) {
      m_type = type;
    }
    
    public void widgetSelected(SelectionEvent evt) {
      if(((Button) evt.widget).getSelection()) {
        setType(m_type);
      }
    }
  }
  
  //
  // RadioAdapter
  /////
  
  /////
  // TextAdapter
  //
  
  class TextAdapter implements ModifyListener {
    int m_type;
    
    public TextAdapter(int type) {
      m_type = type;
    }
    
    public void modifyText(ModifyEvent evt) {
      setType(m_type);
    }
  }    
  
  //
  // TextAdapter
  /////
  
  /////
  // ButtonAdapter
  //
  
  abstract class ButtonAdapter extends SelectionAdapter {
    private int m_type;
    
    public ButtonAdapter(int type) {
      m_type = type;
    }
    
    public abstract void handleButton();
    
    public void widgetSelected(SelectionEvent evt) {
      setType(m_type);
      try {
        setEnabledRadios(false);
        handleButton();
      }
      finally {
        setEnabledRadios(true);
      }
    };
    
    //
    // ButtonAdapter
    /////
      
  };
  
}
