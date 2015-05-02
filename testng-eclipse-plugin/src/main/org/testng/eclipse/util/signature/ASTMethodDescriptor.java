package org.testng.eclipse.util.signature;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;



public class ASTMethodDescriptor implements IMethodDescriptor {
  private MethodDeclaration m_method;
  private String m_annotationType;
  
  public ASTMethodDescriptor(MethodDeclaration methodDeclaration, final String annotationType) {
    m_method = methodDeclaration;
    m_annotationType = annotationType;
  }
  
  public String getName() {
    return ((SimpleName) m_method.getName()).toString();
  }

  public String getReturnTypeSignature() {
    Type returnType = m_method.getReturnType2();
    if (null == returnType && m_method.getAST().apiLevel() < AST.JLS3) {
      returnType = m_method.getReturnType();
    }
    
    return TypeSignature.getSignature(returnType);
  }

  public String getSignature() {
    StringBuffer buf = new StringBuffer("(");
    
    List paramVars = m_method.parameters(); // List<SingleVariableDeclaration>
    for(int i = 0; i < paramVars.size(); i++) {
      buf.append(TypeSignature.getSignature(((SingleVariableDeclaration) paramVars.get(i)).getType()));
      
//      if(i + 1 < paramVars.size()) {
//        buf.append(",");
//      }
    }
    
    buf.append(")")
        .append(getReturnTypeSignature())
        ;
    
    return buf.toString();
  }

  public String getFullSignature() {
    return getName() + getSignature();
  }

  public int getParameterCount() {
    return m_method.parameters().size();
  }

  
  public String getAnnotationType() {
    return m_annotationType;
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
