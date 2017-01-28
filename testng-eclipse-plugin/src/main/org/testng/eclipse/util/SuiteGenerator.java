package org.testng.eclipse.util;

import org.testng.reporters.XMLStringBuffer;
import org.testng.xml.LaunchSuite;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
      List<String> groupNames, Map<String, String> parameters,
      int logLevel, String workingDir) {

    if((null != groupNames) && !groupNames.isEmpty()) {
      return new GroupListSuite(projectName, packageNames, classNames, groupNames, parameters,
          logLevel, workingDir);
    }
    else if(null != packageNames && !packageNames.isEmpty()) {
      return new PackageSuite(projectName, packageNames, parameters, logLevel, workingDir);
    }
    else {
      return new ClassMethodsSuite(projectName, classNames, methodNames, parameters,
          logLevel, workingDir);
    }
  }

  public static String createSingleClassSuite(String className) {
    XMLStringBuffer xsb = new XMLStringBuffer();
    Properties p = new Properties();
    p.put("name", "Suite");
    p.put("parallel", "false");
    xsb.push("suite", p);

    p = new Properties();
    p.put("name", "Test");
    xsb.push("test", p);
    xsb.push("classes");
    p = new Properties();
    p.put("name", className);
    xsb.addEmptyElement("class", p);
    xsb.pop("classes");
    xsb.pop("test");
    xsb.pop("suite");

    return xsb.toXML();
  }
}
