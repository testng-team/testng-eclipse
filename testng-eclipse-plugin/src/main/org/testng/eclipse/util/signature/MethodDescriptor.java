package org.testng.eclipse.util.signature;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;


public class MethodDescriptor implements IMethodDescriptor {
  private IMethod m_method;
  
  public MethodDescriptor(IMethod method) {
    m_method = method;
  }

  public String getName() {
    return m_method.getElementName();
  }

  public String getReturnTypeSignature() {
    try {
      return m_method.getReturnType();
    }
    catch(JavaModelException jme) {
      ;
    }
    
    return "";
  }

  public String getSignature() {
    try {
      return m_method.getSignature();
    }
    catch(JavaModelException jme) {
      ;
    }
    
    return "";
  }

  public String getFullSignature() {
    return getName() + getSignature();
  }

  public int getParameterCount() {
    return m_method.getNumberOfParameters();
  }

  
  public String getAnnotationType() {
    return "";
  }

  /**
   * Override hashCode.
   *
   * @return the Objects hashcode.
   */
  public int hashCode() {
    int hashCode = 1;
    hashCode = 31 * hashCode + getFullSignature().hashCode();
    
    return hashCode;
  }

  /**
   * Returns <code>true</code> if this <code>MethodDescriptor</code> is the same as the o argument.
   *
   * @return <code>true</code> if this <code>MethodDescriptor</code> is the same as the o argument.
   */
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if (o == null || !(o instanceof IMethodDescriptor)) {
      return false;
    }

    IMethodDescriptor castedObj = (IMethodDescriptor) o;
    return getFullSignature().equals(castedObj.getFullSignature());
  }
  
  
}
