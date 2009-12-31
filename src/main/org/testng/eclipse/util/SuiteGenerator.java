package org.testng.eclipse.util;

import org.testng.xml.LaunchSuite;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Factory to create custom suites.
 * 
 * @author Hani Suleiman
 */
public class SuiteGenerator {
  public static LaunchSuite createProxiedXmlSuite(final File xmlSuitePath) {
    return new LaunchSuite.ExistingSuite(xmlSuitePath);
  }

  public static LaunchSuite createCustomizedSuite(String projectName,
      List<String> packageNames, List<String> classNames, Map<String, List<String>> methodNames,
      List<String> groupNames, Map<String, String> parameters, String annotationType,
      int logLevel) {

    if((null != groupNames) && !groupNames.isEmpty()) {
      return new GroupListSuite(projectName, packageNames, classNames, groupNames, parameters,
          annotationType, logLevel);
    }
    else if(null != packageNames && !packageNames.isEmpty()) {
      return new PackageSuite(projectName, packageNames, parameters, annotationType, logLevel);
    }
    else {
      return new ClassMethodsSuite(projectName, classNames, methodNames, parameters,
          annotationType, logLevel);
    }
  }
}
