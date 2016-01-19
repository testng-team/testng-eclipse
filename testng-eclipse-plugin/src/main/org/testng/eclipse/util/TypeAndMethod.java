package org.testng.eclipse.util;

import java.util.Objects;

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
  
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof TypeAndMethod)) {
      return false;
    }
    TypeAndMethod o = (TypeAndMethod) obj;
    return Objects.equals(type, o.type) && Objects.equals(method, o.method);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(type, method);
  }
}
