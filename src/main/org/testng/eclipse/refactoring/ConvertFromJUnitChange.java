package org.testng.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.testng.eclipse.TestNGPlugin;

public class ConvertFromJUnitChange extends Change {

  public ConvertFromJUnitChange(IProgressMonitor pm) {
  }

  @Override
  public String getName() {
    return "Convert from JUnit change";
  }

  @Override
  public void initializeValidationData(IProgressMonitor pm) {
  }

  @Override
  public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
      OperationCanceledException
  {
    return RefactoringStatus.create(new Status(IStatus.OK, TestNGPlugin.getPluginId(),
        "Converted successfully from JUnit 3 to TestNG"));
  }

  @Override
  public Change perform(IProgressMonitor pm) throws CoreException {
    return null;
  }

  @Override
  public Object getModifiedElement() {
    return null;
  }

}
