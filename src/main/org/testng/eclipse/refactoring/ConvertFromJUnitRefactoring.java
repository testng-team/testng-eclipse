package org.testng.eclipse.refactoring;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.components.Filters.ITypeFilter;
import org.testng.eclipse.util.TestSearchEngine;
import org.testng.eclipse.util.Utils;

import java.lang.reflect.InvocationTargetException;

public class ConvertFromJUnitRefactoring extends Refactoring {
  private RefactoringStatus m_status = new RefactoringStatus();
  private IWorkbenchWindow m_window;
  private IWorkbenchPage m_page;

  public ConvertFromJUnitRefactoring(IWorkbenchWindow window, IWorkbenchPage page,
      RefactoringStatus status) {
    m_window = window;
    m_page = page;
  }

  @Override
  public String getName() {
    return "Convert from JUnit";
  }

  @Override
  public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    return m_status;
  }

  @Override
  public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
      throws CoreException, OperationCanceledException {
    return m_status;
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException,
      OperationCanceledException
  {
    TestNGPlugin.asyncExec(new Runnable() {
      public void run() {
        IRunnableContext context = new FindTestsRunnableContext();
        Object selection = Utils.getSelectedProjectOrPackage(m_page);
        IJavaProject project = (IJavaProject)
            (selection instanceof IJavaProject ? selection : null);
        IPackageFragmentRoot pfr = (IPackageFragmentRoot)
            (selection instanceof IPackageFragmentRoot ? selection : null);
        try {
          ITypeFilter filter = new ITypeFilter() {

            public boolean accept(IType type) {
              IResource obj = (IResource) type.getAdapter(IResource.class);
              IContainer container = null;
              if (obj instanceof IContainer) {
                container = (IContainer) obj;
              } else if (obj != null) {
                container = ((IResource) obj).getParent();
              }
              if (container != null) {
                String sourcePath = container.getFullPath().toString();
                return sourcePath.contains("/test/");
              } else {
                return false;
              }
            }
            
          };
          IType[] types = TestSearchEngine.findTests(context, new Object[] { project }, filter);
          for (IType type : types) {
            System.out.println("  type:" + type.getFullyQualifiedName());
          }
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        
      }
    });
    return new ConvertFromJUnitChange(pm, m_window, m_page);
  }

}
