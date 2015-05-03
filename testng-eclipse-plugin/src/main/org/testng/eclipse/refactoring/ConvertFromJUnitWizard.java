package org.testng.eclipse.refactoring;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class ConvertFromJUnitWizard extends RefactoringWizard {

  private TestNGXmlPage m_xmlPage;

  public ConvertFromJUnitWizard(Refactoring refactoring, int flags) {
    super(refactoring, flags);
  }

  @Override
  protected void addUserInputPages() {
    m_xmlPage = new TestNGXmlPage();
    addPage(m_xmlPage);
  }

  @Override
  public boolean performFinish() {
    if (m_xmlPage.generateXmlFile()) {
      m_xmlPage.finish();
    }

    return super.performFinish();
  }
}
