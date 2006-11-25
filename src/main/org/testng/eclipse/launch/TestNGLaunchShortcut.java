package org.testng.eclipse.launch;

import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.testng.eclipse.util.LaunchUtil;

/**
 * Right-click launcher.
 * 
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class TestNGLaunchShortcut implements ILaunchShortcut {

  public void launch(ISelection selection, String mode) {
    if(selection instanceof StructuredSelection) {
      run((IJavaElement) ((StructuredSelection) selection).getFirstElement(), mode);
    }
  }

  public void launch(IEditorPart editor, String mode) {
  }

  protected void run(IJavaElement ije, String mode) {
    IJavaProject ijp = ije.getJavaProject();

    switch(ije.getElementType()) {
      case IJavaElement.PACKAGE_FRAGMENT:
      {
        LaunchUtil.launchPackageConfiguration(ijp, (IPackageFragment) ije, mode);
        
        return;
      }
      
      case IJavaElement.COMPILATION_UNIT:
      {
        LaunchUtil.launchCompilationUnitConfiguration(ijp, (ICompilationUnit) ije, mode); 

        return;
      }
      
      case IJavaElement.TYPE:
      {
        LaunchUtil.launchTypeConfiguration(ijp, (IType) ije, mode);
        
        return;
      }
      
      case IJavaElement.METHOD:
      {
        LaunchUtil.launchMethodConfiguration(ijp, (IMethod) ije, null /*complianceLevel*/, mode); 
        
        return;
      }
      
      default:
        return;
    }    
  }

  /*protected void launchConfiguration(ILaunchConfiguration config, String mode) {
    if(null != config) {
      DebugUITools.launch(config, mode);
    }
  }*/
  
  /*protected ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }*/  
}
