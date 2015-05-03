package org.testng.eclipse.refactoring;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

public class ConvertFromJUnitAction extends AbstractHandler {

  @Override
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

    if (shell == null) {
      System.out.println("Can't get a shell, is the Eclipse window active?");
      return;
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
}
