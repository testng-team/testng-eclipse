package org.testng.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ConvertFromJUnitRefactoringDescriptor extends RefactoringDescriptor {

  public ConvertFromJUnitRefactoringDescriptor(String refactoringId,
      String project, String string, String string2, int structuralChange)
  {
    super(refactoringId, project, string, string2, structuralChange);
  }

  @Override
  public Refactoring createRefactoring(RefactoringStatus status)
      throws CoreException
  {
    return new ConvertFromJUnitRefactoring(null /* page */, status);
  }

}
