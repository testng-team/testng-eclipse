package org.testng.eclipse.launch;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.TypeParser;
import org.testng.eclipse.util.JDTUtil;


public class DebugEditorLaunchDelegate extends AbstractTestNGLaunchDelegate {

  protected String getLaunchMode() {
    return "debug";
  }

  protected String getCommandPrefix() {
    return "Debug as";
  }

  protected String getTestShortcut() {
    return "M3+M2+D N";
  }

  protected String getSuiteShortcut() {
    return "M3+M2+D G";
  }
}
