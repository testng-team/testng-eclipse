package org.testng.eclipse.ui.conversion;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.testng.AssertJUnit;
import org.testng.collections.Lists;
import org.testng.eclipse.collections.Maps;
import org.testng.internal.annotations.Sets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

/**
 * This visitor stores all the interesting things in a JUnit class:
 * - JUnit imports
 * - "extends TestCase" declaration
 * - Methods that start with test
 * - setUp and tearDown
 * - ...
 * 
 * Created on Aug 8, 2005
 * @author Cedric Beust <cedric@beust.com>
 */
public class JUnitVisitor extends ASTVisitor {
  private List<MethodDeclaration> m_testMethods = Lists.newArrayList();
  private List<MethodDeclaration> m_beforeMethods = Lists.newArrayList();
  private List<MethodDeclaration> m_afterMethods = Lists.newArrayList();
  private MethodDeclaration m_suite = null;
  private SimpleType m_testCase = null;
  private List<ImportDeclaration> m_junitImports = Lists.newArrayList();
  // List of all the methods that have @Test(expected) or @Test(timeout)
  private Map<MemberValuePair, String> m_testsWithExpected = Maps.newHashMap();

  // The position and length of all the Assert references
  private Set<MethodInvocation> m_asserts = Sets.newHashSet();

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
    if (methodName.indexOf("test") != -1 && ! hasAnnotation(md, "Test")) {
      m_testMethods.add(md);
    }
    else if (hasAnnotation(md, "Test")) {
      m_hasTestMethods = true;  // to make sure we import org.testng.annotations.Test
      MemberValuePair mvp = getAttribute(md, "expected");
      if (mvp != null) {
        m_testsWithExpected.put(mvp, "expectedExceptions");
      }
      mvp = getAttribute(md, "timeout");
      if (mvp != null) {
        m_testsWithExpected.put(mvp, "timeOut");
      }
    }
    else if (methodName.equals("setUp") || hasAnnotation(md, "Before")) {
      m_beforeMethods.add(md);
    }
    else if (methodName.equals("tearDown") || hasAnnotation(md, "After")) {
      m_afterMethods.add(md);
    }
    else if (methodName.equals("suite")) {
      m_suite = md;
    }
    
    return super.visit(md);
  }

  /**
   * @return true if the given method is annotated with the annotation
   */
  private boolean hasAnnotation(MethodDeclaration md, String annotation) {
    @SuppressWarnings("unchecked")
    List<IExtendedModifier> modifiers = md.modifiers();
    for (IExtendedModifier m : modifiers) {
      if (m.isAnnotation()) {
        Annotation a = (Annotation) m;
        if (annotation.equals(a.getTypeName().toString())) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * @return true if the given method is annotated @Test(expected = ...)
   */
  private MemberValuePair getAttribute(MethodDeclaration md, String attribute) {
    @SuppressWarnings("unchecked")
    List<IExtendedModifier> modifiers = md.modifiers();
    for (IExtendedModifier m : modifiers) {
      if (m.isAnnotation()) {
        Annotation a = (Annotation) m;
        if ("Test".equals(a.getTypeName().toString()) && a instanceof NormalAnnotation) {
          NormalAnnotation na = (NormalAnnotation) a;
          for (Object o : na.values()) {
            MemberValuePair mvp = (MemberValuePair) o;
            if (mvp.getName().toString().equals(attribute)) return mvp;
          }
        }
      }
    }

    return null;
  }

  public boolean visit(TypeDeclaration td) {
    Type superClass = td.getSuperclassType();
    if (superClass instanceof SimpleType) {
      SimpleType st = (SimpleType) superClass;
      if (st.getName().getFullyQualifiedName().equals("TestCase")) {
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
    String method = node.getName().toString();

    if ((exp != null && "Assert".equals(exp.toString())) || method.startsWith("assert")) {
      // Method prefixed with "Assert."
      if (belongsToAssertJUnit(node)) m_asserts.add(node);
    } else if ("fail".equals(method)) {
      // assert or fail not prefixed with "Assert."
      if (belongsToAssertJUnit(node)) m_fails.add(node);
    }
    return super.visit(node);
  }

  private static Map<String, Class> BINARY_CLASS_NAMES = new HashMap<String, Class>() {{
    put("B", byte.class);
    put("C", char.class);
    put("D", double.class);
    put("F", float.class);
    put("I", int.class);
    put("J", long.class);
    put("L", Class.class);
    put("S", short.class);
    put("Z", boolean.class);
    put("[", Object[].class);
  }};

  private Class getBinaryClassName(String binaryName) {
    return BINARY_CLASS_NAMES.get(binaryName);
  }
  /**
   * @return true if this method is defined on the AssertJUnit class.
   */
  private boolean belongsToAssertJUnit(MethodInvocation method) {
    List<Expression> arguments = method.arguments();
    List<Class> types = Lists.newArrayList();
    for (Expression e : arguments) {
      ITypeBinding binding = e.resolveTypeBinding();
      Class c = bindingToClass(binding);
      // Early abort if a binding fails
      if (binding == null) return true;

      types.add(c);
    }
    boolean result = false;

    // Try to find a method with the exact signature. This can fail for a few reasons
    // (such as not being able to resolve the binary name), in which case I should try
    // to fall back on a simple name search
    try {
      Object m = AssertJUnit.class.getMethod(method.getName().getFullyQualifiedName(),
          types.toArray(new Class[types.size()]));
      result = true;
    } catch (SecurityException e1) {
//      e1.printStackTrace();
    } catch (NoSuchMethodException e1) {
//      e1.printStackTrace();
    }

    return result;
  }

  /**
   * If the key is found on the ITypeBinding, then it's probably of the type equal to its value. 
   */
  private static Map<String, Class> METHOD_TO_CLASS= new HashMap<String, Class>() {{
    put("indexOf", String.class);
  }};

  /**
   * Use heuristics to try to find the right class for this binding. This method
   * can fail in so many ways that I don't want to talk about it.
   */
  private Class bindingToClass(ITypeBinding binding) {
    Class result = getBinaryClassName(binding.getBinaryName());
    if (result == null) {
      // Binary name fail, use the methods on this type to try to identify its class.
      IMethodBinding[] methods = binding.getDeclaredMethods();
      for (IMethodBinding method : methods) {
        result = METHOD_TO_CLASS.get(method.getName());
        if (result != null) {
          return result;
        }
      }
    }

    return result;
  }

  public Set<MethodInvocation> getAsserts() {
    return m_asserts;
  }

  private static void ppp(String s) {
    System.out.println("[JUnitVisitor] " + s);
    Assert.assertTrue(true);
  }

  public List<MethodDeclaration> getBeforeMethods() {
    return m_beforeMethods;
  }

  public MethodDeclaration getSuite() {
    return m_suite;
  }

  public void setSuite(MethodDeclaration suite) {
    m_suite = suite;
  }

  public List<MethodDeclaration> getAfterMethods() {
    return m_afterMethods;
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

  /**
   * All the @Test annotated methods that have attributes that need to be replaced.
   */
  public Map<MemberValuePair, String> getTestsWithExpected() {
    return m_testsWithExpected;
  }
}
