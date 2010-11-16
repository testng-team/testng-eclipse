package org.testng.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.Utils;

public class ConvertFromJUnitChange extends Change {

  private IWorkbenchWindow m_window;
  private IWorkbenchPage m_page;

  public ConvertFromJUnitChange(IProgressMonitor pm, IWorkbenchWindow window,
      IWorkbenchPage page) {
    m_window = window;
    m_page = page;
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
    TestNGPlugin.asyncExec(new Runnable() {
      public void run() {
        Object selection = Utils.getSelectedProjectOrPackage(m_page);
      }
    });
//    CompositeChange change = new CompositeChange(getName(), changes.toArray(new Change[changes.size()])) {
//
//      @Override
//      public ChangeDescriptor getDescriptor() {
//        String project= fMethod.getJavaProject().getElementName();
//        String description= MessageFormat.format("Introduce indirection for ''{0}''", new Object[] { fMethod.getElementName()});
//        String methodLabel= JavaElementLabels.getTextLabel(fMethod, JavaElementLabels.ALL_FULLY_QUALIFIED);
//        String typeLabel= JavaElementLabels.getTextLabel(fType, JavaElementLabels.ALL_FULLY_QUALIFIED);
//        String comment= MessageFormat.format("Introduce indirection for ''{0}'' in ''{1}''", new Object[] { methodLabel, typeLabel});
//        Map<String, String> arguments= new HashMap<String, String>();
//        arguments.put(METHOD, fMethod.getHandleIdentifier());
//        arguments.put(TYPE, fType.getHandleIdentifier());
//        arguments.put(NAME, fName);
//        arguments.put(REFERENCES, Boolean.valueOf(fUpdateReferences).toString());
//        return new RefactoringChangeDescriptor(new IntroduceIndirectionDescriptor(project, description, comment, arguments));
//      }
//    };
//    return change;
    return null;
  }

  @Override
  public Object getModifiedElement() {
    return null;
  }

}
