package org.testng.eclipse.refactoring;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.PlatformUI;

public class ConvertFromJUnitAction extends AbstractHandler {

  public void run(IAction action) {
    System.out.println("Run");
  }

  public Object execute(ExecutionEvent event) throws ExecutionException {
    System.out.println("Execute");

//    RefactoringProcessor processor = new RenamePropertyProcessor( info );
    ConvertFromJUnitRefactoring ref = new ConvertFromJUnitRefactoring(null /* status */);
    ConvertFromJUnitWizard wizard = new ConvertFromJUnitWizard(ref, 0);
    RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
    try { 
      String titleForFailedChecks = ""; //$NON-NLS-1$ 
      op.run(PlatformUI.getWorkbench().getDisplay().getActiveShell(), titleForFailedChecks ); 
    } catch( InterruptedException irex ) { 
      // operation was cancelled 
    }
    return null;
  }

//  protected ConvertFromJUnit3Action(IWorkbenchSite site) {
//    super(site);
//  }

//  public void run(IStructuredSelection selection) {
//    System.out.println("Converting");
//  }
//    try {
//      Assert.isTrue(RefactoringAvailabilityTester.isIntroduceIndirectionAvailable(selection));
//      Object first= selection.getFirstElement();
//      Assert.isTrue(first instanceof IMethod);
//      run((IMethod) first);
//    } catch (CoreException e) {
//      ExceptionHandler.handle(e, RefactoringMessages.IntroduceIndirectionAction_dialog_title, RefactoringMessages.IntroduceIndirectionAction_unknown_exception);
//    }
}
