package org.testng.eclipse.launch.tester;

import org.testng.eclipse.util.TestSearchEngine;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jdt.core.IJavaElement;


public class JavaTypeExtender extends PropertyTester {

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if(!(receiver instanceof IJavaElement)) {
      return false;
    }
    
    IJavaElement javaElement = (IJavaElement) receiver;
    int javaElementType = javaElement.getElementType();
    
    if(IJavaElement.JAVA_PROJECT == javaElementType
        || IJavaElement.PACKAGE_FRAGMENT_ROOT == javaElementType) {
      return false;
    }
    
    if(IJavaElement.PACKAGE_FRAGMENT == javaElementType) {
      return true;
    }
    
    if(IJavaElement.COMPILATION_UNIT == javaElementType
        || IJavaElement.TYPE == javaElementType
        || IJavaElement.METHOD == javaElementType) {
      return TestSearchEngine.isTest(javaElement);
    }
    
    return false;
  }
}
