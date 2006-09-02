package org.testng.eclipse.launch.tester;

import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.SuiteFileValidator;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;


/**
 * Property tester contributing the org.testng.eclipse.isSuite property to
 * file matching the criteria: *.xml, instanceof IFile and being a suite.
 * 
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class FileExtender extends PropertyTester {
  private static final String PROPERTY_IS_Test= "isSuite"; //$NON-NLS-1$

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if(!(receiver instanceof IFile)) {
      return false;
    }
    
    try {
      return SuiteFileValidator.isSuiteDefinition(((IFile) receiver).getContents());
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }

    return false;
  }
}
