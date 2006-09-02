package org.testng.eclipse.launch.components;

import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.core.IMethod;

public interface ITestContent {
  public boolean isTestNGClass();
  
  /**
   * Returns a Set<IMethodDescriptor>.
   */
  Set getTestMethods();
  
  boolean hasTestMethods();

  Collection getGroups();
  
  boolean isTestMethod(IMethod imethod);

  String getAnnotationType();
}
