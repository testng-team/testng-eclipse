package org.testng.eclipse.util;


import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.testng.xml.LaunchSuite;

/**
 * Factory to create custom suites.
 * @author Hani Suleiman
 *         Date: Jul 25, 2005
 *         Time: 1:12:18 PM
 */
public class SuiteGenerator {
  public static LaunchSuite createProxiedXmlSuite(final File xmlSuitePath) {
    return new LaunchSuite.ExistingSuite(xmlSuitePath);
  }

  public static LaunchSuite createCustomizedSuite(final String projectName,
                                                  final Collection packageNames,
                                                  final Collection classNames,
                                                  final Collection methodNames,
                                                  final Collection groupNames,
                                                  final Map parameters,
                                                  final String annotationType,
                                                  final int logLevel) {
    if((null != groupNames) && !groupNames.isEmpty()) {
      return new GroupListSuite(projectName,
                                packageNames, /* is emtpy */
                                classNames,
                                groupNames,
                                parameters, /* is empty */
                                annotationType,
                                logLevel);
    }
    else if(null != methodNames && methodNames.size() > 0) {
      return new MethodsSuite(projectName,
                              (String) classNames.iterator().next(),
                              methodNames,
                              parameters,
                              annotationType,
                              logLevel);
    }
    else if(null != packageNames && !packageNames.isEmpty()) {
      return new PackageSuite(projectName,
                              packageNames, 
                              classNames, /* is empty */
                              groupNames, /* is empty */
                              parameters,
                              annotationType,
                              logLevel);
    }
    else  {
      return new ClassListSuite(projectName,
                                packageNames, /* is empty */
                                classNames,
                                groupNames, /* is empty */
                                parameters,
                                annotationType,
                                logLevel);
    }
  }
}
