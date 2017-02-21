package org.testng.eclipse.launch.components;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.signature.ASTMethodDescriptor;
import org.testng.eclipse.util.signature.IMethodDescriptor;
import org.testng.eclipse.util.signature.MethodDescriptor;

public class BaseVisitor extends ASTVisitor implements ITestContent {
  // List<MethodDeclaration>
  private Set<IMethodDescriptor> m_testMethods = new HashSet<>();
  private Set<IMethodDescriptor> m_factoryMethods = new HashSet<>();
  private Set<String> m_groups = new HashSet<>();
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
    
    return m_testMethods.iterator().next().getAnnotationType();
  }
  
  public Set<IMethodDescriptor> getTestMethods() {
    return m_testMethods;
  }
  
  public boolean hasTestMethods() {
    return m_typeIsTest || m_testMethods.size() > 0 || m_factoryMethods.size() > 0;
  }
    
  public Collection<String> getGroups() {
    return m_groups;
  }

  protected void addGroup(String groupNames) {
    groupNames = Utils.stripDoubleQuotes(groupNames);
    final String[] names = Utils.split(groupNames, ",");
    for(String name : names) {
//      ppp("    FOUND GROUP:" + names[i]);
      m_groups.add(name);
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
