package org.testng.eclipse.launch.components;

import org.eclipse.jdt.core.IMethod;
import org.testng.eclipse.util.signature.IMethodDescriptor;

import java.util.Collection;
import java.util.Set;

public interface ITestContent {
  public boolean isTestNGClass();
  
  /**
   * Returns a Set<IMethodDescriptor>.
   */
  Set<IMethodDescriptor> getTestMethods();
  
  boolean hasTestMethods();

  Collection<String> getGroups();
  
  boolean isTestMethod(IMethod imethod);

  String getAnnotationType();
}
