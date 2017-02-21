package org.testng.eclipse.util;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.ui.RunInfo;

/**
 * Class offering utility method to access different Eclipse resources.
 *
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class JDTUtil {
  public static final String PROJECT_TYPE = "P";
  public static final String DIRECTORY_TYPE = "D";
  public static final String CLASS_TYPE = "C";
  public static final String SOURCE_TYPE = "J";
  public static final String SUITE_TYPE = "S";

  public static final int NO_CTX = 0;
  public static final int TEST_CLASS_CTX = 1;
  public static final int TEST_SUITE_CTX = 2;

  private JDTUtil() {
  }

  /**
   * Convenience method to get the workspace root.
   */
  public static IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  /**
   * Convenience method to get access to the java model.
   */
  public static IJavaModel getJavaModel() {
    return JavaCore.create(getWorkspaceRoot());
  }

  public static IJavaProject getJavaProject(String projectName) {
    if ((null == projectName) || (projectName.length() < 1)) {
      return null;
    }

    return getJavaModel().getJavaProject(projectName);
  }

  public static IJavaProject[] getJavaProjects() {
    IJavaProject[] projects = null;

    try {
      projects = JavaCore.create(getWorkspaceRoot()).getJavaProjects();
    }
    catch (JavaModelException jme) {
      projects = new IJavaProject[0];
    }

    return projects;
  }

  public static IJavaProject getJavaProjectContext() {
    IWorkbenchPage page = getActivePage();
    if (null != page) {
      ISelection selection = page.getSelection();
      if ((null != selection) && (selection instanceof IStructuredSelection)) {
        IStructuredSelection ss = (IStructuredSelection) selection;
        if (!ss.isEmpty()) {
          Object obj = ss.getFirstElement();

          if (obj instanceof IJavaElement) {
            return ((IJavaElement) obj).getJavaProject();
          }

          if (obj instanceof IResource) {
            IProject pro = ((IResource) obj).getProject();
            IJavaProject ijp = JavaCore.create(pro);

            return ijp;
          }
        }
      }
    }

    return null;
  }

  public static String getResourceType(IResource resource) {
    if (resource instanceof IProject) {
      return "P";
    }
    if (resource instanceof IFolder) {
      return "D";
    }

    IFile file = (IFile) resource;
    if ("class".equals(file.getFileExtension())) {
      return "C";
    }
    else if ("java".equals(file.getFileExtension())) {
      return "J";
    }
    else if ("xml".equals(file.getFileExtension())) {
      return "S";
    }

    return "";
  }

  private static IWorkbenchPage getActivePage() {
    IWorkbenchWindow window = TestNGPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();

    if (null != window) {
      return window.getActivePage();
    }

    return null;
  }

  public static int getResourceContextType() {
    IWorkbenchPage page = getActivePage();

    if (null != page) {
      ISelection selection = page.getSelection();

      if ((null != selection) && (selection instanceof IStructuredSelection)) {
        IStructuredSelection ss = (IStructuredSelection) selection;

        if (!ss.isEmpty()) {
          Object obj = ss.getFirstElement();

          if (obj instanceof IJavaElement) {
            IJavaElement ije = (IJavaElement) obj;
            ppp("getResourceContextType():IJavaElement: " + ije.getElementName());
            ppp("getResourceContextType():Element type: " + ije.getElementType());

            if (IJavaElement.COMPILATION_UNIT <= ije.getElementType()) {
              return TEST_CLASS_CTX;
            }
            else {
              return NO_CTX;
            }
          }

          if (obj instanceof IResource) {
            IResource ir = (IResource) obj;

            if (IResource.FILE == ir.getType()) {
              if ("class".equals(ir.getFileExtension())) { // IClassFile
                return TEST_CLASS_CTX;
              }
              else if ("xml".equals(ir.getFileExtension())) {
                return TEST_SUITE_CTX;
              }

              return NO_CTX;
            }
            else {
              return NO_CTX;
            }
          }
        }
      }
    }

    return NO_CTX;
  }

  public static ICompilationUnit getJavaElement(IFile file) {
    IJavaElement ije = JavaCore.create(file);

    return (ICompilationUnit) ije.getAncestor(IJavaElement.COMPILATION_UNIT);
  }

  /**
   * @return
   */
  public static Object getResourceContext() {
    IWorkbenchPage page = getActivePage();

    if (null != page) {
      ISelection selection = page.getSelection();

      if ((null != selection) && (selection instanceof IStructuredSelection)) {
        IStructuredSelection ss = (IStructuredSelection) selection;

        if (!ss.isEmpty()) {
          Object obj = ss.getFirstElement();

          if (obj instanceof IJavaElement) {
            IJavaElement ije = (IJavaElement) obj;
            ppp("IJavaElement: " + ije.getElementName());
            ppp("Element type: " + ije.getElementType());
            if (IJavaElement.COMPILATION_UNIT <= ije.getElementType()) {
              return ije.getAncestor(IJavaElement.COMPILATION_UNIT);
            }
          }

          if (obj instanceof IResource) {
            IResource ir = (IResource) obj;

            if (IResource.FILE == ir.getType()) {
              if ("class".equals(ir.getFileExtension())) {
                IJavaElement ije = JavaCore.create((IFile) ir);

                return ije.getAncestor(IJavaElement.COMPILATION_UNIT);
              }
              else if ("xml".equals(ir.getFileExtension())) {
                return ir;
              }
            }
          }
        }
      }
    }

    return null;
  }

  private static void ppp(final Object msg) {
    System.out.println("[JDTUtil]: " + msg);
  }

  /**
   * Returns a list of files (List<File>) containing the source directories
   * defined in the specified project.
   */
  public static List<File> getSourceDirFileList(IJavaProject jproject) {
    if ((null == jproject) || !jproject.exists()) {
      return Collections.<File>emptyList();
    }

    List<File> sourcePaths = new ArrayList<>();

    try {
      IPackageFragmentRoot[] sourceRoots = jproject.getAllPackageFragmentRoots();

      for (int i = 0; i < sourceRoots.length; i++) {
        if (IPackageFragmentRoot.K_SOURCE == sourceRoots[i].getKind()) {
          IResource sourceRes = sourceRoots[i].getCorrespondingResource();
          if (null != sourceRes) {
            sourcePaths.add(sourceRes.getLocation().toFile());
          }
        }
      }
    }
    catch (JavaModelException jme) {
      TestNGPlugin.log(jme);
    }

    return sourcePaths;
  }

  public static IJavaElement findElement(IJavaProject javaProject, String className,
      String methodName, String[] paramTypes) throws JavaModelException {

    IType type = javaProject.findType(className);
    if (null == type) {
      return null;
    }

    if (null == methodName) {
      return type;
    }

    if (null == paramTypes) {
      paramTypes= new String[0];
    }
    List<String> params= new ArrayList<String>(paramTypes.length);
    for(String paramType : paramTypes) {
      int idx= paramType.lastIndexOf('.');
      String typeName= idx == -1 ? paramType : paramType.substring(idx + 1);
      params.add(Signature.createTypeSignature(typeName, false));
    }
    IMethod method= findMethodInTypeHierarchy(type, methodName,
        params.toArray(new String[paramTypes.length]));
    if (null == method) {
      method = fuzzyFindMethodInTypeHierarchy(type, methodName, paramTypes);
    }
    
    return method;
  }

  public static IJavaElement findElement(IJavaProject javaProject, RunInfo runInfo)
      throws JavaModelException {

    return findElement(javaProject, runInfo.getClassName(), runInfo.getMethodName(),
        runInfo.getParameterTypes());
  }
  
  public static IJavaElement findElement(IJavaProject javaProject, String className) 
  throws JavaModelException {
    return javaProject.findType(className);
  }
  
  /**
   * Retrieves in the <code>IJavaProject</code> the class or the method element.
   * @param javaProject
   * @param className
   * @param methodName
   * @return
   * @throws JavaModelException
   */
//  public static IJavaElement findElement(IJavaProject javaProject, String className, String methodName) 
//  throws JavaModelException {
//    IType type = javaProject.findType(className);
//    if(null == type) {
//      return null;
//    }
//
//    if(null == methodName) {
//      return type;
//    }
//
//    // FIXME: we need some work here
//    String finalMethodName = methodName.indexOf('(') == -1 ? methodName : methodName.substring(0, methodName.indexOf('('));
//    
//    IMethod method = findMethodInTypeHierarchy(type, finalMethodName);
//    if (null == method) {
//      method = fuzzyFindMethodInTypeHierarchy(type, finalMethodName);
//    }
//
//    return method;
//  }
  
  private static IMethod findMethodInTypeHierarchy(IType type, String methodName, String[] paramTypes) throws JavaModelException {
    IMethod method = type.getMethod(methodName, paramTypes);
    if(method != null && method.exists()) { 
        return method;
    }

    ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(null);
    IType[] types = typeHierarchy.getAllSuperclasses(type);
    for(IType t : types) {
      method = t.getMethod(methodName, paramTypes);
      if((method != null) && method.exists()) {
        return method;
      }
    }

    return null;
  }

  public static IMethod fuzzyFindMethodInTypeHierarchy(IType type, String methodName,
      String[] paramTypes) throws JavaModelException {
    List<IMethod> fuzzyResults= new ArrayList<IMethod>();
    IMethod[] methods = type.getMethods();
    for(IMethod m : methods) {
      if(methodName.equals(m.getElementName()) && m.exists()) {
        if(m.getNumberOfParameters() == paramTypes.length) {
          return m;
        }
        else {
          fuzzyResults.add(m);
        }
      }
    }

    ITypeHierarchy typeHierarchy = type.newSupertypeHierarchy(null);
    IType[] types = typeHierarchy.getAllSuperclasses(type);
    for(IType t : types) {
      methods = t.getMethods();
      for(IMethod m : methods) {
        if(methodName.equals(m.getElementName()) && m.exists()) {
          return m;
        }
      }
    }

    return (fuzzyResults.isEmpty() ? null : fuzzyResults.get(0) );
  }

  public static IMethod fuzzyFindMethodInProject(IJavaProject project,
      IType methodType, IMethod currentMethod, String methodName) throws JavaModelException {
    int dotIdx = methodName.lastIndexOf('.');
    if (dotIdx > 0) {
      String fullyQualifiedName = methodName.substring(0, dotIdx);
      methodType = project.findType(fullyQualifiedName);
      methodName = methodName.substring(dotIdx + 1);
    }
    if (methodType == null) {
      TestNGPlugin.log("Could not find the enclosed class for: " + methodName);
      return null;
    }
    IMethod depMethod = fuzzyFindMethodInTypeHierarchy(methodType,
        methodName, new String[0]);
    if (depMethod == null) {
      // just log the error only, since the testng core is responsible for print the true error message
      TestNGPlugin.log("Could not find method: " + methodType.getFullyQualifiedName() + "." + methodName);
    }
    return depMethod;
  }

  public static List<MethodDefinition> solveDependencies(IMethod method) {
    Set<String> parsedMethods = new HashSet<>();
    MethodDefinition md = new MethodDefinition(method);
    parsedMethods.add(method.getElementName());

    List<MethodDefinition> results = new ArrayList<>();
    results.add(md);
    results.addAll(solveDependencies(md, parsedMethods));

    return results;
  }

  /**
   * Tries to retrieve the dependsOn part of a method definition.
   * @param method
   * @param allMethods
   */
  private static List<MethodDefinition> solveDependencies(MethodDefinition methodDef,
      Set<String> parsedMethods) {
    DependencyVisitor dv = parse(methodDef.getMethod());

    List<MethodDefinition> results = new ArrayList<>();
    List<String> dependsOnMethods = dv.getDependsOnMethods();

    if(!dependsOnMethods.isEmpty()) {
      for(String methodName : dependsOnMethods) {
        if(!parsedMethods.contains(methodName)) {
          IMethod meth= solveMethod(methodDef.getMethod().getDeclaringType(), methodName);
          if(null != meth) {
            MethodDefinition md= new MethodDefinition(meth);

            parsedMethods.add(meth.getElementName());
            results.add(md);
            methodDef.addDependencyMethod(md);
            results.addAll(solveDependencies(md, parsedMethods));
          }
        }
      }
    }

    methodDef.addDependencyGroups(dv.getDependsOnGroups());

    return results;
  }

  private static DependencyVisitor parse(IMethod method) {
    DependencyVisitor dv= new DependencyVisitor();
    try {
      ASTParser parser= ASTParser.newParser(AST.JLS3);
      parser.setSource(method.getSource().toCharArray());
      parser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
      ASTNode node= parser.createAST(null);
      node.accept(dv);
    }
    catch(JavaModelException jmex) { ; }

    return dv;
  }
  
  public static IMethod solveMethod(IType type, String methodName) {
    try {
      IMethod[] typemethods= type.getMethods();
      
      for(IMethod m : typemethods) {
        if(methodName.equals(m.getElementName())) {
          return m;
        }
      }
      
      ITypeHierarchy typeHierarchy= type.newSupertypeHierarchy(null);
      IType[] superTypes= typeHierarchy.getAllSuperclasses(type);
      for(IType t : superTypes) {
        IMethod[] methods= t.getMethods();
        
        for(IMethod m : methods) {
          if(methodName.equals(m.getElementName())) {
            return m;
          }
        }
      }
    }
    catch(JavaModelException jme) {
      ; //ignore
    }
    
    return null;
  }
  
  public static class MethodDefinition {
    private final IMethod m_method;
    private final Set<String> m_dependsongroups= new HashSet<>();
    private final Set<MethodDefinition> m_dependsonmethods= new HashSet<MethodDefinition>();

    public MethodDefinition(IMethod method) {
      m_method= method;
    }

    public void addDependencyGroups(List<String> dependsOnGroups) {
      if (null != dependsOnGroups && !dependsOnGroups.isEmpty()) {
        m_dependsongroups.addAll(dependsOnGroups);
      }
    }

    public void addDependencyMethod(MethodDefinition md) {
      m_dependsonmethods.add(md);
    }

    /**
     * @return
     */
    public IMethod getMethod() {
      return m_method;
    }

    public String getKey() {
      return m_method.getKey(); 
    }

    /**
     * @return
     */
    public Set<String> getGroups() {
      return m_dependsongroups;
    }
  }
}