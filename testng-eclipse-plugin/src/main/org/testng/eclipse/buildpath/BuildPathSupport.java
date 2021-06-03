package org.testng.eclipse.buildpath;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.ResourceUtil;

public class BuildPathSupport {

  private static final PluginDescription TESTNG_PLUGIN_DESC = new PluginDescription("org.testng", new VersionRange("[6.0, 8.0)"), null, null, "org.testng.source", null);
  private static final PluginDescription JCOMMANDER_PLUGIN_DESC = new PluginDescription("com.beust.jcommander", new VersionRange("[1.5, 2.0)"), null, null, "com.beust.jcommander.source", null);
  private static final PluginDescription JQUERY_PLUGIN_DESC = new PluginDescription("org.webjars.jquery", new VersionRange("[3.5.1, 4.0)"), null, null, "org.webjars.jquery.source", null);
  private static final PluginDescription BSH_PLUGIN_DESC = new PluginDescription("org.apache-extras.beanshell.bsh", new VersionRange("[2.0, 2.1)"), null, null, "org.apache-extras.beanshell.bsh.source", null);
  private static final PluginDescription YAML_PLUGIN_DESC = new PluginDescription("org.yaml.snakeyaml", new VersionRange("[1.0, 2.0)"), null, null, "org.yaml.snakeyaml", null);
  private static IClasspathEntry[] TESTNG_LIB_ENTRIES_CACHE;

  private static IPath getBundleLocation() {
    Bundle bundle = TestNGPlugin.getDefault().getBundle();
    return getBundleLocation(bundle);
  }

  private static IPath getBundleLocation(Bundle bundle) {
    if (bundle == null) {
      return null;
    }
    URL local;
    try {
      local = FileLocator.toFileURL(bundle.getEntry("/")); //$NON-NLS-1$
    }
    catch (IOException e) {
      return null;
    }
    String fullPath = new File(local.getPath()).getAbsolutePath();

    return Path.fromOSString(fullPath);
  }

  public static IClasspathEntry getTestNGClasspathEntry() {
    return JavaCore.newContainerEntry(TestNGContainerInitializer.TESTNG_PATH);
  }

  /**
   *
   * @return the <code>IClasspathEntry</code> array which contains the testng jar itself and its dependencies, e.g. jcommander
   */
  public static IClasspathEntry[] getTestNGLibraryEntries() {
    if (TESTNG_LIB_ENTRIES_CACHE == null) {
      List<IClasspathEntry> result = new ArrayList<IClasspathEntry>();

      result.add(TESTNG_PLUGIN_DESC.getLibraryEntry());
      result.add(JCOMMANDER_PLUGIN_DESC.getLibraryEntry());
      IClasspathEntry bshCpEnt = BSH_PLUGIN_DESC.getLibraryEntry();
      if (bshCpEnt != null) {
        result.add(bshCpEnt);
      }

      IClasspathEntry yamlCpEnt = YAML_PLUGIN_DESC.getLibraryEntry();
      if (yamlCpEnt != null) {
        result.add(yamlCpEnt);
      }

      IClasspathEntry jqueryCpEnt = JQUERY_PLUGIN_DESC.getLibraryEntry();
      if (jqueryCpEnt != null) {
        result.add(jqueryCpEnt);
      }

      TESTNG_LIB_ENTRIES_CACHE = result.toArray(new IClasspathEntry[result.size()]);
    }
    return TESTNG_LIB_ENTRIES_CACHE;
  }

  public static IPath getBundleFile(String relativePath) {
    return getBundleLocation().append(relativePath);
  }

  public static boolean projectContainsClasspathEntry(IJavaProject project, IClasspathEntry entry) throws JavaModelException {
    IClasspathEntry[] oldEntries = project.getRawClasspath();
    for (IClasspathEntry oldEntry : oldEntries) {
      if (oldEntry.equals(entry)) {
        return true;
      }
    }

    return false;
  }

  /**
   * copied from
   * org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport.JUnitPluginDescription
   *
   */
  public static class PluginDescription {

    private final String bundleId;
    private final VersionRange versionRange;
    private final String bundleRoot;
    private final String binaryImportedRoot;
    private final String sourceBundleId;
    private final String repositorySource;

    private String resolvedVersion = null;

    public PluginDescription(String bundleId, VersionRange versionRange,
        String bundleRoot, String binaryImportedRoot, String sourceBundleId,
        String repositorySource) {
      this.bundleId = bundleId;
      this.versionRange = versionRange;
      this.bundleRoot = bundleRoot;
      this.binaryImportedRoot = binaryImportedRoot;
      this.sourceBundleId = sourceBundleId;
      this.repositorySource = repositorySource;
    }

    public IPath getBundleLocation() {
      return getBundleLocation(bundleId, versionRange);
    }

    public IPath getSourceBundleLocation() {
      return getSourceLocation(getBundleLocation());
    }

    private IPath getBundleLocation(String aBundleId,
        VersionRange aVersionRange) {
      return getBundleLocation(aBundleId, aVersionRange, false);
    }

    private IPath getBundleLocation(String aBundleId,
        VersionRange aVersionRange, boolean isSourceBundle) {
      BundleInfo bundleInfo = P2Utils.findBundle(aBundleId, aVersionRange,
          isSourceBundle);
      if (bundleInfo != null) {
        resolvedVersion = bundleInfo.getVersion();
        return P2Utils.getBundleLocationPath(bundleInfo);
      } else {
        // p2's simple configurator is not available. Let's try with installed
        // bundles from the running platform.
        // Note: Source bundles are typically not available at run time!
        Bundle[] bundles = Platform.getBundles(aBundleId,
            aVersionRange.toString());
        Bundle bestMatch = null;
        if (bundles != null) {
          for (int i = 0; i < bundles.length; i++) {
            Bundle bundle = bundles[i];
            if (bestMatch == null || bundle.getState() > bestMatch.getState()) {
              bestMatch = bundle;
            }
          }
        }
        if (bestMatch != null) {
          try {
            resolvedVersion = bestMatch.getVersion().toString();
            URL rootUrl = bestMatch.getEntry("/"); //$NON-NLS-1$
            URL fileRootUrl = FileLocator.toFileURL(rootUrl);
            return new Path(fileRootUrl.getPath());
          } catch (IOException ex) {
            TestNGPlugin.log(ex);
          }
        }
      }
      return null;
    }

    public IClasspathEntry getLibraryEntry() {
      IPath bundleLocation = getBundleLocation(bundleId, versionRange);
      if (bundleLocation != null) {
        IPath bundleRootLocation = null;
        if (bundleRoot != null) {
          bundleRootLocation = getLocationIfExists(bundleLocation, bundleRoot);
        }
        if (bundleRootLocation == null && binaryImportedRoot != null) {
          bundleRootLocation = getLocationIfExists(bundleLocation,
              binaryImportedRoot);
        }
        if (bundleRootLocation == null) {
          bundleRootLocation = getBundleLocation(bundleId, versionRange);
        }

        IPath srcLocation = getSourceLocation(bundleLocation);

        return JavaCore.newLibraryEntry(bundleRootLocation, srcLocation, null,
            getAccessRules(), new IClasspathAttribute[0], false);
      }
      return null;
    }

    public IAccessRule[] getAccessRules() {
      return new IAccessRule[0];
    }

    private IPath getSourceLocation(IPath bundleLocation) {
      IPath srcLocation = null;
      if (repositorySource != null) {
        // Try source in workspace (from repository)
        srcLocation = getLocationIfExists(bundleLocation, repositorySource);
      }

      if (srcLocation == null) {
        if (bundleLocation != null) {
          // Try exact version
          Version version = new Version(resolvedVersion);
          srcLocation = getBundleLocation(sourceBundleId,
              new VersionRange(version, true, version, true), true);
        }
        if (srcLocation == null) {
          // Try version range
          srcLocation = getBundleLocation(sourceBundleId, versionRange, true);
        }
      }

      return srcLocation;
    }

    private IPath getLocationIfExists(IPath bundleLocationPath,
        final String entryInBundle) {
      IPath srcLocation = null;
      if (bundleLocationPath != null) {
        File bundleFile = bundleLocationPath.toFile();
        if (bundleFile.isDirectory()) {
          File srcFile = null;
          final int starIdx = entryInBundle.indexOf('*');
          if (starIdx != -1) {
            File[] files = bundleFile.listFiles(new FilenameFilter() {
              private String pre = entryInBundle.substring(0, starIdx);
              private String post = entryInBundle.substring(starIdx + 1);

              @Override
              public boolean accept(File dir, String name) {
                return name.startsWith(pre) && name.endsWith(post);
              }
            });
            if (files.length > 0) {
              srcFile = files[0];
            }
          }
          if (srcFile == null)
            srcFile = new File(bundleFile, entryInBundle);
          if (srcFile.exists()) {
            srcLocation = new Path(srcFile.getPath());
            if (srcFile.isDirectory()) {
              srcLocation = srcLocation.addTrailingSeparator();
            }
          }
        }
      }
      return srcLocation;
    }
  }
}
