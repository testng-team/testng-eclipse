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
import org.testng.eclipse.util.JDTUtil;
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
    String confName= null;
    
    int runType= -1;

    switch(ije.getElementType()) {
      case IJavaElement.PACKAGE_FRAGMENT:
      {
        runType= TestNGLaunchConfigurationConstants.PACKAGE;
        IPackageFragment ipf= (IPackageFragment) ije;
        packageNames.add(ipf.getElementName());
        confName= "package " + ipf.getElementName();
        
        break;
      }
      
      case IJavaElement.COMPILATION_UNIT:
      {
        runType= TestNGLaunchConfigurationConstants.CLASS;
        
        
        try {
          ICompilationUnit icu= (ICompilationUnit) ije;
          types = icu.getTypes();
          
          IType mainType= icu.findPrimaryType();
          confName= mainType != null ? mainType.getElementName() : icu.getElementName();
        }
        catch(JavaModelException jme) {
          ; // nothing
        }

        break;
      }
      
      case IJavaElement.TYPE:
      {
        IType type= (IType) ije;
        types = new IType[] {type};
        runType= TestNGLaunchConfigurationConstants.CLASS;
        confName= type.getElementName();
        
        break;
      }
      
      case IJavaElement.METHOD:
      {
        IMethod imethod = (IMethod) ije;
        Map methods = new HashMap();
        methods.put(imethod.getElementName(), imethod);
        
        JDTUtil.solveDependencies(imethod, methods);
        
        methodNames= new ArrayList(methods.size());
        for(Iterator it= methods.values().iterator(); it.hasNext(); ) {
          IMethod m= (IMethod) it.next();
          methodNames.add(m.getElementName());
        }
        
        types= new IType[] {imethod.getDeclaringType()};
        runType= TestNGLaunchConfigurationConstants.METHOD;
        confName= imethod.getDeclaringType().getElementName() + "." + imethod.getElementName();
        
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
    
    ILaunchConfiguration config= ConfigurationHelper.findConfiguration(getLaunchManager(), ijp.getProject(), confName);
    
    if(null == config) {    
      ILaunchConfigurationWorkingCopy workingCopy = ConfigurationHelper.createBasicConfiguration(
          getLaunchManager(), ijp.getProject(), confName);
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
  
      try {
        config= workingCopy.doSave();
      }
      catch(CoreException cex) {
        TestNGPlugin.log(cex);
      }
    }
    
    if(null != config) {
      launchConfiguration(config, mode);
    }
  }

  protected void launchConfiguration(ILaunchConfiguration config, String mode) {
    if(null != config) {
      DebugUITools.launch(config, mode);
    }
  }
  
  protected ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }  
}
