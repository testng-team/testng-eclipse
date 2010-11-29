package org.testng.eclipse.refactoring;

import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class TestNGXmlPage extends UserInputWizardPage {

  protected TestNGXmlPage() {
    super("Generate testng.xml");
  }

  public void createControl(Composite parent) {
    Text l = new Text(parent, SWT.NONE);
    l.setText("Generate testng.xml");
    setControl(l);
  }

}
