package org.testng.eclipse.refactoring;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class ConvertFromJUnitWizard extends RefactoringWizard {

  public ConvertFromJUnitWizard(Refactoring refactoring, int flags) {
    super(refactoring, flags);
  }

  @Override
  protected void addUserInputPages() {
    addPage(new TestNGXmlPage());
  }

}
