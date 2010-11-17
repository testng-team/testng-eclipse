package org.testng.eclipse.refactoring;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

public class ConvertFromJUnitAction extends AbstractHandler {

  private ISelection m_selection;

  public void dispose() {
  }

  public Object execute(ExecutionEvent event) throws ExecutionException {
    Event ev = (Event) event.getTrigger();
    run(ev.display.getActiveShell());
    return null;
  }
  
  private void run(Shell shell) {
    if (shell == null) {
      // We won't have an active shell if the user right-clicked on an item without
      // selecting it first
      shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
    }

//    RefactoringProcessor processor = new RenamePropertyProcessor( info );
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    ConvertFromJUnitRefactoring ref = new ConvertFromJUnitRefactoring(page, null /* status */);
    ConvertFromJUnitWizard wizard = new ConvertFromJUnitWizard(ref, 0);
    RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
    try { 
      String titleForFailedChecks = ""; //$NON-NLS-1$ 
      op.run(shell, titleForFailedChecks);
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
