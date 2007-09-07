package org.testng.eclipse.util;


import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.components.Filters;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.ui.util.TypeParser;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;

/**
 * Search engine for TestNG related elements.
 * <P/>
 * Original idea from org.eclipse.jdt.internal.junit.util.TestSearchEngine
 * 
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 * @author cedric
 */
public class TestSearchEngine {

  /**
   * Searches for TestNG test types.
   *
   * @param context
   * @param javaProject
   * @return
   * @throws InvocationTargetException
   * @throws InterruptedException
   */
  public static IType[] findTests(IRunnableContext context,
                                  final Object[] elements,
                                  final Filters.ITypeFilter filter) throws InvocationTargetException, InterruptedException {
    final Set result = new HashSet();

    if(elements.length != 0) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
          public void run(IProgressMonitor pm) throws InterruptedException {
            doFindTests(elements, result, pm, filter);
          }
        };
      context.run(true, true, runnable);
    }

    return (IType[]) result.toArray(new IType[result.size()]);
  }

  public static File[] findSuites(IRunnableContext context,
                                  final Object[] elements) throws InvocationTargetException, InterruptedException {
    final Set result = new HashSet();

    if(elements.length != 0) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
          public void run(IProgressMonitor pm) throws InterruptedException {
            doFindSuites(elements, result, pm);
          }
        };
      context.run(true, true, runnable);
    }

    return (File[]) result.toArray(new File[result.size()]);
  }

  public static IFile[] findSuites(final Object[] elements) throws InterruptedException,
                                                                   InvocationTargetException {
    final Set result = new HashSet();

    if(elements.length > 0) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
          public void run(IProgressMonitor pm) throws InterruptedException {
            doFindSuites(elements, result, pm);
          }
        };
      PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
    }

    return (IFile[]) result.toArray(new IFile[result.size()]);
  }

  public static IType[] findTests(final Object[] elements, final Filters.ITypeFilter filter)
  throws InvocationTargetException, InterruptedException {
    final Set result = new HashSet();

    if(elements.length > 0) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
          public void run(IProgressMonitor pm) throws InterruptedException {
            doFindTests(elements, result, pm, filter);
          }
        };
      PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
    }

    return (IType[]) result.toArray(new IType[result.size()]);
  }

  private static Map/*<IJavaElement,Boolean>*/ s_isTestCache= new HashMap();
  
  /**
   * Returns true in case the IJavaElement is one of COMPILATION_UNIT, TYPE or METHOD 
   * representing a valid TestNG test element.
   */
  public static boolean isTest(IJavaElement ije) {
    Boolean cachedResult= (Boolean) s_isTestCache.get(ije);
    if(null != cachedResult && cachedResult.booleanValue()) {
      return true;
    }
    
    boolean result= false;
    IType[] types = null;
    
    if(IJavaElement.METHOD == ije.getElementType()) {
      IMethod iMethod = (IMethod) ije;
      ITestContent content = TypeParser.parseType(iMethod.getDeclaringType());
      if(content.hasTestMethods()) {
        result= content.isTestMethod(iMethod);
        if(result) {
          s_isTestCache.put(ije, Boolean.TRUE);
        }
        return result;
      }
      
      return false;
    }
    
    if(IJavaElement.COMPILATION_UNIT == ije.getElementType()) {
      try {
        types = ((ICompilationUnit) ije).getAllTypes();
      }
      catch(JavaModelException jme) {
        TestNGPlugin.log(jme);
      }
    } 
    else if(IJavaElement.TYPE == ije.getElementType()) {
      types = new IType[] {(IType) ije};
    } 
    else {
      return false;
    }
    
    if(null != types) {
      for(int i = 0; i < types.length; i++) {
        ITestContent testContent = TypeParser.parseType(types[i]);
        
        if(testContent.hasTestMethods()) {
          s_isTestCache.put(ije, Boolean.TRUE);
          return true;
        }
      }
    }
    
    return false;
  }
  
  private static void doFindTests(Object[] elements,
                                  Set result,
                                  IProgressMonitor pm,
                                  Filters.ITypeFilter filter) throws InterruptedException {
    int nElements = elements.length;
    pm.beginTask(ResourceUtil.getString("TestSearchEngine.message.searching"), nElements); //$NON-NLS-1$
    try {
      for(int i = 0; i < nElements; i++) {
        try {
          collectTypes(elements[i], new SubProgressMonitor(pm, 1), result, filter);
        }
        catch(CoreException e) {
          TestNGPlugin.log(e.getStatus());
        }
        if(pm.isCanceled()) {
          throw new InterruptedException();
        }
      }
    }
    finally {
      pm.done();
    }
  }

  private static boolean isTestNgXmlFile(IFile f) {
    if(!"xml".equals(f.getFileExtension())) {
      return false;
    }

    try {
      return SuiteFileValidator.isSuiteDefinition(f);
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }

    return false;
  }

  private static void doFindSuites(Object[] elements, Set result,
                                   IProgressMonitor pm) 
  throws InterruptedException 
{
    int nElements = elements.length;
    pm.beginTask(ResourceUtil.getString("TestSearchEngine.message.searching"), nElements); //$NON-NLS-1$
    
    try {
      for(int i = 0; i < nElements; i++) {
        if(elements[i] instanceof IJavaProject) {
          findSuites(((IJavaProject) elements[i]).getProject(), result);
        }
        if(pm.isCanceled()) {
          throw new InterruptedException();
        }
      }
    }
    finally {
      pm.done();
    }
    
  }

  private static void findSuites(IContainer ires, Set results) {
    if(null == ires) {
      return;
    }

    try {
      IResource[] children = ires.members();
      for(int i = 0; i < children.length; i++) {
        if(children[i] instanceof IFile) {
          if(isTestNgXmlFile((IFile) children[i])) {
            results.add(children[i]);
          }
        }
        else {
          findSuites((IContainer) children[i], results);
        }
      }
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }
  }

  private static void collectTypes(Object element,
                                   IProgressMonitor pm,
                                   Set result,
                                   Filters.ITypeFilter filter) throws CoreException {
    pm.beginTask(ResourceUtil.getString("TestSearchEngine.message.searching"), 10); //$NON-NLS-1$
    element = computeScope(element);
    try {
//      ppp("CURRENT ELEMENT:" + element.getClass());
      while((element instanceof ISourceReference) && !(element instanceof ICompilationUnit)) {
        if(element instanceof IType) {
          if(filter.accept((IType) element)) {
            result.add(element);

            return;
          }
        }
        element = ((IJavaElement) element).getParent();
      }
      if(element instanceof ICompilationUnit) {
        ICompilationUnit cu = (ICompilationUnit) element;

        IType[] types = cu.getAllTypes();

        for(int i = 0; i < types.length; i++) {
          if(filter.accept(types[i])) {
            result.add(types[i]);
          }
        }
      }
      else if(element instanceof IJavaElement) {
        findTestTypes(((IJavaElement) element).getJavaProject(), result, filter);
      }
    }
    finally {
      pm.done();
    }
  }


  private static void findTestTypes(IJavaElement ije, 
                                    Set result,
                                    Filters.ITypeFilter filter) {
    if(IJavaElement.PACKAGE_FRAGMENT > ije.getElementType()) {
      try {
        IJavaElement[] children = ((IParent) ije).getChildren();

        for(int i = 0; i < children.length; i++) {
          findTestTypes(children[i], result, filter);
        }
      }
      catch(JavaModelException jme) {
        TestNGPlugin.log(jme);
      }
    }

    if(IJavaElement.PACKAGE_FRAGMENT == ije.getElementType()) {
      try {
        ICompilationUnit[] compilationUnits = ((IPackageFragment) ije).getCompilationUnits();

        for(int i = 0; i < compilationUnits.length; i++) {
          findTestTypes(compilationUnits[i], result, filter);
        }
      }
      catch(JavaModelException jme) {
        TestNGPlugin.log(jme);
      }

    }

    if(IJavaElement.COMPILATION_UNIT == ije.getElementType()) {
      try {
        IType[] types = ((ICompilationUnit) ije).getAllTypes();

        for(int i = 0; i < types.length; i++) {
          if(filter.accept(types[i])) {
            result.add(types[i]);
          }
        }
      }
      catch(JavaModelException jme) {
        TestNGPlugin.log(jme);
      }
    }
  }


  private static Object computeScope(Object element) throws JavaModelException {
    if(element instanceof IResource) {
      element = JavaCore.create((IResource) element);
    }
    if(element instanceof IClassFile) {
      IClassFile cf = (IClassFile) element;
      element = cf.getType();
    }

    return element;
  }

  public static void ppp(String s) {
    System.out.println("[TestSearchEngine] " + s);
  }
}
