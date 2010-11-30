package org.testng.eclipse.refactoring;

import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.testng.eclipse.collections.Sets;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.Utils;
import org.testng.eclipse.util.Utils.JavaElement;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlPackage;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

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

  protected TestNGXmlPage() {
    super(NAME);
    setTitle(TITLE);
  }

  public void createControl(Composite p) {
    createUi(p);
    createModel();
    updateUi();
    addListeners();
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
    updateXmlSuite(m_xmlSuite);
    m_previewText.setText(m_xmlSuite.toXml());
  }

  private void createUi(Composite p) {
    Composite parent = SWTUtil.createGridContainer(p, 3);

    //
    // Path
    //
    Text xmlFile = SWTUtil.createPathBrowserText(parent, "Location:", null);
    List<JavaElement> elements = Utils.getSelectedJavaElements();
    if (elements.size() > 0) {
      xmlFile.setText(elements.get(0).getPath() + "/testng.xml");
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
    
    m_previewText = new Text(parent, SWT.BORDER);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd.horizontalSpan = 3;
    m_previewText.setLayoutData(gd);

    setControl(parent);
  }

  private void createModel() {
    m_selectedElements = Utils.getSelectedJavaElements();

    // Initialize m_classes
    for (JavaElement element : m_selectedElements) {
      List<IType> types = JDTUtil.findTypesInPackage(element.getProject(), element.getPackageName());
      if (element.getClassName() != null) {
        XmlClass c = new XmlClass(element.getPackageName() + "." + element.getClassName());
        m_classes.add(c);
      } else {
        for (IType type : types) {
          m_classes.add(new XmlClass(type.getFullyQualifiedName()));
        }
      }
    }

    // Initialize m_packages
    Set<String> packageSet = Sets.newHashSet();
    for (JavaElement element : m_selectedElements) {
      packageSet.add(element.getPackageName());
    }
    for (String p : packageSet) {
      XmlPackage pkg = new XmlPackage();
      pkg.setName(p);
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
    XmlTest test = suite.getTests().get(0);
    test.getXmlClasses().clear();
    test.getXmlPackages().clear();
    if (m_selectionCombo.getSelectionIndex() == 0) {
      test.getXmlClasses().addAll(m_classes);
    } else {
      test.getXmlPackages().addAll(m_packages);
    }
  }

  /**
   * @return whether the selected elements are all classeds.
   */
  private boolean selectionContainsOnlyClasses() {
    for (JavaElement je : m_selectedElements) {
      if (je.compilationUnit == null) return false;
    }
    return true;
  }

  private Text addTextLabel(Composite parent, String text) {
    Text result = SWTUtil.createLabelText(parent, "Suite name:", null);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
    gd.horizontalSpan = 2;
    result.setLayoutData(gd);

    return result;
  }
}
