package org.testng.eclipse.util.signature;


public interface IMethodDescriptor {
  public String getName();
  public String getReturnTypeSignature();
  public String getSignature();
  public String getFullSignature();
  public int getParameterCount();
  public String getAnnotationType();
}
