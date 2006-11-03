package org.testng.eclipse.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.Utils;

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
    final String fileConfName= suiteFile.getProjectRelativePath().toString().replace('/', '.');
    ILaunchConfiguration config= ConfigurationHelper.findConfiguration(getLaunchManager(), suiteFile.getProject(), fileConfName);
    if(null == config) {
      ILaunchConfigurationWorkingCopy wCopy = 
        ConfigurationHelper.createBasicConfiguration(getLaunchManager(),
                                                     suiteFile.getProject(),
                                                     fileConfName);
      wCopy.setAttribute(TestNGLaunchConfigurationConstants.SUITE_TEST_LIST,
                        Utils.stringToList(suiteFile.getProjectRelativePath().toOSString()));
      wCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
                         TestNGLaunchConfigurationConstants.SUITE);
    
      try {
        config= wCopy.doSave();
      }
      catch(CoreException cex) {
        TestNGPlugin.log(cex);
      }
    }
    
    if(null != config) {
      launchConfiguration(config, mode);
    }
  }
  
  protected void launchConfiguration(ILaunchConfiguration config, String mode) {
    if (config != null) {
      DebugUITools.launch(config, mode);
    }
  }
  
  /**
   * Returns the local java launch config type
   */
  protected ILaunchConfigurationType getJavaLaunchConfigType() {
    return getLaunchManager().getLaunchConfigurationType(TestNGLaunchConfigurationConstants.ID_TESTNG_APPLICATION);    
  }
  
  protected ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }
  
  private static void ppp(Object msg) {
//    System.out.println("[TestNGSuiteLaunchShortcut]: " + msg);
  }
}
