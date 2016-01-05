package org.testng.eclipse.buildpath;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.ClasspathVariableInitializer;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;


/**
 * This class/interface 
 */
public class TestNGHomeInitializer extends ClasspathVariableInitializer {

  /**
   * @see org.eclipse.jdt.core.ClasspathVariableInitializer#initialize(java.lang.String)
   */
  public void initialize(String variable) {
    try {
        Bundle bundle= Platform.getBundle(TestNGPlugin.PLUGIN_ID);
        if (bundle == null) {
          clearVariable();
          return;
        }
        
        URL local= null;
        try {
            local= FileLocator.toFileURL(bundle.getEntry("/")); //$NON-NLS-1$
        } 
        catch (IOException e) {
          clearVariable();
          return;
        }
        IPath location= Path.fromOSString(new File(local.getPath()).getAbsolutePath());
        if (null != location) {
          JavaCore.setClasspathVariable(TestNGPluginConstants.TESTNG_HOME, location, null);
        }
        else {
        }
    }
    catch(JavaModelException jmex) {
      clearVariable();
    }
  }

  private void clearVariable() {
    JavaCore.removeClasspathVariable(TestNGPluginConstants.TESTNG_HOME, null);
  }
}
