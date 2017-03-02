package org.testng.eclipse.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.testng.eclipse.launch.components.Filters.ITypeFilter;
import org.testng.eclipse.refactoring.FindTestsRunnableContext;
import org.testng.eclipse.ui.conversion.JUnitConverterQuickAssistProcessor;
import org.testng.eclipse.ui.conversion.JUnitVisitor;

public class Utils {
  /**
   * A filter that will keep types that need to be converted to TestNG.
   */
  public static final ITypeFilter CONVERSION_FILTER = new ITypeFilter() {

    public boolean accept(IType type) {
      IResource resource = type.getResource();
      ICompilationUnit cu = JDTUtil.getJavaElement((IFile) resource);
      CompilationUnit astRoot = JUnitConverterQuickAssistProcessor.createCompilationUnit(cu);
      JUnitVisitor visitor = new JUnitVisitor();
      astRoot.accept(visitor);

      return visitor.needsConversion();
    }

  };

  public static class JavaElement {
    public IJavaProject m_project;
    public IPackageFragmentRoot packageFragmentRoot;
    public IPackageFragment packageFragment;
    public ICompilationUnit compilationUnit;
    public String sourceFolder;

    public JavaElement() {
    }

    public String getPath() {
      String result = null;
      if (compilationUnit != null) {
        result = resourceToPath(compilationUnit);
      } else if (packageFragmentRoot != null) {
        result = resourceToPath(packageFragmentRoot);
      } else if (packageFragment != null) {
        result = resourceToPath(packageFragment);
      } else {
        result = resourceToPath(getProject());
      }
      return result;
    }

    public IJavaProject getProject() {
      if (m_project != null) return m_project;
      else if (packageFragmentRoot != null) return packageFragmentRoot.getJavaProject();
      else if (packageFragment != null) return packageFragment.getJavaProject();
      else if (compilationUnit != null) return compilationUnit.getJavaProject();
      else throw new AssertionError("Couldn't find a project");
    }

    private String resourceToPath(IJavaElement element) {
      return ((IResource) element.getAdapter(IResource.class)).getFullPath().toOSString();
    }

    public String getPackageName() {
      String result = null;
      if (packageFragment != null) {
        result = packageFragment.getElementName();
      } else if (compilationUnit != null) {
        try {
          IPackageDeclaration[] pkg = compilationUnit.getPackageDeclarations();
          result = pkg.length > 0 ? pkg[0].getElementName() : null;
        } catch (JavaModelException e) {
          // ignore
        }
      }

      return result;
    }

    public String getClassName() {
      String result = null;
      if (compilationUnit != null) {
        result = compilationUnit.getElementName();
        if (result.endsWith(".java")) {
          result = result.substring(0, result.length() - ".java".length());
        }
      }
      return result;
    }

    public IResource getResource() {
      if (compilationUnit != null) {
        return (IResource) compilationUnit.getAdapter(IResource.class);
      } else if (packageFragment != null) {
        return (IResource) packageFragment.getAdapter(IResource.class);
      } else if (m_project != null) {
        return (IResource) m_project.getAdapter(IResource.class);
      } else {
        return null;
      }
    }


  }

  /**
   * @return all the ITypes included in the current selection.
   */
  public static List<IType> findSelectedTypes(IWorkbenchPage page, ITypeFilter filter) {
    return findTypes(Utils.getSelectedJavaElements(page), filter);
  }

  public static List<IType> findTypes(List<JavaElement> elements, ITypeFilter filter) {
    List<IType> result = new ArrayList<>();
    if (filter == null) {
      filter = new ITypeFilter() {
        public boolean accept(IType type) {
          return true;
        }
      };
    };

    for (JavaElement pp : elements) {
      if (pp.compilationUnit != null) {
        try {
          for (IType t : pp.compilationUnit.getAllTypes()) {
            if (filter.accept(t)) {
              result.add(t);
            }
          }
        } catch (JavaModelException e) {
          e.printStackTrace();
        }
      } else {
        IPackageFragmentRoot pfr = pp.packageFragmentRoot;
        IPackageFragment pf = pp.packageFragment;
        try {
          IRunnableContext context = new FindTestsRunnableContext();
          if (pf != null) {
            result.addAll(Arrays.asList(
                TestSearchEngine.findTests(context, new Object[] { pf }, filter)));
          } else if (pfr != null) {
            result.addAll(Arrays.asList(
                TestSearchEngine.findTests(context, new Object[] { pfr }, filter)));
          } else {
            result.addAll(Arrays.asList(
                TestSearchEngine.findTests(context, new Object[] { pp.getProject() }, filter)));
          }
        }
        catch(InvocationTargetException ex) {
          ex.printStackTrace();
        }
        catch(InterruptedException ex) {
          // ignore
        }
      }
    }

    return result;
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
  public static List<JavaElement> getSelectedJavaElements() {
    return getSelectedJavaElements(
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());
  }

  public static List<JavaElement> getSelectedJavaElements(IWorkbenchPage page) {
    List<JavaElement> result = new ArrayList<>();
    ISelection selection = page.getSelection();

    if (selection instanceof TreeSelection) {
      //
      // If we have a selection, extract the Java information from it
      //
      TreeSelection sel = (TreeSelection) selection;
      for (Iterator it = sel.iterator(); it.hasNext();) {
        result.add(convertToJavaElement(it.next()));
      }
    } else {
      //
      // No selection, extract the Java information from the current editor, if applicable
      //
      IEditorReference[] editors = page.getEditorReferences();
//        workbench.getActiveWorkbenchWindow().getActivePage().getEditorReferences();
      for (IEditorReference ref : editors) {
        IEditorPart editor = ref.getEditor(false);
        if (editor != null) {
          ITypeRoot root = JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
          if (root != null && root.getElementType() == IJavaElement.COMPILATION_UNIT) {
            result.add(convertToJavaElement(root));
          }
        }
      }

    }

    return result;
  }

  private static JavaElement convertToJavaElement(Object element) {
    JavaElement result = new JavaElement();
    if (element instanceof IFile) {
      IJavaElement je = JavaCore.create((IFile) element);
      if (je instanceof ICompilationUnit) {
        result.compilationUnit = (ICompilationUnit) je;
      }
    }
    else if (element instanceof ICompilationUnit) {
      result.compilationUnit = (ICompilationUnit) element;
    } else if (element instanceof IPackageFragment) {
      result.packageFragment = (IPackageFragment) element;
    } else if (element instanceof IPackageFragmentRoot) {
      result.packageFragmentRoot = (IPackageFragmentRoot) element;
    } else if (element instanceof IJavaProject) {
      result.m_project = (IJavaProject) element;
    } else if (element instanceof IProject) {
      result.m_project = JavaCore.create((IProject) element);
    }

    // If we have a project, initialize the source folder too
    IResource resource = result.getResource();
    if (resource != null) {
      // By default, the target directory is the same as the class file
      result.sourceFolder = resource.getFullPath().removeLastSegments(1).toOSString();

      // Try to find a better target directory for the test class we're about to create
      for (IClasspathEntry entry : Utils.getSourceFolders(result.getProject())) {
        String source = entry.getPath().toOSString();
        if (source.endsWith("src/test/java")) {
          result.sourceFolder = source;
          break;
        } else if (source.contains("test")) {
          result.sourceFolder = source;
          break;
        }
        else if (resource.getFullPath().toString().startsWith(source)) {
          result.sourceFolder = source;
          break;
        }
      }
      if (result.sourceFolder.endsWith("src/main/java")) {
        result.sourceFolder = result.sourceFolder.replace("main", "test");
      }
    }

    return result;
  }

  /**
   * @return the source folders for this Java project.
   */
  public static List<IClasspathEntry> getSourceFolders(IJavaProject jp) {
    List<IClasspathEntry> result = new ArrayList<>();
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

  /**
   * Open the given file in the editor.
   */
  public static void openFile(Shell shell, final IFile javaFile, IProgressMonitor monitor) {
    monitor.setTaskName("Opening file for editing...");
  	shell.getDisplay().asyncExec(new Runnable() {
      public void run() {
  			IWorkbenchPage page =
  				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
  			try {
  				IDE.openEditor(page, javaFile, true);
  			} catch (PartInitException e) {
  			}
  		}
  	});
  	monitor.worked(1);
  }

}
