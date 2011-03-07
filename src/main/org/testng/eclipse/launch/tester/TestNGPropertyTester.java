package org.testng.eclipse.launch.tester;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;

import java.util.HashSet;
import java.util.Set;

/**
 * The generic property tester used by TestNG. Supports various properties:
 * - isTest: TestNG class or test method
 * - isSuite: suite file (XML or YAML)
 * - isXmlSuite: suite file (XML only)
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class TestNGPropertyTester extends PropertyTester {
  private static final Set<String> PROPERTIES = new HashSet<String>() {{
    add("isTest");
    add("isSuite");
    add("isXmlSuite");
  }};
  private static final boolean VERBOSE = false;

  private JavaTypeExtender m_typeExtender= new JavaTypeExtender();
  private FileExtender m_fileExtender= new FileExtender();

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    p("Testing property:" + property + " receiver:" + receiver);
    boolean result = false;
    if (!(receiver instanceof IAdaptable)) {
      throw new IllegalArgumentException("Element must be of type 'IAdaptable', is "
          + receiver == null ? "null" : receiver.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (! PROPERTIES.contains(property)) {
      throw new IllegalArgumentException("Unknown test property '" + property +"'");
    }

    if ("isTest".equals(property)) {
      result = isTestClass(receiver, property, args, expectedValue);
    }  else {
      boolean xmlOnly = "isXmlSuite".equals(property);
      result = isTestSuite(receiver, property, args, expectedValue, xmlOnly);
    }

    return result;
  }

  private boolean isTestClass(Object receiver, String property, Object[] args, 
      Object expectedValue) {
    if (receiver instanceof IJavaElement) {
      return m_typeExtender.test(receiver, property, args, expectedValue);
    } 
    else {
      IAdaptable adaptable= (IAdaptable) receiver;
      IJavaElement element= (IJavaElement) adaptable.getAdapter(IJavaElement.class);
      if(null != element) {
        return m_typeExtender.test(element, property, args, expectedValue);
      }      
    }
    return false;
  }
  
  private boolean isTestSuite(Object receiver, String property, Object[] args,
      Object expectedValue, boolean xmlOnly) {
    if(receiver instanceof IFile) {
      return m_fileExtender.test(receiver, property, args, expectedValue, xmlOnly); 
    }
    else {
      IAdaptable adaptable= (IAdaptable) receiver;
      IFile file= (IFile) adaptable.getAdapter(IFile.class);
      if(null != file) {
        return m_fileExtender.test(file, property, args, expectedValue, xmlOnly);
      }
    }
    return false;
  }

  private static void p(String s) {
    if (VERBOSE) {
      System.out.println("[TestNGPropertyTester] " + s);
    }
  }
}
