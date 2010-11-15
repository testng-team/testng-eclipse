package org.testng.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ConvertFromJUnitRefactoring extends Refactoring {
  private RefactoringStatus m_status = new RefactoringStatus();

  public ConvertFromJUnitRefactoring(RefactoringStatus status) {
  }

  @Override
  public String getName() {
    return "Convert from JUnit";
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    return m_status;
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    return m_status;
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException,
      OperationCanceledException
  {
    return new ConvertFromJUnitChange(pm);
  }

}
