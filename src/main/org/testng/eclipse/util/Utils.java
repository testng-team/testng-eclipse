package org.testng.eclipse.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.testng.eclipse.collections.Lists;
import org.testng.eclipse.launch.components.Filters.ITypeFilter;
import org.testng.eclipse.refactoring.FindTestsRunnableContext;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

public class Utils {
  public static class ProjectPackage {
    public IJavaProject project;
    public IPackageFragmentRoot packageFragmentRoot;
    public IPackageFragment packageFragment;
    public ICompilationUnit compilationUnit;
  }

  /**
   * @return all the ITypes included in the current selection.
   */
  public static List<IType> findSelectedTypes(IWorkbenchPage page) {
    IType[] types = null;
    ProjectPackage pp = Utils.getSelectedProjectOrPackage(page);
    if (pp.compilationUnit != null) {
      try {
        types = ((ICompilationUnit) pp.compilationUnit).getAllTypes();
      } catch (JavaModelException e) {
        e.printStackTrace();
      }
    } else {
      IJavaProject project = pp.project;
      IPackageFragmentRoot pfr = pp.packageFragmentRoot;
      IPackageFragment pf = pp.packageFragment;
      try {
        ITypeFilter filter = new ITypeFilter() {
          public boolean accept(IType type) {
            return true;
          }
        };

        IRunnableContext context = new FindTestsRunnableContext();
        if (pf != null) {
          types = TestSearchEngine.findTests(context, new Object[] { pf }, filter);
        } else if (pfr != null) {
          types = TestSearchEngine.findTests(context, new Object[] { pfr }, filter);
        } else if (project != null) {
          types = TestSearchEngine.findTests(context, new Object[] { project }, filter);
        }
      }
      catch(InvocationTargetException ex) {
        ex.printStackTrace();
      }
      catch(InterruptedException ex) {
        // ignore
      }
    }

    return Arrays.asList(types);
  }

  /**
   * Known limitation of this method: if the selection is happening in the Navigator,
   * the selected tree item will contain a path that I'm not bothering turning into
   * Java elements: instead, I just return the entire project. Therefore, right clicking
   * on a file in the Navigator and selecting "Convert to TestNG" will cause the refactoring
   * to apply to the entire project.
   *
   * TODO: handle the Navigator as well as the Package Explorer.
   *
   * @param page
   * @return
   */
  private static ProjectPackage getSelectedProjectOrPackage(IWorkbenchPage page) {
    ProjectPackage result = new ProjectPackage();
    ISelection selection = page.getSelection();

    if (selection instanceof TreeSelection) {
      TreeSelection sel = (TreeSelection) selection;
      TreePath[] paths = sel.getPaths();
      for (TreePath path : paths) {
        int count = path.getSegmentCount();
        if (count > 0) {
          Object project = path.getFirstSegment();
          if (project instanceof IJavaProject) {
            result.project = (IJavaProject) project;
          } else if (project instanceof IProject) {
            result.project = JavaCore.create((IProject) project);
          }
        }
        if (count > 1 && path.getSegment(1) instanceof IPackageFragmentRoot) {
          result.packageFragmentRoot = (IPackageFragmentRoot) path.getSegment(1);
        }
        if (count > 2 && path.getSegment(2) instanceof IPackageFragment) {
          result.packageFragment = (IPackageFragment) path.getSegment(2);
        }
        if (count > 3 && path.getSegment(3) instanceof ICompilationUnit) {
          result.compilationUnit = (ICompilationUnit) path.getSegment(3);
        }
      }
    }

    return result;
  }

  /**
   * @return the source folders for this Java project.
   */
  public static List<IClasspathEntry> getSourceFolders(IJavaProject jp) {
    List<IClasspathEntry> result = Lists.newArrayList();
    try {
      for (IClasspathEntry entry : jp.getRawClasspath()) {
        if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
          result.add(entry);
        }
      }
    } catch (JavaModelException e) {
      e.printStackTrace();
    }
    return result;
  }
}
