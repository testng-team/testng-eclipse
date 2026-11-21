package org.testng.eclipse.launch;

import org.osgi.framework.Version;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestNGLaunchConfigurationDelegateTest {

  @DataProvider(name = "javaVersions")
  public Object[][] javaVersions() {
    return new Object[][] {
        // legacy format with update suffix (#583)
        { "1.8.0_201", new Version(1, 8, 0) },
        { "1.7.0", new Version(1, 7, 0) },
        // JEP 223 with more than three numeric elements (#587)
        { "17.0.21.0.3", new Version(17, 0, 21) },
        { "21.0.13.0.3", new Version(21, 0, 13) },
        { "25.0.5.0.3", new Version(25, 0, 5) },
        // short and suffixed forms
        { "21", new Version(21, 0, 0) },
        { "17.0", new Version(17, 0, 0) },
        { "21-ea", new Version(21, 0, 0) },
        { "11.0.2+9", new Version(11, 0, 2) },
        { "17.0.1-internal", new Version(17, 0, 1) },
    };
  }

  @Test(dataProvider = "javaVersions")
  public void parseJavaVersion(String input, Version expected) {
    Assert.assertEquals(TestNGLaunchConfigurationDelegate.parseJavaVersion(input), expected);
  }

  @Test
  public void parseJavaVersionRejectsNonNumeric() {
    Assert.assertNull(TestNGLaunchConfigurationDelegate.parseJavaVersion("abc"));
    Assert.assertNull(TestNGLaunchConfigurationDelegate.parseJavaVersion(""));
  }

  @Test
  public void parsedVersionsCompareAgainstMinimum() {
    Version min = new Version(1, 7, 0);
    Assert.assertTrue(TestNGLaunchConfigurationDelegate.parseJavaVersion("17.0.21.0.3").compareTo(min) > 0);
    Assert.assertTrue(TestNGLaunchConfigurationDelegate.parseJavaVersion("1.6.0_45").compareTo(min) < 0);
  }
}
