package org.testng.eclipse.refactoring;

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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.testng.eclipse.collections.Sets;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.Utils;
import org.testng.eclipse.util.Utils.JavaElement;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlPackage;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class TestNGXmlPage extends UserInputWizardPage {
  private static final String NAME = "testng.xml generation";
  private static final String TITLE = "Generate testng.xml";
  private Text m_previewText;
  private XmlSuite m_xmlSuite;
  private Text m_suiteText;
  private Text m_testText;

  private final ModifyListener MODIFY_LISTENER = new ModifyListener() {
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
  private List<JavaElement> m_selectedElements;
  private Set<XmlClass> m_classes = Sets.newHashSet();
  private Set<XmlPackage> m_packages = Sets.newHashSet();
  private Text m_xmlFile;
  private Button m_generateBox;

  protected TestNGXmlPage() {
    super(NAME);
    setTitle(TITLE);
  }

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
    updateXmlSuite(m_xmlSuite);
    m_previewText.setText(m_xmlSuite.toXml());
  }

  private void createUi(Composite wizardParent) {
    Composite control = new Composite(wizardParent, SWT.NONE);
    SWTUtil.createGridLayout(control, 1);
    control.setLayout(new GridLayout());
    control.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

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

      public void widgetSelected(SelectionEvent e) {
        group.setEnabled(((Button) e.getSource()).getSelection());
      }

      public void widgetDefaultSelected(SelectionEvent e) {
      }

    });

    Composite parent = SWTUtil.createGridContainer(group, 3);
    parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    //
    // Path
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

    //
    // Selection combo
    //
    {
      Label l = new Label(parent, SWT.NONE);
      l.setText("Class selection:");
      m_selectionCombo = new Combo(parent, SWT.READ_ONLY);
      m_selectionCombo.add(Selection.CLASSES.toString());
      m_selectionCombo.add(Selection.PACKAGES.toString());
      m_selectionCombo.select(0);
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

    setControl(control);
  }

  private void createModel() {
    m_selectedElements = Utils.getSelectedJavaElements();

    // Initialize m_classes
    Set<String> packageSet = Sets.newHashSet();
    List<IType> types = Utils.findTypes(m_selectedElements);
    for (JavaElement element : m_selectedElements) {
      if (element.getClassName() != null) {
        XmlClass c = new XmlClass(element.getPackageName() + "." + element.getClassName(),
            false /* don't resolve */);
        p("Adding class " + c);
        m_classes.add(c);
        packageSet.add(element.getPackageName());
      } else {
        for (IType type : types) {
          p("Adding type " + type);
          m_classes.add(new XmlClass(type.getFullyQualifiedName(), false /* don't resolve */));
          packageSet.add(type.getPackageFragment().getElementName());
        }
      }
    }

    // Initialize m_packages
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
      file.create(is, true /* force */, null /* progress monitor */);
    } catch (CoreException e) {
      e.printStackTrace();
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
}
