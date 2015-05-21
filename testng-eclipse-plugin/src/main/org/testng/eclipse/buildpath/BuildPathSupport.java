package org.testng.eclipse.buildpath;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Bundle;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.ResourceUtil;

public class BuildPathSupport {

  private static IPath getBundleLocation() {
    Bundle bundle = TestNGPlugin.getDefault().getBundle();
    if (bundle == null) {
      return null;
    }
    URL local;
    try {
      local = getLocalURL(bundle.getEntry("/")); //$NON-NLS-1$
    }
    catch (IOException e) {
      return null;
    }
    String fullPath = new File(local.getPath()).getAbsolutePath();

    return Path.fromOSString(fullPath);
  }

  private static URL getLocalURL(URL url) throws IOException {
    // 3.2 only: FileLocator.toFileURL(url);
    return Platform.asLocalURL(url);
  }

  public static IClasspathEntry getTestNGClasspathEntry() {
    return JavaCore.newContainerEntry(TestNGContainerInitializer.TESTNG_PATH);
  }

  /**
   *
   * @return the <code>IClasspathEntry</code> array which contains the testng jar itself and its dependencies, e.g. jcommander
   */
  public static IClasspathEntry[] getTestNGLibraryEntries() {
    List<IClasspathEntry> result = new ArrayList<IClasspathEntry>();

    IPath jarLocation = getBundleLocation().append(ResourceUtil.getString("TestNG.library")); //$NON-NLS-1$
    IPath srcLocation = getBundleLocation().append(ResourceUtil.getString("TestNG.sources")); //$NON-NLS-1$
    result.add(JavaCore.newLibraryEntry(jarLocation, srcLocation, null));

    jarLocation = getBundleLocation().append(ResourceUtil.getString("Jcommander.library")); //$NON-NLS-1$
    result.add(JavaCore.newLibraryEntry(jarLocation, null, null));

    jarLocation = getBundleLocation().append(ResourceUtil.getString("Bsh.library")); //$NON-NLS-1$
    result.add(JavaCore.newLibraryEntry(jarLocation, null, null));

    jarLocation = getBundleLocation().append(ResourceUtil.getString("Snakeyaml.library")); //$NON-NLS-1$
    result.add(JavaCore.newLibraryEntry(jarLocation, null, null));

    return result.toArray(new IClasspathEntry[result.size()]);
  }

  public static boolean projectContainsClasspathEntry(IJavaProject project, IClasspathEntry entry) throws JavaModelException {
    IClasspathEntry[] oldEntries = project.getRawClasspath();
    for (int i = 0; i < oldEntries.length; i++) {
      if (oldEntries[i].equals(entry)) {
        return true;
      }
    }

    return false;
  }

}
