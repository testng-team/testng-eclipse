package org.testng.eclipse.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.debug.ui.threadgroups.JavaDebugTargetProxy;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.collections.Lists;
import org.testng.eclipse.collections.Maps;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.Utils;

import java.util.List;
import java.util.Map;

/**
 * A composite change that gathers all the changes needs to convert the current
 * selection (project or package) from JUnit to TestNG. This composite change
 * creates one change per class to convert.
 *
 * @author Cedric Beust <cedric@beust.com>
 *
 */
public class ConvertFromJUnitCompositeChange extends CompositeChange {

  private IProgressMonitor m_pm;
  private IWorkbenchWindow m_window;
  private IWorkbenchPage m_page;

  private Map<IClasspathEntry, List<IType>> m_classes = Maps.newHashMap();

  public ConvertFromJUnitCompositeChange(IProgressMonitor pm,
      IWorkbenchWindow window, IWorkbenchPage page) {
    super("Composite change");
    m_pm = pm;
    m_window = window;
    m_page = page;
    markAsSynthetic();
    computeChanges();
  }

  private void computeChanges() {
    TestNGPlugin.asyncExec(new Runnable() {
      public void run() {
        IJavaProject javaProject = JDTUtil.getJavaProjectContext();
        if (javaProject == null) return;

        List<IType> types = Utils.findSelectedTypes(m_page);
        for (IType type : types) {
          for (IClasspathEntry entry : Utils.getSourceFolders(javaProject)) {
            String source = entry.getPath().toOSString();
            if (type.getResource().getFullPath().toOSString().startsWith(source)) {
              List<IType> l = m_classes.get(entry);
              if (l == null) {
                l = Lists.newArrayList();
                m_classes.put(entry, l);
              }
              l.add(type);
            }
          }
        }

        // Now create one composite change per source folder
        for (Map.Entry<IClasspathEntry, List<IType>> entry : m_classes.entrySet()) {
          add(new SourceFolderChange(entry.getKey(), entry.getValue()));
        }
        
      }
    });
  }

  @Override
  public String getName() {
    return "src/test/java";
  }

  @Override
  public void initializeValidationData(IProgressMonitor pm) {
  }

  @Override
  public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
      OperationCanceledException {
    return null;
  }

  @Override
  public Object getModifiedElement() {
    return null;
  }

}
