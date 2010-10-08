package org.testng.eclipse.ui.conversion;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.testng.collections.Lists;
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
  private List<MethodDeclaration> m_testMethods = Lists.newArrayList();
  private MethodDeclaration m_setUp = null;
  private MethodDeclaration m_tearDown = null;
  private MethodDeclaration m_suite = null;
  private SimpleType m_testCase = null;
  private List<ImportDeclaration> m_junitImports = new ArrayList();

  // The position and length of all the Assert references
  private Set<Expression> m_asserts = Sets.newHashSet();

  // The position and length of all the fail() calls
  private Set<MethodInvocation> m_fails = Sets.newHashSet();

  // True if there are test methods (if they are annotated with @Test, they won't
  // show up in m_testMethods).
  private boolean m_hasTestMethods = false;

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
      m_hasTestMethods = true;
      boolean hasTestAnnotation = false;
      List<IExtendedModifier> modifiers = md.modifiers();
      for (IExtendedModifier m : modifiers) {
        if (m.isAnnotation()) {
          Annotation a = (Annotation) m;
          if ("Test".equals(a.getTypeName().toString())) {
            hasTestAnnotation = true;
            break;
          }
        }
      }
      if (! hasTestAnnotation) m_testMethods.add(md);
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
    if (exp != null && "Assert".equals(exp.toString())) {
      m_asserts.add(exp);
    } else if ("fail".equals(node.getName().toString())) {
      m_fails.add(node);
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

  public List<MethodDeclaration> getTestMethods() {
    return m_testMethods;
  }

  public boolean hasTestMethods() {
    return m_hasTestMethods || m_testMethods.size() > 0;
  }

  public void setTestMethods(List testMethods) {
    m_testMethods = testMethods;
  }

  public SimpleType getTestCase() {
    return m_testCase;
  }

  public List<ImportDeclaration> getJUnitImports() {
    return m_junitImports;
  }

  public boolean hasAsserts() {
    return m_asserts.size() > 0;
  }

  public Set<MethodInvocation> getFails() {
    return m_fails;
  }

  public boolean hasFail() {
    return m_fails.size() > 0;
  }
}
