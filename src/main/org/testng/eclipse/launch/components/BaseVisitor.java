package org.testng.eclipse.launch.components;

import org.testng.TestNG;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.signature.ASTMethodDescriptor;
import org.testng.eclipse.util.signature.IMethodDescriptor;
import org.testng.eclipse.util.signature.MethodDescriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class BaseVisitor extends ASTVisitor implements ITestContent {
  static final String JDK14_ANNOTATION = TestNG.JAVADOC_ANNOTATION_TYPE;
  static final String JDK15_ANNOTATION = TestNG.JDK_ANNOTATION_TYPE;
  
  // List<MethodDeclaration>
  private Set m_testMethods = new HashSet();
  private Set m_factoryMethods = new HashSet();
  
  private Map m_groups = new HashMap();
  protected boolean m_typeIsTest;
  protected String m_annotationType;
  
  public BaseVisitor(boolean f) {
    super(f);
  }
  
  public BaseVisitor() {
    super();
  }
  
  public boolean isTestNGClass() {
    return m_testMethods.size() > 0 || m_factoryMethods.size() > 0; 
  }
  
  public String getAnnotationType() {
    if(null != m_annotationType) {
      return m_annotationType;
    }
    
    return ((IMethodDescriptor) m_testMethods.iterator().next()).getAnnotationType();
  }
  
  public Set getTestMethods() {
    return m_testMethods;
  }
  
  public boolean hasTestMethods() {
    return m_typeIsTest || m_testMethods.size() > 0 || m_factoryMethods.size() > 0;
  }
    
  public Collection getGroups() {
    return m_groups.values();
  }

  protected void addGroup(String groupNames) {
    groupNames = Utils.stripDoubleQuotes(groupNames);
    final String[] names = Utils.split(groupNames, ",");
    for(int i = 0; i < names.length; i++) {
//      ppp("    FOUND GROUP:" + names[i]);
      m_groups.put(names[i], names[i]);
    }
  }
  
  protected void addTestMethod(MethodDeclaration md, String annotationType) {
    if(md.isConstructor()) {
      return; // constructors cannot be marked as test methods
    }
    IMethodDescriptor imd = new ASTMethodDescriptor(md, annotationType);
    m_testMethods.add(imd);
  }
  
  protected void addFactoryMethod(MethodDeclaration md, String annotationType) {
    IMethodDescriptor imd = new ASTMethodDescriptor(md, annotationType);
    m_factoryMethods.add(imd);
  }

  public static void ppp(String s) {
    System.out.println("[BaseVisitor] " + s);
  }

  public boolean isTestMethod(IMethod imethod) {
    if(m_typeIsTest) {
      return true;
    }

    return m_testMethods.contains(new MethodDescriptor(imethod));
  }

}
