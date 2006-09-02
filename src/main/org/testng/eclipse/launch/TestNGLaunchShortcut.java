package org.testng.eclipse.launch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.TypeParser;
import org.testng.eclipse.util.param.ParameterSolver;


public class TestNGLaunchShortcut implements ILaunchShortcut {

  public void launch(ISelection selection, String mode) {
    if(selection instanceof StructuredSelection) {
      run((IJavaElement) ((StructuredSelection) selection).getFirstElement(), mode);
    }
  }

  public void launch(IEditorPart editor, String mode) {
  }

  protected void run(IJavaElement ije, String mode) {
    IJavaProject ijp = ije.getJavaProject();
    IType[] types = new IType[0];
    List methodNames = null;
    List packageNames= new ArrayList();
    
    int runType= -1;

    switch(ije.getElementType()) {
      case IJavaElement.PACKAGE_FRAGMENT:
      {
        runType= TestNGLaunchConfigurationConstants.PACKAGE;
        packageNames.add(((IPackageFragment) ije).getElementName());
        
        break;
      }
      
      case IJavaElement.COMPILATION_UNIT:
      {
        runType= TestNGLaunchConfigurationConstants.CLASS;
        
        
        try {
          types = ((ICompilationUnit) ije).getTypes();
        }
        catch(JavaModelException jme) {
          ; // nothing
        }

        break;
      }
      
      case IJavaElement.TYPE:
      {
        types = new IType[] {(IType) ije};
        runType= TestNGLaunchConfigurationConstants.CLASS;

        break;
      }
      
      case IJavaElement.METHOD:
      {
        IMethod imethod = (IMethod) ije;
        Map methods = new HashMap();
        methods.put(imethod.getElementName(), imethod);
        
        solveDependencies(imethod, methods);
        
        methodNames= new ArrayList(methods.size());
        Set typesSet = new HashSet();
        for(Iterator it= methods.values().iterator(); it.hasNext(); ) {
          IMethod m= (IMethod) it.next();
          methodNames.add(m.getElementName());
          typesSet.add(m.getDeclaringType());
        }
        
        types= (IType[]) typesSet.toArray(new IType[typesSet.size()]);
        runType= TestNGLaunchConfigurationConstants.METHOD;
        
        break;
      }
      
      default:
        return;
    }
    
    List typeNames = new ArrayList();
    
    for(int i = 0; i < types.length; i++) {
      typeNames.add(types[i].getFullyQualifiedName());
    }
    
    
    Map parameters= ParameterSolver.solveParameters(ije);
    
    ILaunchConfigurationWorkingCopy workingCopy = ConfigurationHelper.createBasicConfiguration(
        getLaunchManager(), ijp.getProject(), "TestNG context suite");
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
                             typeNames);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.METHOD_TEST_LIST,
                             methodNames);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PACKAGE_TEST_LIST,
                             packageNames);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE,
                             runType);
    if(null != parameters) {
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PARAMS,
                               parameters);
    }

    if (IJavaElement.PACKAGE_FRAGMENT != ije.getElementType()) {
      ITestContent testContent = TypeParser.parseType(types[0]);
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR,
                               testContent.getAnnotationType());
    }

    
    if(null != workingCopy) {
      try {
        launchConfiguration(workingCopy.doSave(), mode);
      }
      catch(CoreException ce) {
        TestNGPlugin.log(ce);
      }
    }
  }

  private void solveDependencies(IMethod method, Map allMethods) {
    try {
      DependencyVisitor dv= new DependencyVisitor();
      ASTParser parser= ASTParser.newParser(AST.JLS3);
      parser.setSource(method.getSource().toCharArray());
      parser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
      ASTNode node= parser.createAST(null);
      node.accept(dv);
      if(!dv.dependsOn.isEmpty()) {
        for(int i= 0; i < dv.dependsOn.size(); i++) {
          String methodName= (String) dv.dependsOn.get(i);
          if(!allMethods.containsKey(methodName)) {
            IMethod meth= solveMethod(method.getDeclaringType(), methodName);
            allMethods.put(methodName, meth);
            solveDependencies(meth, allMethods);
          }
        }
      }
    }
    catch(JavaModelException jme) {
      ; //ignore
    }
  }
  
  private IMethod solveMethod(IType type, String methodName) {
    try {
      IMethod[] typemethods= type.getMethods();
      
      for(int i=0; i < typemethods.length; i++) {
        if(methodName.equals(typemethods[i].getElementName())) {
          return typemethods[i];
        }
      }
      
      ITypeHierarchy typeHierarchy= type.newSupertypeHierarchy(null);
      IType[] superTypes= typeHierarchy.getAllSuperclasses(type);
      for(int i= 0; i < superTypes.length; i++) {
        IMethod[] methods= superTypes[i].getMethods();
        
        for(int j=0; j < methods.length; j++) {
          if(methodName.equals(methods[j].getElementName())) {
            return methods[j];
          }
        }
      }
    }
    catch(JavaModelException jme) {
      ; //ignore
    }
    
    return null;
  }
  
  protected void launchConfiguration(ILaunchConfiguration config, String mode) {
    if(null != config) {
      DebugUITools.launch(config, mode);
    }
  }
  
  protected ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }
  
  private static class DependencyVisitor extends ASTVisitor {
    private static final String ANNOTATION_PACKAGE = "org.testng.annotations.";
    private static final String TEST_ANNOTATION = "Test";
    private static final String TEST_ANNOTATION_FQN = ANNOTATION_PACKAGE + TEST_ANNOTATION;
    List dependsOn= new ArrayList();

    public boolean visit(NormalAnnotation annotation) {
      if(!TEST_ANNOTATION.equals(annotation.getTypeName().getFullyQualifiedName()) 
          && !TEST_ANNOTATION_FQN.equals(annotation.getTypeName().getFullyQualifiedName())) {
        return false;
      }
      
      List values= annotation.values();
      
      if(null != values && !values.isEmpty()) {
        for(int i= 0; i < values.size(); i++) {
          MemberValuePair pair= (MemberValuePair) values.get(i);
          if("dependsOnMethods".equals(pair.getName().toString())) {
            Expression paramAttr= pair.getValue();
            if(paramAttr instanceof ArrayInitializer) {
              List literals= ((ArrayInitializer) paramAttr).expressions();
              List paramNames= new ArrayList(literals.size());
              for(int j= 0; j < literals.size(); j++) {
                StringLiteral str= (StringLiteral) literals.get(j);
                dependsOn.add(str.getLiteralValue());
              }
            }
          }
        }
      }
      
      return false;
    }
  }
}
