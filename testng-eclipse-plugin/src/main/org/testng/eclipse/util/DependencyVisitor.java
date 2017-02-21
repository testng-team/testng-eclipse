package org.testng.eclipse.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;

/**
 * An <code>ASTVisitor</code> that extracts the <tt>dependsOnMethods</tt> and
 * <tt>dependsOnGroups</tt>. Note: this class is no longer used, finding these
 * dependencies is now performed in GroupInfo. Using an ASTVisitor such as this
 * one takes longer since it reparses all the source files, while GroupInfo
 * simply walks all the types of the IJavaProject.
 */
public class DependencyVisitor extends ASTVisitor {
  private static final String ANNOTATION_PACKAGE = "org.testng.annotations.";
  private static final String TEST_ANNOTATION = "Test";
  private static final String TEST_ANNOTATION_FQN = ANNOTATION_PACKAGE + TEST_ANNOTATION;
  private static final String DEPENDS_ON_METHODS= "dependsOnMethods";
  private static final String DEPENDS_ON_GROUPS= "dependsOnGroups";
  
  List<String> m_dependsOnMethods = new ArrayList<>();
  List<String> m_dependsOnGroups = new ArrayList<>();

  @Override
  public boolean visit(NormalAnnotation annotation) {
    String typeName = annotation.getTypeName().getFullyQualifiedName();
    if(!TEST_ANNOTATION.equals(typeName) && !TEST_ANNOTATION_FQN.equals(typeName)) {
      return false;
    }

    List values= annotation.values();
    
    if(null != values && !values.isEmpty()) {
      for(int i= 0; i < values.size(); i++) {
        MemberValuePair pair= (MemberValuePair) values.get(i);
        String name = pair.getName().toString();
        if(DEPENDS_ON_METHODS.equals(name)) {
          m_dependsOnMethods.addAll(extractValues(pair.getValue()));
        }
        else if(DEPENDS_ON_GROUPS.equals(name)) {
          m_dependsOnGroups.addAll(extractValues(pair.getValue()));
        }
      }
    }
    
    return false;
  }

  public List<String> getDependsOnGroups() {
    return m_dependsOnGroups;
  }
  
  public List<String> getDependsOnMethods() {
    return m_dependsOnMethods;
  }

  private List<String> extractValues(Expression paramAttr) {
    List<String> values = new ArrayList<>();
    if(paramAttr instanceof ArrayInitializer) {
      List<StringLiteral> literals= ((ArrayInitializer) paramAttr).expressions();
//      List paramNames= new ArrayList(literals.size());
      for(int j= 0; j < literals.size(); j++) {
        StringLiteral str= literals.get(j);
        values.add(str.getLiteralValue());
      }
    } else if (paramAttr instanceof StringLiteral) {
      values.add(((StringLiteral) paramAttr).getLiteralValue());
    }

    return values;
  }
}