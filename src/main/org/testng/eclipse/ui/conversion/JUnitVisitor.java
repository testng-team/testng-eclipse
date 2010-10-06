package org.testng.eclipse.ui.conversion;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.testng.internal.annotations.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

/**
 * This visitor stores all the interesting things in a JUnit class:
 * - JUnit imports
 * - "extends TestCase" declaration
 * - Methods that start with test
 * - setUp and tearDown
 * 
 * Created on Aug 8, 2005
 * @author cbeust
 */
public class JUnitVisitor extends ASTVisitor {
  private List m_testMethods = new ArrayList();
  private MethodDeclaration m_setUp = null;
  private MethodDeclaration m_tearDown = null;
  private MethodDeclaration m_suite = null;
  private SimpleType m_testCase = null;
  private List m_junitImports = new ArrayList();

  // The position and length of all the Assert references
  private Set<Expression> m_asserts = Sets.newHashSet();

  public boolean visit(ImportDeclaration id) {
    String name = id.getName().getFullyQualifiedName();
    if (name.indexOf("junit") != -1) {
      m_junitImports.add(id);
    }
    return super.visit(id);
  }

  public boolean visit(MethodDeclaration md) {
    String methodName = md.getName().getFullyQualifiedName();
    if (methodName.indexOf("test") != -1) {
      m_testMethods.add(md);
    }
    else if (methodName.equals("setUp")) {
      m_setUp = md;
    }
    else if (methodName.equals("tearDown")) {
      m_tearDown = md;
    }
    else if (methodName.equals("suite")) {
      m_suite = md;
    }
    
    return super.visit(md);
  }

  public boolean visit(TypeDeclaration td) {
    Type superClass = td.getSuperclassType();
    if (superClass instanceof SimpleType) {
      SimpleType st = (SimpleType) superClass;
      if (st.getName().getFullyQualifiedName().indexOf("TestCase") != -1) {
        m_testCase = st;
      }
    }
    return super.visit(td);
  }

  /**
   * Find occurrences of "Assert.xxx", which need to be replaced with "AssertJUnit.xxx".
   */
  public boolean visit(MethodInvocation node) {
    Expression exp = node.getExpression();
    if ("Assert".equals(exp.toString())) {
      m_asserts.add(exp);
      System.out.println("Found Assert at " + exp.getStartPosition());
    }
    return super.visit(node);
  }

  public Set<Expression> getAsserts() {
    return m_asserts;
  }

  private static void ppp(String s) {
    System.out.println("[JUnitVisitor] " + s);
    Assert.assertTrue(true);
  }

  public MethodDeclaration getSetUp() {
    return m_setUp;
  }

  public void setSetUp(MethodDeclaration setUp) {
    m_setUp = setUp;
  }

  public MethodDeclaration getSuite() {
    return m_suite;
  }

  public void setSuite(MethodDeclaration suite) {
    m_suite = suite;
  }

  public MethodDeclaration getTearDown() {
    return m_tearDown;
  }

  public void setTearDown(MethodDeclaration tearDown) {
    m_tearDown = tearDown;
  }

  public List getTestMethods() {
    return m_testMethods;
  }

  public void setTestMethods(List testMethods) {
    m_testMethods = testMethods;
  }

  public SimpleType getTestCase() {
    return m_testCase;
  }

  public List getJUnitImports() {
    return m_junitImports;
  }

}
