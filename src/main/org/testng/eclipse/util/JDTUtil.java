package org.testng.eclipse.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants;

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
   *
   * @param jproject
   * @return
   */
  public static List getSourceDirFileList(IJavaProject jproject) {
    if ((null == jproject) || !jproject.exists()) {
      return Collections.EMPTY_LIST;
    }

    List sourcePaths = new ArrayList();

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

  public static String getProjectVMVersion(IJavaProject ijp) {
    if(null == ijp) {
      return "";
    }
    
    try {
      IType itype = ijp.findType("java.lang.annotation.Annotation");

      return (itype == null) ? TestNGLaunchConfigurationConstants.JDK14_COMPLIANCE
                             : TestNGLaunchConfigurationConstants.JDK15_COMPLIANCE;
    }
    catch (JavaModelException jme) {
      TestNGPlugin.log(jme);
    }

    return "";
  }
}
