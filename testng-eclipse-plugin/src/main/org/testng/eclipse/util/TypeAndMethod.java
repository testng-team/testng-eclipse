package org.testng.eclipse.util;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;

public class TypeAndMethod {

  private final IType type;
  private final IMethod method;

  public TypeAndMethod(IType type, IMethod method) {
    this.type = type;
    this.method = method;
  }
  
  public IType getType() {
    return type;
  }
  
  public IMethod getMethod() {
    return method;
  }
}
