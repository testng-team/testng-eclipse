package org.testng.eclipse.refactoring;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.PreferenceStoreUtil.SuiteMethodTreatment;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.Utils;
import org.testng.eclipse.util.Utils.JavaElement;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlPackage;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

/**
 * The page in the refactoring wizard that lets the user configure the
 * generation of the testng.xml file.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class TestNGXmlPage extends UserInputWizardPage {
  private static final String NAME = "testng.xml generation";
  private static final String TITLE = "Generate testng.xml";
  private Text m_previewText;
  private XmlSuite m_xmlSuite;
  private Text m_suiteText;
  private Text m_testText;

  private final ModifyListener MODIFY_LISTENER = new ModifyListener() {
    @Override
    public void modifyText(ModifyEvent e) {
      updateUi();
    }
  };

  // Whether classes are selected by packages or by class names
  enum Selection {
    CLASSES("Classes"),
    PACKAGES("Packages");

    private String m_name;

    private Selection(String name) {
      m_name = name;
    }

    @Override
    public String toString() {
      return m_name;
    }
  };
  private Combo m_selectionCombo;
  private Set<XmlClass> m_classes = new HashSet<>();
  private Set<XmlPackage> m_packages = new HashSet<>();
  private Text m_xmlFile;
  private Button m_generateBox;
  private Combo m_parallelCombo;
  private Text m_threadCountText;

  // Code generation UI
  private Label m_codeGenerationBox;
  private Combo m_suiteMethodCombo;

  protected TestNGXmlPage() {
    super(NAME);
    setTitle(TITLE);
  }

  @Override
  public void createControl(Composite p) {
    p("createUI");
    createUi(p);
    p("createModel");
    createModel();
    p("updateUI");
    updateUi();
    p("addListeners");
    addListeners();
  }

  private void p(String string) {
    if (false) {
      System.out.println("[TestNGXmlPage] " + string);
    }
  }

  private void addListeners() {
    m_suiteText.addModifyListener(MODIFY_LISTENER);
    m_testText.addModifyListener(MODIFY_LISTENER);
    m_selectionCombo.addModifyListener(MODIFY_LISTENER);
    m_parallelCombo.addModifyListener(MODIFY_LISTENER);
    m_threadCountText.addModifyListener(MODIFY_LISTENER);
  }

  private String getDefaultSuiteName() {
    return "Suite";
  }

  private String getDefaultTestName() {
    return "Test";
  }

  private void updateUi() {
    m_xmlSuite.setName(m_suiteText.getText());
    m_xmlSuite.getTests().get(0).setName(m_testText.getText());
    m_xmlSuite.setParallel(m_parallelCombo.getItem(m_parallelCombo.getSelectionIndex()));
    Integer threadCount = null;
    try {
      threadCount = Integer.parseInt(m_threadCountText.getText());
      m_xmlSuite.setThreadCount(threadCount);
    } catch(NumberFormatException ex) {
      m_xmlSuite.setThreadCount(XmlSuite.DEFAULT_THREAD_COUNT);
    }
    updateXmlSuite(m_xmlSuite);
    m_previewText.setText(m_xmlSuite.toXml());
  }

  private void createUi(Composite wizardParent) {
    Composite control = new Composite(wizardParent, SWT.NONE);
    SWTUtil.createGridLayout(control, 1);
    control.setLayout(new GridLayout());
    control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    //
    // "Generate testng.xml" box
    //
    m_generateBox = new Button(control, SWT.CHECK);
    m_generateBox.setText("Generate testng.xml");
    m_generateBox.setSelection(true);

    final Group group = new Group(control, SWT.NONE);
    {
      group.setLayout(new GridLayout());
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
      group.setLayoutData(gd);
      group.setEnabled(true);
    }

    m_generateBox.addSelectionListener(new SelectionListener() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        group.setEnabled(((Button) e.getSource()).getSelection());
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
      }

    });

    Composite parent = SWTUtil.createGridContainer(group, 3);
    parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    //
    // Location
    //
    m_xmlFile = SWTUtil.createPathBrowserText(parent, "Location:", null);
    List<JavaElement> elements = Utils.getSelectedJavaElements();
    if (elements.size() > 0) {
      m_xmlFile.setText(elements.get(0).getProject().getPath() + "/testng.xml");
    }

    //
    // Suite/test name
    //
    m_suiteText = addTextLabel(parent, "Suite name:");
    m_suiteText.setText(getDefaultSuiteName());
    m_testText = addTextLabel(parent, "Test name:");
    m_testText.setText(getDefaultTestName());

    Composite horizontal = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout(6, true);
    horizontal.setLayout(layout);
    {
      GridData gd = new GridData();
      gd.horizontalSpan = 3;
      horizontal.setLayoutData(gd);
    }

    //
    // Selection combo
    //
    {
      Label l = new Label(horizontal, SWT.NONE);
      l.setText("Class selection:");
      m_selectionCombo = new Combo(horizontal, SWT.READ_ONLY);
      m_selectionCombo.add(Selection.CLASSES.toString());
      m_selectionCombo.add(Selection.PACKAGES.toString());
      m_selectionCombo.select(0);
    }


    //
    // Parallel mode
    //
    {
      Label l = new Label(horizontal, SWT.NONE);
      l.setText("Parallel mode:");
      m_parallelCombo = new Combo(horizontal, SWT.READ_ONLY);
      m_parallelCombo.add(XmlSuite.ParallelMode.NONE.toString());
      m_parallelCombo.add(XmlSuite.ParallelMode.METHODS.toString());
      m_parallelCombo.add(XmlSuite.ParallelMode.CLASSES.toString());
      m_parallelCombo.add(XmlSuite.ParallelMode.TESTS.toString());
      m_parallelCombo.select(0);
    }

    //
    // Thread count
    //
    {
      Label l = new Label(horizontal, SWT.NONE);
      l.setText("Thread count:");
      m_threadCountText = new Text(horizontal, SWT.BORDER);
    }

    //
    // Preview text
    //
    {
      Label previewLabelText = new Label(parent, SWT.NONE);
      previewLabelText.setText("Preview");
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
      gd.horizontalSpan = 3;
      previewLabelText.setLayoutData(gd);
    }

    {
      m_previewText = new Text(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
      gd.horizontalSpan = 3;
      m_previewText.setLayoutData(gd);
    }

    //
    // "Code generation" box
    //
    m_codeGenerationBox = new Label(control, SWT.CHECK);
    m_codeGenerationBox.setText("Code generation");

    final Group group2 = new Group(control, SWT.NONE);
    {
      RowLayout gl = new RowLayout();
//      GridLayout gl = new GridLayout(2, true /* same size  columns */);
      group2.setLayout(gl);
      GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
      group2.setLayoutData(gd);
      group2.setEnabled(true);
    }

    {
      Label l = new Label(group2, SWT.NONE);
      l.setText("suite() methods:");

      m_suiteMethodCombo = new Combo(group2, SWT.READ_ONLY);
      m_suiteMethodCombo.add("Remove");
      m_suiteMethodCombo.add("Comment out");
      m_suiteMethodCombo.add("Don't touch");
      SuiteMethodTreatment lastValue = TestNGPlugin.getPluginPreferenceStore().getSuiteMethodTreatement();
      m_suiteMethodCombo.select(lastValue.ordinal());
      m_suiteMethodCombo.addSelectionListener(new SelectionListener() {

        @Override
        public void widgetSelected(SelectionEvent e) {
          TestNGPlugin.getPluginPreferenceStore().storeSuiteMethodTreatement(
              m_suiteMethodCombo.getSelectionIndex());
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
        }

      });
    }

    setControl(control);
  }

  private void createModel() {
    //
    // Initialize m_classes
    //
    Set<String> packageSet = new HashSet<>();
    List<IType> types = Utils.findTypes(Utils.getSelectedJavaElements(), Utils.CONVERSION_FILTER);
    for (IType type : types) {
      String packageName = type.getPackageFragment().getElementName();
      String className = type.getElementName();
      if (className != null) {
        XmlClass c = new XmlClass(packageName + "." + className, false /* don't resolve */);
        p("Adding class " + c);
        m_classes.add(c);
        packageSet.add(packageName);
      } else {
        p("Adding type " + type);
        m_classes.add(new XmlClass(type.getFullyQualifiedName(), false /* don't resolve */));
        packageSet.add(packageName);
      }
    }
//    for (JavaElement element : m_selectedElements) {
//      if (element.getClassName() != null) {
//        XmlClass c = new XmlClass(element.getPackageName() + "." + element.getClassName(),
//            false /* don't resolve */);
//        p("Adding class " + c);
//        m_classes.add(c);
//        packageSet.add(element.getPackageName());
//      } else {
//        for (IType type : types) {
//          p("Adding type " + type);
//          m_classes.add(new XmlClass(type.getFullyQualifiedName(), false /* don't resolve */));
//          packageSet.add(type.getPackageFragment().getElementName());
//        }
//      }
//    }

    //
    // Initialize m_packages
    //
    for (String p : packageSet) {
      XmlPackage pkg = new XmlPackage();
      pkg.setName(p);
      p("Adding package " + p);
      m_packages.add(pkg);
    }

    m_xmlSuite = createXmlSuite();
  }

  private XmlSuite createXmlSuite() {
    XmlSuite result = new XmlSuite();
    result.setName(getDefaultSuiteName());
    XmlTest test = new XmlTest(result);
    test.setName(getDefaultTestName());

    updateXmlSuite(result);

    return result;
  }
  
  private void updateXmlSuite(XmlSuite suite) {
    p("Updating XML suite");
    XmlTest test = suite.getTests().get(0);
    test.getXmlClasses().clear();
    test.getXmlPackages().clear();
    if (m_selectionCombo.getSelectionIndex() == 0) {
      test.getXmlClasses().addAll(m_classes);
    } else {
      test.getXmlPackages().addAll(m_packages);
    }
    p("Done updating XML suite");
  }

  private Text addTextLabel(Composite parent, String text) {
    Text result = SWTUtil.createLabelText(parent, text, null);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
    gd.horizontalSpan = 2;
    result.setLayoutData(gd);

    return result;
  }

  /**
   * @return whether the user wants us to generate a testng.xml file.
   */
  public boolean generateXmlFile() {
    return m_generateBox.getSelection();
  }

  public void saveXmlFile() {
    String path = m_xmlFile.getText();
    if (! path.endsWith(".xml")) path = path + (path.endsWith("/") ? "testng.xml" : "/testng.xml");
    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
    ByteArrayInputStream is = new ByteArrayInputStream(m_xmlSuite.toXml().getBytes());
    try {
      org.testng.eclipse.ui.util.Utils.createFileWithDialog(
          PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
              file, is);
    } catch (CoreException e) {
      e.printStackTrace();
    }
  }

  public void finish() {
    saveXmlFile();
  }
}
