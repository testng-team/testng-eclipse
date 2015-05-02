package org.testng.eclipse.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.testng.eclipse.util.LaunchUtil;

/**
 * Suite contextual launcher.
 * 
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class TestNGSuiteLaunchShortcut implements ILaunchShortcut {

  public void launch(ISelection selection, String mode) {
    if(selection instanceof StructuredSelection) {
      run((IFile) ((StructuredSelection) selection).getFirstElement(), mode);
    }
  }

  /**
   * FIXME: not supported yet
   * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
   */
  public void launch(IEditorPart editor, String mode) {
    IEditorInput input = editor.getEditorInput();
    IFile suiteFile = (IFile) input.getAdapter(IFile.class);

    if(null != suiteFile) {
      run(suiteFile, mode);
    } 
  }

  protected void run(IFile suiteFile, String mode) {
    LaunchUtil.launchSuiteConfiguration(suiteFile, mode);
  }
}
