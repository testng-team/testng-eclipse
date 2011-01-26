package org.testng.eclipse.refactoring;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.testng.eclipse.wizards.NewTestNGClassWizard;

/**
 * Create a TestNG class based on the class currently selected.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class CreateClassAction extends AbstractHandler {

  public Object execute(ExecutionEvent event) throws ExecutionException {
    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    WizardDialog dialog = new WizardDialog(shell, new NewTestNGClassWizard());
    dialog.create();
    dialog.open();

    return null;
  }

}
