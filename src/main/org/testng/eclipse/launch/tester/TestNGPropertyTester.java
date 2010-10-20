package org.testng.eclipse.launch.tester;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;

public class TestNGPropertyTester extends PropertyTester {
  private JavaTypeExtender m_typeExtender= new JavaTypeExtender();
  private FileExtender m_fileExtender= new FileExtender();
  
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    boolean result = false;
    if (!(receiver instanceof IAdaptable)) {
      throw new IllegalArgumentException("Element must be of type 'IAdaptable', is " + receiver == null ? "null" : receiver.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$
    }
    if (!"isTest".equals(property) && !"isSuite".equals(property)) {
      throw new IllegalArgumentException("Unknown test property '" + property +"'");
    }

    if("isTest".equals(property)) {
      result = isTestClass(receiver, property, args, expectedValue);
    }
    else {
      result = isTestSuite(receiver, property, args, expectedValue);
    }

    return true;
  }

  private boolean isTestClass(Object receiver, String property, Object[] args, Object expectedValue) {
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
  
  private boolean isTestSuite(Object receiver, String property, Object[] args, Object expectedValue) {
    if(receiver instanceof IFile) {
      return m_fileExtender.test(receiver, property, args, expectedValue); 
    }
    else {
      IAdaptable adaptable= (IAdaptable) receiver;
      IFile file= (IFile) adaptable.getAdapter(IFile.class);
      if(null != file) {
        return m_fileExtender.test(file, property, args, expectedValue);
      }
    }
    return false;
  }
}
