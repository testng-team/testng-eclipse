package org.testng.eclipse.launch.components;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.List;

/**
 * An AST visitor to collect all the groups defined in a compilation unit.
 * This visitor extends JavaDocVisitor so it's able to visit both annotations
 * and javadoc annotations, so maybe it should actually be renamed.
 * 
 * @author cbeust
 */
public class AnnotationVisitor extends BaseVisitor {
  // Deprecated, remove
  private static final String JDK15_ANNOTATION = "jdk15";

  @Override
  public boolean visit(MethodDeclaration node) {
    if(m_typeIsTest) {
      addTestMethod(node, JDK15_ANNOTATION);
//      return false; // no need to continue
    }
    
    return true;
  }

  @Override
  public boolean visit(MarkerAnnotation node) {
    ASTNode parent = node.getParent();
    if (isTestAnnotation(node.getTypeName().toString())) {
      if (parent instanceof MethodDeclaration) {
        addTestMethod((MethodDeclaration) parent, JDK15_ANNOTATION);
      }
      else if (parent instanceof TypeDeclaration) { // TESTNG-24
        m_typeIsTest = true;
        m_annotationType = JDK15_ANNOTATION;
      }
    }
    else if (isFactoryAnnotation(node.getTypeName().toString())) {
      if (parent instanceof MethodDeclaration) {
        m_annotationType = JDK15_ANNOTATION;
        addFactoryMethod((MethodDeclaration) parent, JDK15_ANNOTATION);
      }
    }
    
    return false;
  }
  
  @Override
  public boolean visit(NormalAnnotation node) {
    //
    // Test method?
    //
    if(isTestAnnotation(node.getTypeName().toString())) {
      ASTNode parent = node.getParent();
      if (parent instanceof MethodDeclaration) {
        addTestMethod((MethodDeclaration) parent, JDK15_ANNOTATION);
      } else if(parent instanceof TypeDeclaration) {
        m_typeIsTest = true;
        m_annotationType = JDK15_ANNOTATION;
      }
      
      @SuppressWarnings("unchecked")
      List<MemberValuePair> pairs = node.values();
      for (MemberValuePair mvp : pairs) {
        Name attribute = mvp.getName();
        String name = attribute.getFullyQualifiedName();
        if ("groups".equals(name)) {
          Expression value = mvp.getValue();
          // Array?
          if (value instanceof ArrayInitializer) {
            ArrayInitializer ai = (ArrayInitializer) value;
            @SuppressWarnings("unchecked")
            List<Expression> expressions = ai.expressions();
            for (Expression e : expressions) {
              Object v = e.resolveConstantExpressionValue();
              String g = v != null ? v.toString() : e.toString();
              addGroup(g);
            }
          }
          else if (value instanceof Name) {
            Object v = value.resolveConstantExpressionValue();
            String boundValue = v != null ? v.toString() : value.toString();
            addGroup(boundValue);
          }
          else if(value instanceof StringLiteral) {
            addGroup(value.toString());
          }
        }
      }
    }
    else if (isFactoryAnnotation(node.getTypeName().toString())) {
      if (node.getParent() instanceof MethodDeclaration) {
        m_annotationType = JDK15_ANNOTATION;
        addFactoryMethod((MethodDeclaration) node.getParent(), JDK15_ANNOTATION);
      }
    }

    return false;
  }
  
  public boolean isTestAnnotation(String annotation) {
    return "Test".equals(annotation) || annotation.endsWith(".Test");
  }
  
  public boolean isFactoryAnnotation(String annotation) {
    return "Factory".equals(annotation) || annotation.endsWith(".Factory");    
  }
  
  public static void ppp(String s) {
    System.out.println("[AnnotationVisitor] " + s);
  }

}
