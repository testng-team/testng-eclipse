package org.testng.eclipse.refactoring;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ui.IWorkbenchPage;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.Utils;

/**
 * A composite change that gathers all the changes needs to convert the current
 * selection (project or package) from JUnit to TestNG. This composite change
 * creates one SourceFolderChange per source folder found.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class ConvertFromJUnitCompositeChange extends CompositeChange {

  private IProgressMonitor m_pm;
  private IWorkbenchPage m_page;

  /**
   * Map of <source folder, Set<IResource>>.
   * Note: using a set here since the same resource can appear several times for a different
   * IType (if the class has nested classes) and we only want to perform the refactoring
   * once per file.
   */
  private Map<String, Set<IResource>> m_classes = new HashMap<>();

  public ConvertFromJUnitCompositeChange(IProgressMonitor pm, IWorkbenchPage page) {
    super("Composite change");
    m_pm = pm;
    m_page = page;
    markAsSynthetic();
    computeChanges();
  }

  private void computeChanges() {
    TestNGPlugin.asyncExec(new Runnable() {
      public void run() {
        IJavaProject javaProject = JDTUtil.getJavaProjectContext();
        if (javaProject == null) return;

        List<IType> types = Utils.findSelectedTypes(m_page, Utils.CONVERSION_FILTER);
        m_pm.beginTask("Finding test classes", types.size());
        for (IType type : types) {
          for (IClasspathEntry entry : Utils.getSourceFolders(javaProject)) {
            String sourceFolder = entry.getPath().toOSString();
            IResource resource = type.getResource();
            if (resource.getFullPath().toOSString().contains(sourceFolder)) {
              Set<IResource> l = m_classes.get(sourceFolder);
              if (l == null) {
                l = new HashSet<>();
                m_classes.put(sourceFolder, l);
              }
              l.add(resource);
              m_pm.worked(1);
            }
          }
        }

        // Now create one composite change per source folder
        for (Map.Entry<String, Set<IResource>> entry : m_classes.entrySet()) {
          add(new SourceFolderChange(entry.getKey(), entry.getValue()));
        }

        m_pm.done();
      }
    });
  }

  @Override
  public String getName() {
    return "Convert to TestNG";
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
