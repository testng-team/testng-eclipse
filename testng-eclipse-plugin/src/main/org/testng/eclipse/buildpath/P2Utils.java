package org.testng.eclipse.buildpath;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.testng.eclipse.TestNGPlugin;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.simpleconfigurator.manipulator.SimpleConfiguratorManipulator;
import org.eclipse.osgi.service.resolver.VersionRange;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;


/**
 * copied from org.eclipse.jdt.internal.junit.buildpath.P2Utils
 * 
 */
class P2Utils {

  /**
   * Finds the bundle info for the given arguments.
   * <p>
   * The first match will be returned if more than one bundle matches the arguments.
   * </p>
   * 
   * @param symbolicName the symbolic name
   * @param version the bundle version
   * @param isSourceBundle <code>true</code> if it is a source bundle <code>false</code> otherwise
   * @return the bundle info or <code>null</code> if not found
   */
  public static BundleInfo findBundle(String symbolicName, Version version, boolean isSourceBundle) {
    Assert.isLegal(symbolicName != null);
    Assert.isLegal(version != null);

    return findBundle(symbolicName, new VersionRange(version, true, version, true), isSourceBundle);
  }

  /**
   * Finds the bundle info for the given arguments.
   * <p>
   * The best match (latest version) will be returned if more than one bundle matches the arguments.
   * </p>
   * 
   * @param symbolicName the symbolic name
   * @param versionRange the version range for the bundle version
   * @param isSourceBundle <code>true</code> if it is a source bundle <code>false</code> otherwise
   * @return the bundle info or <code>null</code> if not found
   */
  public static BundleInfo findBundle(String symbolicName, VersionRange versionRange, boolean isSourceBundle) {
    Assert.isLegal(symbolicName != null);
    Assert.isLegal(versionRange != null);

    SimpleConfiguratorManipulator manipulator = (SimpleConfiguratorManipulator) TestNGPlugin
        .getDefault().getService(SimpleConfiguratorManipulator.class.getName());
    if (manipulator == null) {
      return null;
    }

    BundleInfo bestMatch= null;
    Version bestVersion= null;

    // A null bundleInfoPath means load the bundles.info according to the BundleContext property "org.eclipse.equinox.simpleconfigurator.configUrl"
    String bundleInfoPath= null;
    if (isSourceBundle)
      bundleInfoPath= SimpleConfiguratorManipulator.SOURCE_INFO;

    BundleContext context= TestNGPlugin.getDefault().getBundle().getBundleContext();
    BundleInfo bundles[]= null;
    try {
      bundles= manipulator.loadConfiguration(context, bundleInfoPath);
    } catch (IOException e) {
      TestNGPlugin.log(e);
    }

    if (bundles != null) {
      for (int j= 0; j < bundles.length; j++) {
        BundleInfo bundleInfo= bundles[j];
        if (symbolicName.equals(bundleInfo.getSymbolicName())) {
          Version version= new Version(bundleInfo.getVersion());
          if (versionRange.isIncluded(version)) {
            IPath path= getBundleLocationPath(bundleInfo);
            if (path.toFile().exists()) {
              if (bestMatch == null || bestVersion.compareTo(version) < 0) {
                bestMatch= bundleInfo;
                bestVersion= version;
              }
            }
          }
        }
      }
    }

    return bestMatch;
  }

  /**
   * Returns the bundle location path.
   * 
   * @param bundleInfo the bundle info or <code>null</code>
   * @return the bundle location or <code>null</code> if it is not possible to convert to a path
   */
  public static IPath getBundleLocationPath(BundleInfo bundleInfo) {
    if (bundleInfo == null)
      return null;

    URI bundleLocation= bundleInfo.getLocation();
    if (bundleLocation == null)
      return null;

    try {
      URL localFileURL= FileLocator.toFileURL(URIUtil.toURL(bundleLocation));
      URI localFileURI= new URI(localFileURL.toExternalForm());
      return new Path(localFileURI.getPath());
    } catch (IOException e) {
      TestNGPlugin.log(e);
      return null;
    } catch (URISyntaxException e) {
      TestNGPlugin.log(e);
      return null;
    }
  }

}
