package org.testng.eclipse.refactoring;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

public class ConvertFromJUnitAction extends AbstractHandler
    implements IWorkbenchWindowActionDelegate { // extends AbstractHandler {

  private IWorkbenchWindow m_window;
  private ISelection m_selection;

  public void run(IAction action) {
    System.out.println("Run");
    run();
  }

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
    ISelectionService ss = window.getSelectionService();
    m_window = window;
  }

  public Object execute(ExecutionEvent event) throws ExecutionException {
    System.out.println("Execute");
    run();
    return null;
  }
  
  private void run() {

//    RefactoringProcessor processor = new RenamePropertyProcessor( info );
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    ConvertFromJUnitRefactoring ref = new ConvertFromJUnitRefactoring(m_window, page,
        null /* status */);
    ConvertFromJUnitWizard wizard = new ConvertFromJUnitWizard(ref, 0);
    RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
    try { 
      String titleForFailedChecks = ""; //$NON-NLS-1$ 
      op.run(PlatformUI.getWorkbench().getDisplay().getActiveShell(), titleForFailedChecks ); 
    } catch( InterruptedException irex ) { 
      // operation was cancelled 
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
    m_selection = selection;
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
