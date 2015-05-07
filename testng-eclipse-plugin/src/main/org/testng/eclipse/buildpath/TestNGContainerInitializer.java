package org.testng.eclipse.buildpath;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class TestNGContainerInitializer extends ClasspathContainerInitializer {

  public static final String TESTNG_CONTAINER_ID = "org.testng.TESTNG_CONTAINER"; //$NON-NLS-1$

  public final static IPath TESTNG_PATH = new Path(TESTNG_CONTAINER_ID);

  private static class TestNGContainer implements IClasspathContainer {

    private final IClasspathEntry[] fEntries;

    private final IPath fPath;

    public TestNGContainer(IPath path, IClasspathEntry[] entries) {
      fPath = path;
      fEntries = entries;
    }

    public IClasspathEntry[] getClasspathEntries() {
      return fEntries;
    }

    public String getDescription() {
      return "TestNG"; //$NON-NLS-1$
    }

    public int getKind() {
      return IClasspathContainer.K_APPLICATION;
    }

    public IPath getPath() {
      return fPath;
    }

  }

  // @Override
  public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
    if (isValidTestNGContainerPath(containerPath)) {
      IClasspathEntry[] entries = BuildPathSupport.getTestNGLibraryEntries();
      TestNGContainer testNGContainer = new TestNGContainer(containerPath, entries);
      JavaCore.setClasspathContainer(containerPath,
                                     new IJavaProject[] { project },
                                     new IClasspathContainer[] { testNGContainer },
                                     null);
    }

  }

  private static boolean isValidTestNGContainerPath(IPath path) {
    return (path != null) && (path.segmentCount() == 1) && TESTNG_CONTAINER_ID.equals(path.segment(0));
  }

  // @Override
  public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
    return true;
  }

  // @Override
  public void requestClasspathContainerUpdate(IPath containerPath,
                                              IJavaProject project,
                                              IClasspathContainer containerSuggestion)
  throws CoreException {
    JavaCore.setClasspathContainer(containerPath,
                                   new IJavaProject[] { project },
                                   new IClasspathContainer[] { containerSuggestion },
                                   null);
  }

  // @Override
  public Object getComparisonID(IPath containerPath, IJavaProject project) {
    return containerPath;
  }
}
