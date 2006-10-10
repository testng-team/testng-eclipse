package org.testng.eclipse.launch;

import java.util.List;

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
import org.testng.remote.RemoteTestNG;

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
    ppp("launch " + suiteFile.getFullPath().toOSString() + " in " + mode + " mode");
    final String launchConfName = suiteFile.getFullPath().toOSString().replace('\\', '.').replace('/', '.');
//    ILaunchConfiguration conf = null; //findConfiguration(suiteFile, mode);
    
//    if(null == conf) {
      ILaunchConfigurationWorkingCopy wCopy = 
          ConfigurationHelper.createBasicConfiguration(getLaunchManager(),
                                                       suiteFile.getProject(),
                                                       "TestNG context suite");
      wCopy.setAttribute(TestNGLaunchConfigurationConstants.SUITE_TEST_LIST,
                        Utils.stringToList(suiteFile.getProjectRelativePath().toOSString()));
      wCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
                         TestNGLaunchConfigurationConstants.SUITE);
//      try {
//        conf = wCopy.doSave();
//      }
//      catch(CoreException ce) {
//        TestNGPlugin.log(ce);
//      }
//    }
    
    if(null != wCopy) {
      try {
        launchConfiguration(wCopy.doSave(), mode);
      }
      catch(CoreException ce) {
        TestNGPlugin.log(ce);
      }
    }
  }
  
  protected ILaunchConfiguration findConfiguration(IFile file, String mode) {
    ILaunchConfigurationType confType = getJavaLaunchConfigType();
    ILaunchConfiguration resultConf = null;
    try {
      ILaunchConfiguration[] availConfs = getLaunchManager().getLaunchConfigurations(confType);
      
      String projectName = file.getProject().getName();
      String suitePath = file.getFullPath().toOSString().replace('\\', '.').replace('/', '.');
      String main = RemoteTestNG.class.getName();
      
      for(int i = 0; i < availConfs.length; i++) {
        String confProjectName = ConfigurationHelper.getProjectName(availConfs[i]);
        String confMainName = ConfigurationHelper.getMain(availConfs[i]);
        
        if(projectName.equals(confProjectName) && main.equals(confMainName)) {
          List suiteList = ConfigurationHelper.getSuites(availConfs[i]);
          if(null != suiteList 
              && suiteList.size() == 1 
              && suitePath.equals(suiteList.get(0))) {
            if(null == resultConf) {
              resultConf = availConfs[i];
            }
            else {
              ppp("another conf matching found");
            }
          }
        }
      }
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }
    
    return resultConf;
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
