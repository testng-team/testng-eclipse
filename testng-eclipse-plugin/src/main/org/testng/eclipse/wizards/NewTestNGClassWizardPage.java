package org.testng.eclipse.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.StringUtils;
import org.testng.eclipse.util.Utils;
import org.testng.eclipse.util.Utils.JavaElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO refactor by extending org.eclipse.jdt.ui.wizards.NewTypeWizardPage, 
 *      see org.eclipse.jdt.junit.wizards.NewTestCaseWizardPageOne
 * 
 *
 * Generate a new TestNG class and optionally, the corresponding XML suite file.
 */
public class NewTestNGClassWizardPage extends WizardPage {
  private Text m_sourceFolderText;
  private Text m_packageNameText;
  private Text m_classNameText;
  private Text m_xmlFilePath;

  private Map<String, Button> m_annotations = new HashMap<String, Button>();
  private List<JavaElement> m_elements;
  public static final String[] ANNOTATIONS = new String[] {
    "BeforeMethod", "AfterMethod", "DataProvider",
    "BeforeClass", "AfterClass", "",
    "BeforeTest",  "AfterTest", "",
    "BeforeSuite", "AfterSuite", ""
  };

  public NewTestNGClassWizardPage() {
    super(ResourceUtil.getString("NewTestNGClassWizardPage.title"));
    setTitle(ResourceUtil.getString("NewTestNGClassWizardPage.title"));
    setDescription(ResourceUtil.getString("NewTestNGClassWizardPage.description"));
  }

  /**
   * @see IDialogPage#createControl(Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    container.setLayout(layout);

    createTop(container);
    createBottom(container);

    initialize();
    dialogChanged();
    setControl(container);
  }

  private void createTop(Composite parent) {
    final Composite container = SWTUtil.createGridContainer(parent, 3);

    //
    // Source folder
    //
    {
      m_sourceFolderText = SWTUtil.createPathBrowserText(container, "&Source folder:",
          new ModifyListener() {
            public void modifyText(ModifyEvent e) {
              dialogChanged();
            }
          });
    }

    //
    // Package name
    //
    {
      Label label = new Label(container, SWT.NULL);
      label.setText("&Package name:");
      m_packageNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
      m_packageNameText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          dialogChanged();
        }
      });
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      m_packageNameText.setLayoutData(gd);
      Button button = new Button(container, SWT.PUSH);
      button.setText("Browse...");
      button.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          handleBrowsePackages(container.getShell());
        }
      });
    }

    //
    // Class name
    //
    {
      Label label = new Label(container, SWT.NULL);
      label.setText("&Class name:");
      m_classNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      m_classNameText.setLayoutData(gd);
      m_classNameText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          dialogChanged();
        }
      });
    }
  }

  private void createBottom(Composite parent) {
    //
    // Annotations
    //
    {
      Group g = new Group(parent, SWT.SHADOW_ETCHED_OUT);
      g.setText("Annotations");
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      g.setLayoutData(gd);

      GridLayout layout = new GridLayout();
      g.setLayout(layout);
      layout.numColumns = 3;

      for (String label : ANNOTATIONS) {
        if ("".equals(label)) {
          new Label(g, SWT.NONE);
        } else {
          Button b = new Button(g, "".equals(label) ? SWT.None : SWT.CHECK);
          m_annotations.put(label, b);
          b.setText("@" + label);
        }
      }
    }

    //
    // XML suite file
    //
    {
      Composite container = SWTUtil.createGridContainer(parent, 2);

      //
      // Label
      //
      Label label = new Label(container, SWT.NULL);
      label.setText(ResourceUtil.getString("TestNG.newClass.suitePath"));

      //
      // Text widget
      //
      m_xmlFilePath = new Text(container, SWT.SINGLE | SWT.BORDER);
      GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
      gd.grabExcessHorizontalSpace = true;
      m_xmlFilePath.setLayoutData(gd);
    }
  }

  /**
   * Tests if the current workbench selection is a suitable container to use.
   */
  private void initialize() {
    m_elements = Utils.getSelectedJavaElements();
    if (m_elements.size() > 0) {
      JavaElement sel = m_elements.get(0);
      if (sel.sourceFolder != null) {
        m_sourceFolderText.setText(sel.sourceFolder);
      }
      if (sel.getPackageName() != null) {
        m_packageNameText.setText(sel.getPackageName());
      }
      String className = StringUtils.isEmptyString(sel.getClassName())
          ? "NewTest" : sel.getClassName() + "Test";
      m_classNameText.setText(className);
    }
  }

  public List<JavaElement> getJavaElements() {
    return m_elements;
  }

  private void handleBrowsePackages(Shell dialogParrentShell) {
    try {
      IResource sourceContainer = ResourcesPlugin.getWorkspace().getRoot().findMember(
          new Path(getSourceFolder()));
      IJavaProject javaProject = JDTUtil.getJavaProject(sourceContainer.getProject().getName());
      
      SelectionDialog dialog = JavaUI.createPackageDialog(dialogParrentShell, javaProject, 0);
      dialog.setTitle("Package selection");
      dialog.setMessage("&Choose a package:");
      
      if (dialog.open() == SelectionDialog.OK) {
        Object[] selectedPackages = dialog.getResult();
        if (selectedPackages.length == 1) {
          m_packageNameText.setText(((IPackageFragment) selectedPackages[0]).getElementName());
        }
      }
    } catch (JavaModelException e) {
      updateStatus("Failed to list packages.");
    }
  }

  /**
   * Ensures that both text fields are set.
   */
  private void dialogChanged() {
    IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(
        new Path(getSourceFolder()));
    String className = getClassName();

    if (container.getProject() == null || container.getProject().getName() == null || container.getProject().getName().length() == 0) {
      updateStatus("The source folder of an existing project must be specified.");
      return;
    }
//    if (getPackageName().length() == 0) {
//      updateStatus("The package must be specified");
//      return;
//    }
    if (container != null && !container.isAccessible()) {
      updateStatus("Project must be writable");
      return;
    }
    if (className.length() == 0) {
      updateStatus("Class name must be specified");
      return;
    }
    if (className.replace('\\', '/').indexOf('/', 1) > 0) {
      updateStatus("Class name must be valid");
      return;
    }

    int dotLoc = className.lastIndexOf('.');
    if (dotLoc != -1) {
      String ext = className.substring(dotLoc + 1);
      if (ext.equalsIgnoreCase("java") == false) {
        updateStatus("File extension must be \"java\"");
        return;
      }
    }
    updateStatus(null);
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

  public String getSourceFolder() {
    return m_sourceFolderText.getText();
  }

  public String getXmlFile() {
    return m_xmlFilePath.getText();
  }

  public String getPackageName() {
    return m_packageNameText.getText();
  }

  public String getClassName() {
    return m_classNameText.getText();
  }

  public boolean containsAnnotation(String annotation) {
    Button b = m_annotations.get(annotation);
    return b.getSelection();
  }
}