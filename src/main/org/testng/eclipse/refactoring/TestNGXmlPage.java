package org.testng.eclipse.refactoring;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.Utils;
import org.testng.eclipse.util.Utils.JavaElement;

import java.util.List;

public class TestNGXmlPage extends UserInputWizardPage {
  private static final String NAME = "testng.xml generation";
  private static final String TITLE = "Generate testng.xml";

  protected TestNGXmlPage() {
    super(NAME);
    setTitle(TITLE);
  }

  public void createControl(Composite parent) {
    Composite group = SWTUtil.createGridContainer(parent, 3);

    //
    // Path
    //
    Text xmlFile = SWTUtil.createPathBrowserText(group, "Location", null);
    List<JavaElement> elements = Utils.getSelectedJavaElements();
    if (elements.size() > 0) {
      xmlFile.setText(elements.get(0).project.getPath().toOSString() + "/testng.xml");
    }

    //
    // Suite name
    //
    setControl(group);
  }

}
