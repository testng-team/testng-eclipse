package org.testng.eclipse.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.testng.reporters.XMLStringBuffer;


/**
 * This class/interface 
 */
abstract public class ListSuite extends CustomSuite {
  protected Collection m_packageNames;
  protected Collection m_classNames;
  protected Collection m_groupNames;
  protected int m_logLevel;

  public ListSuite(final String projectName,
                   final String suiteName,
                   final Collection packageNames,
                   final Collection classNames,
                   final Collection groupNames,
                   final Map parameters,
                   final String annotationType,
                   final int logLevel) {
    super(projectName, suiteName, parameters, annotationType);
    
    m_packageNames= packageNames;
    m_classNames= classNames;
    m_groupNames= groupNames;
    m_logLevel= logLevel;
  }
  
  abstract protected String getTestName();
  
  protected void initContentBuffer(XMLStringBuffer suiteBuffer) {
    Properties testAttrs= new Properties();
    testAttrs.setProperty("name", getTestName());
    if(m_annotationType != null) {
      testAttrs.setProperty("annotations", m_annotationType);
    }
    testAttrs.setProperty("verbose", String.valueOf(m_logLevel));

    suiteBuffer.push("test", testAttrs);

    if(null != m_groupNames) {
      suiteBuffer.push("groups");
      suiteBuffer.push("run");

      for(Iterator it = m_groupNames.iterator(); it.hasNext(); ) {
        String groupName= (String) it.next();
        Properties includeAttrs= new Properties();
        includeAttrs.setProperty("name", groupName);
        suiteBuffer.addEmptyElement("include", includeAttrs);
      }

      suiteBuffer.pop("run");
      suiteBuffer.pop("groups");
    }

    // packages belongs to suite according to the latest DTD
    if((m_packageNames != null) && (m_packageNames.size() > 0)) {
      suiteBuffer.push("packages");

      for(Iterator it = m_packageNames.iterator(); it.hasNext(); ) {
        String packageName= (String) it.next();
        Properties packageAttrs= new Properties();
        packageAttrs.setProperty("name", packageName);
        suiteBuffer.addEmptyElement("package", packageAttrs);
      }

      suiteBuffer.pop("packages");
    }
    
    if((m_classNames != null) && (m_classNames.size() > 0)) {
      suiteBuffer.push("classes");

      for(Iterator it = m_classNames.iterator(); it.hasNext(); ) {
        String className = (String) it.next();
        Properties classAttrs= new Properties();
        classAttrs.setProperty("name", className);
        suiteBuffer.addEmptyElement("class", classAttrs);
      }

      suiteBuffer.pop("classes");
    }
    
    suiteBuffer.pop("test");
  }
}

class ClassListSuite extends ListSuite {

  public ClassListSuite(final String projectName,
                        final Collection packageNames,
                        final Collection classNames,
                        final Collection groupNames,
                        final Map parameters,
                        final String annotationType,
                        final int logLevel) {
    super(projectName, projectName, packageNames, classNames, groupNames, parameters, annotationType, logLevel);
  }

  protected String getTestName() {
    return m_classNames.size() == 1 ? (String) m_classNames.iterator().next() : "classes";
  }
}

class GroupListSuite extends ListSuite {
  public GroupListSuite(final String projectName,
                        final Collection packageNames,
                        final Collection classNames,
                        final Collection groupNames,
                        final Map parameters,
                        final String annotationType,
                        final int logLevel) {
    super(projectName, "dummy" /*replaced later*/, packageNames, classNames, groupNames, parameters, annotationType, logLevel);

    // real suite name
    StringBuffer buf= new StringBuffer("Suite by groups [");
    for(Iterator it= groupNames.iterator(); it.hasNext(); ) {
      buf.append(it.next());
      if(it.hasNext()) {
        buf.append(",");
      }
    }
    buf.append("]");
    m_suiteName= buf.toString();
  }

  protected String getTestName() {
    return "groups";
  }
}

class PackageSuite extends ListSuite {
  public PackageSuite(final String projectName,
                        final Collection packageNames,
                        final Collection classNames,
                        final Collection groupNames,
                        final Map parameters,
                        final String annotationType,
                        final int logLevel) {
    super(projectName, projectName, packageNames, classNames, groupNames, parameters, annotationType, logLevel);
  }

  protected String getTestName() {
    return m_packageNames.size() == 1 ? (String) m_packageNames.iterator().next() : "packages";
  }
}