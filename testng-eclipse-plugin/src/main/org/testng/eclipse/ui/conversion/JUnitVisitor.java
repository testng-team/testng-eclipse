package org.testng.eclipse.ui.conversion;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.testng.AssertJUnit;

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
public class JUnitVisitor extends Visitor {
  private List<MethodDeclaration> m_testMethods = new ArrayList<>();
  private List<MethodDeclaration> m_disabledTestMethods = new ArrayList<>();
  private List<MethodDeclaration> m_beforeMethods = new ArrayList<>();
  private List<MethodDeclaration> m_afterMethods = new ArrayList<>();
  private List<MethodDeclaration> m_beforeClasses = new ArrayList<>();
  private List<MethodDeclaration> m_afterClasses = new ArrayList<>();
  private MethodDeclaration m_suite = null;

  // Parent classes
  private SimpleType m_testCase = null;
  private boolean m_isTestSuite = false;

  // The JUnit imports found on this class
  private List<ImportDeclaration> m_junitImports = new ArrayList<>();

  // Static imports for assert methods
  private Set<String> m_assertStaticImports = new HashSet<>();

  // List of all the methods that have @Test(expected) or @Test(timeout)
  private Map<MemberValuePair, String> m_testsWithExpected = new HashMap<>();

  // The position and length of all the Assert references that are not statically
  // imported
  private Set<MethodInvocation> m_asserts = new HashSet<>();

  // The position and length of all the fail() calls
  private Set<MethodInvocation> m_fails = new HashSet<>();

  // True if there are test methods (if they are annotated with @Test, they won't
  // show up in m_testMethods).
  private boolean m_hasTestMethods = false;

  // Nodes that need to be removed by the refactoring
  private List<ASTNode> m_nodesToRemove = new ArrayList<>();
  private SuperConstructorInvocation m_superConstructorInvocation;
  private String m_className;
  private SingleMemberAnnotation m_runWithParameterized;
  private MethodDeclaration m_parametersMethod;
  private TypeDeclaration m_type;
  private boolean m_hasDefaultConstructor = false;
  private Map<MethodDeclaration, Annotation> m_ignoredMethods = new HashMap<>();

  // The list of methods that are present on JUnit's Assert class
  private static Set<String> m_assertMethods = new HashSet<>();

  static {
    for (Method m : Assert.class.getDeclaredMethods()) {
      m_assertMethods.add(m.getName());
    }
    // Also add a few methods from the JUnit4 Assert class
    m_assertMethods.add("assertArrayEquals");
  }

  @Override
  public boolean visit(ImportDeclaration id) {
    Name simpleName = id.getName();
    String name = simpleName.getFullyQualifiedName();
    if (name.indexOf("junit") != -1) {
      int ind = simpleName.toString().indexOf("assert");
      if (id.isStatic() && ind > 0) {
        m_assertStaticImports.add(simpleName.toString().substring(ind));
      }
      m_junitImports.add(id);
    }
    return super.visit(id);
  }

  @Override
  public boolean visit(SuperMethodInvocation smi) {
    String name = smi.getName().toString();
    // Only remove the call to super if the class extends TestCase directly
    if (m_testCase != null && ("setUp".equals(name) || "tearDown".equals(name))) {
      m_nodesToRemove.add(smi.getParent());
    }
    return super.visit(smi);
  }

  /**
   * Remember if we find a constructor that calls super(String).
   */
  @Override
  public boolean visit(SuperConstructorInvocation sci) {
    // Only remove the call to super if the class extends TestCase directly
    if (m_testCase != null) {
      List args = sci.arguments();
      if (args.size() == 1) {
        Expression arg = (Expression) args.get(0);
        ITypeBinding binding = arg.resolveTypeBinding();
        if (binding != null && String.class.getName().equals(binding.getBinaryName())) {
          m_superConstructorInvocation = sci;
        }
      }
    }

    return super.visit(sci);
  }

  public SuperConstructorInvocation getSuperConstructorInvocation() {
    return m_superConstructorInvocation;
  }

  @Override
  public boolean visit(MethodDeclaration md) {
    String methodName = md.getName().getFullyQualifiedName();

    if (methodName.equals("setUp") || hasAnnotation(md, "Before")) {
      m_beforeMethods.add(md);
    } else if (methodName.equals("tearDown") || hasAnnotation(md, "After")) {
      m_afterMethods.add(md);
    } else if (methodName.equals("suite")) {
      m_suite = md;
    } else if (methodName.equals(m_type.getName().toString())) {
      // A constructor
      if (md.parameters().size() == 0) {
        m_hasDefaultConstructor = true;
      }
    } else if (hasAnnotation(md, "Parameters")) {
      m_parametersMethod = md;
    } else if (hasAnnotation(md, "BeforeClass")) {
      m_beforeClasses.add(md);
    } else if (hasAnnotation(md, "AfterClass")) {
      m_afterClasses.add(md);
    } else if (hasAnnotation(md, "Ignore")) {
      m_ignoredMethods.put(md, getAnnotation(md, "Ignore"));
    } else if (! hasAnnotation(md, "Test")) {
      // Public methods that start with "test" are tests.
      // Methods that start with "_test" or private test methods that start with "test" are disabled
      boolean isPrivate = (md.getModifiers() & Modifier.PRIVATE) != 0;
      if (methodName.startsWith("test") && ! isPrivate) {
        m_testMethods.add(md);
      } else if (methodName.startsWith("_test") || (methodName.startsWith("test") && isPrivate)) {
        m_disabledTestMethods.add(md);
      }
    }  else if (hasAnnotation(md, "Test")) {
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
    
    return super.visit(md);
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

  /**
   * Record whether this type declaration is a TestCase or a TestSuite.
   */
  @Override
  public boolean visit(TypeDeclaration td) {
    m_className = td.getName().toString();
    m_type = td;

    //
    // Is this class annotated with @RunWith(Parameterized.class)?
    //
    List<IExtendedModifier> modifiers = td.modifiers();
    for (IExtendedModifier m : modifiers) {
      if (m.isAnnotation() && m instanceof SingleMemberAnnotation) {
        SingleMemberAnnotation a = (SingleMemberAnnotation) m;
        if ("RunWith".equals(a.getTypeName().toString()) &&
            "Parameterized.class".equals(a.getValue().toString())) {
          m_runWithParameterized = a;
        }
      }
    }

    //
    // Is the class a direct subclass of TestCase?
    //
    Type superClass = td.getSuperclassType();
    if (superClass instanceof SimpleType) {
      SimpleType st = (SimpleType) superClass;
      if ("TestCase".equals(st.getName().getFullyQualifiedName())) {
        m_testCase = st;
      }
    }

    // Is the class a subclass of TestSuite?
    if (superClass != null) {
      ITypeBinding binding = superClass.resolveBinding();
      while (binding != null) {
        if ("TestSuite".equals(binding.getName())) {
          m_isTestSuite = true;
          break;
        } else {
          binding = binding.getSuperclass();
        }
      }
    }

    return super.visit(td);
  }

  public SingleMemberAnnotation getRunWithParameterized() {
    return m_runWithParameterized;
  }

  public MethodDeclaration getParametersMethod() {
    return m_parametersMethod;
  }

  /**
   * Find occurrences of "Assert.xxx", which need to be replaced with "AssertJUnit.xxx".
   */
  @Override
  public boolean visit(MethodInvocation node) {
    Expression exp = node.getExpression();
    String method = node.getName().toString();

    if ((exp != null && "Assert".equals(exp.toString())) || method.startsWith("assert")) {
      // Method prefixed with "Assert."
      if (belongsToAssertJUnit(node) &&
          ! m_assertStaticImports.contains(node.getName().toString())) {
        m_asserts.add(node);
      }
    } else if ("fail".equals(method)) {
      // assert or fail not prefixed with "Assert."
      if (belongsToAssertJUnit(node)) m_fails.add(node);
    }
    return super.visit(node);
  }

  // Class internal names, according to
  // http://download.oracle.com/javase/1.4.2/docs/api/java/lang/Class.html#getName()
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
    if (! m_assertMethods.contains(method.getName().toString())) return false;

    List<Expression> arguments = method.arguments();
    List<Class> types = new ArrayList<>();
    for (Expression e : arguments) {
      ITypeBinding binding = e.resolveTypeBinding();
      // Early abort if a binding fails
      if (binding == null) {
        return true;
      }
      Class c = bindingToClass(binding);

      types.add(c);
    }
    boolean result = false;

    adjustForOverloading(types);

    // We need to correct types[1] and types[2] so they match here, in order to
    // emulate type conversions (for example, (int, long) should become (long, long)

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

    if (! result && (arguments.size() == 2 || arguments.size() == 3)) {
      // An assert with two or three parameters will match assertTrue(Object, Object)
      result = true;
    }
    return result;
  }

  /**
   * Modify the list of parameter types to try to match how the compiler will
   * resolve the type conversion. For example, an invocation of assert(..., int, long)
   * will probably end up calling assert(..., long, long).
   */
  private void adjustForOverloading(List<Class> types) {
    if (types.size() > 2) {
      Class t2 = types.get(1);
      Class t3 = types.get(2);
      if ((t2 == long.class && t3 == int.class) || (t3 == long.class && t2 == int.class)) {
        types.set(1, long.class);
        types.set(2, long.class);
      }
    }
  }

  /**
   * Use heuristics to try to find the right class for this binding.
   */
  private Class bindingToClass(ITypeBinding binding) {
    Class result = getBinaryClassName(binding.getBinaryName());
    if (result == null) {
      try {
        result = Class.forName(binding.getQualifiedName());
      } catch (ClassNotFoundException e) {
        // ignore
      }
    }

    if (result == null) result = Object.class;
    return result;
  }

  public Set<MethodInvocation> getAsserts() {
    return m_asserts;
  }

  private static void ppp(String s) {
    System.out.println("[JUnitVisitor] " + s);
    Assert.assertTrue(true);
  }

  public Collection<MethodDeclaration> getBeforeMethods() {
    return m_beforeMethods;
  }

  public Collection<MethodDeclaration> getAfterMethods() {
    return m_afterMethods;
  }

  public Collection<MethodDeclaration> getBeforeClasses() {
    return m_beforeClasses;
  }

  public Collection<MethodDeclaration> getAfterClasses() {
    return m_afterClasses;
  }

  public MethodDeclaration getSuite() {
    return m_suite;
  }

  public void setSuite(MethodDeclaration suite) {
    m_suite = suite;
  }

  public Collection<MethodDeclaration> getTestMethods() {
    return m_testMethods;
  }

  public Collection<MethodDeclaration> getDisabledTestMethods() {
    return m_disabledTestMethods;
  }

  public boolean hasTestMethods() {
    return m_hasTestMethods || m_testMethods.size() > 0 || m_disabledTestMethods.size() > 0;
  }

  public void setTestMethods(List<MethodDeclaration> testMethods) {
    m_testMethods = testMethods;
  }

  public SimpleType getTestCase() {
    return m_testCase;
  }

  public List<ImportDeclaration> getJUnitImports() {
    return m_junitImports;
  }

  public Set<String> getStaticImports() {
    return m_assertStaticImports;
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

  @Override
  public String toString() {
    return "[JUnitVisitor for class " + m_className + "]";
  }

  public List<ASTNode> getNodesToRemove() {
    return m_nodesToRemove;
  }

  /**
   * @return whether this file needs to be changed in order to be converted to TestNG.
   */
  public boolean needsConversion() {
    if (m_isTestSuite) {
      return false;
    }

    if (m_hasTestMethods || getTestMethods().size() > 0 || getDisabledTestMethods().size() > 0 ||
        getBeforeMethods().size() > 0 || getAfterMethods().size() > 0 || m_suite != null) {
      return true;
    }

    return false;
  }

  public TypeDeclaration getType() {
    return m_type;
  }

  public boolean hasDefaultConstructor() {
    return m_hasDefaultConstructor;
  }

  public Map<MethodDeclaration, Annotation> getIgnoredMethods() {
    return m_ignoredMethods;
  }
}
