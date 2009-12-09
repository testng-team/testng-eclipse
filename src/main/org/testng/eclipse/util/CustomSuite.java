package org.testng.eclipse.util;


import org.testng.TestNG;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.collections.Lists;
import org.testng.eclipse.collections.Maps;
import org.testng.reporters.XMLStringBuffer;
import org.testng.xml.LaunchSuite;
import org.testng.xml.Parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Base class used by classes that generate XML suite files.
 *
 * @author cbeust
 */
abstract public class CustomSuite extends LaunchSuite {
  protected String m_projectName;
  protected String m_suiteName;
  protected String m_annotationType;
  protected Map<String, String> m_parameters;
  protected int m_logLevel;

  private XMLStringBuffer m_suiteBuffer;
  private List<String> m_suiteFiles;

  public CustomSuite(List<String> suiteFiles, String projectName, int logLevel) {
    super(false);
    init(suiteFiles, projectName, "Suites", Collections.<String, String>emptyMap(),
        TestNG.JDK_ANNOTATION_TYPE, logLevel);
  }

  public CustomSuite(final String projectName,
                     final String suiteName,
                     final Map<String, String> parameters,
                     final String annotationType,
                     final int logLevel) {
    super(true);
    init(Collections.<String>emptyList(), projectName, suiteName, parameters, annotationType,
        logLevel);
  }

  private void init(List<String> suiteFiles, String projectName,
      final String suiteName,
      final Map<String, String> parameters,
      final String annotationType,
      final int logLevel) {

    m_suiteFiles = suiteFiles;
    m_projectName= projectName;
    m_suiteName= suiteName;
    m_parameters= parameters;

    // TODO: move to AnnotationTypeEnum ?
    if("1.4".equals(annotationType) || TestNG.JAVADOC_ANNOTATION_TYPE.equals(annotationType)) {
      m_annotationType= TestNG.JAVADOC_ANNOTATION_TYPE;
    }
    else {
      m_annotationType= TestNG.JDK_ANNOTATION_TYPE;
    }
    
    m_logLevel= logLevel;
    
  }
  abstract protected String getTestName();

  protected String getSuiteName() {
    return m_suiteName;
  }

  protected XMLStringBuffer createContentBuffer() {
    XMLStringBuffer suiteBuffer= new XMLStringBuffer(""); //$NON-NLS-1$
    suiteBuffer.setDocType("suite SYSTEM \"" + Parser.TESTNG_DTD_URL + "\"");

    Properties attrs= new Properties();
    attrs.setProperty("name", getSuiteName());
    attrs.setProperty("parallel", TestNGPlugin.getPluginPreferenceStore()
        .getParallel(m_projectName, false /* not project only */));
    suiteBuffer.push("suite", attrs);

    if (m_parameters != null) {
      for (Map.Entry<String, String> entry : m_parameters.entrySet()) {
        Properties paramAttrs= new Properties();
        paramAttrs.setProperty("name", entry.getKey());
        paramAttrs.setProperty("value", entry.getValue());
        suiteBuffer.addEmptyElement("parameter", paramAttrs);
      }
    }

    if (m_suiteFiles.size() > 0) {
      suiteBuffer.push("suite-files");
      for (String suite : m_suiteFiles) {
        Properties s = new Properties();
        s.put("path", suite);
        suiteBuffer.addEmptyElement("suite-file", s);
      }
      suiteBuffer.pop("suite-files");
    }
    initContentBuffer(suiteBuffer);

    suiteBuffer.pop("suite");

    return suiteBuffer;
  }

  private XMLStringBuffer getSuiteBuffer() {
    if(null == m_suiteBuffer) {
      m_suiteBuffer= createContentBuffer();
    }

    return m_suiteBuffer;
  }

  public String getFileName() {
    return "temp-testng-customsuite.xml";
  }

  /** 
   * Generate the current suite to a file.
   * @see org.testng.xml.LaunchSuite#save(java.io.File)
   */
  @Override
  public File save(File directory) {
    final File suiteFile= new File(directory, getFileName());

    saveSuiteContent(suiteFile, getSuiteBuffer());

    return suiteFile;
  }

  protected void saveSuiteContent(final File file, final XMLStringBuffer content) {
    FileWriter fw= null;
    BufferedWriter bw= null;
    try {
      fw= new FileWriter(file);
      bw= new BufferedWriter(fw);
      bw.write(content.getStringBuffer().toString());
      bw.flush();
    }
    catch(IOException ioe) {
    }
    finally {
      if(null != bw) {
        try {
          bw.close();
        }
        catch(IOException ioe) {
        }
      }
      if(null != fw) {
        try {
          fw.close();
        }
        catch(IOException ioe) {
        }
      }
    }
  }

  protected void initContentBuffer(XMLStringBuffer suiteBuffer) {
    Properties testAttrs= new Properties();
    testAttrs.setProperty("name", getTestName());
    if(m_annotationType != null) {
      testAttrs.setProperty("annotations", m_annotationType);
    }
    testAttrs.setProperty("verbose", String.valueOf(m_logLevel));

    suiteBuffer.push("test", testAttrs);

    groupsElement(suiteBuffer);

    packagesElement(suiteBuffer);

    classesElement(suiteBuffer);

    suiteBuffer.pop("test");
  }

  /**
   * Override to generate the groups element.
   */
  protected void groupsElement(XMLStringBuffer suiteBuffer) {
  }

  /**
   * Override to generate the packages element.
   */
  protected void packagesElement(XMLStringBuffer suiteBuffer) {
  }

  /**
   * Override to generate the classes element.
   */
  protected void classesElement(XMLStringBuffer suiteBuffer) {
  }

  /**
   * An utility method that generates the groups element using the passed in 
   * collection of names (order of group names ia not important).
   */
  protected void generateDefaultGroupsElement(XMLStringBuffer suiteBuffer, Collection /*<String>*/ groupNames) {
    if((null == groupNames) || groupNames.isEmpty()) {
      return;
    }

    suiteBuffer.push("groups");
    suiteBuffer.push("run");

    for(Iterator it= groupNames.iterator(); it.hasNext();) {
      String groupName= (String) it.next();
      Properties includeAttrs= new Properties();
      includeAttrs.setProperty("name", groupName);
      suiteBuffer.addEmptyElement("include", includeAttrs);
    }

    suiteBuffer.pop("run");
    suiteBuffer.pop("groups");
  }

  protected void generateDefaultPackagesElement(XMLStringBuffer suiteBuffer,
                                                List<String> packageNames) {
    if((null == packageNames) || packageNames.isEmpty()) {
      return;
    }

    suiteBuffer.push("packages");

    for (String packageName : packageNames) {
      Properties packageAttrs= new Properties();
      packageAttrs.setProperty("name", packageName);
      suiteBuffer.addEmptyElement("package", packageAttrs);
    }

    suiteBuffer.pop("packages");
  }

  protected void generateDefaultClassesElement(XMLStringBuffer suiteBuffer,
                                               List<String> classNames) {
    if((null == classNames) || classNames.isEmpty()) {
      return;
    }

    suiteBuffer.push("classes");

    for (String className : classNames) {
      Properties classAttrs= new Properties();
      classAttrs.setProperty("name", className);
      suiteBuffer.addEmptyElement("class", classAttrs);
    }

    suiteBuffer.pop("classes");
  }
}

/**
 * A type based generator. If specific method filtering applies to types
 * then <code></code> should be used.
 */
class ClassMethodsSuite extends CustomSuite {
  protected List<String> m_classNames;
  protected Map<String, List<String>> m_classMethods;
  protected boolean m_useMethods;
  
  public ClassMethodsSuite(final String projectName,
                           final List<String> classNames,
                           final Map<String, List<String>> classMethods,
                           final Map<String, String> parameters,
                           final String annotationType,
                           final int logLevel) {
    super(projectName, projectName, parameters, annotationType, logLevel);
    m_classNames = classNames;
    m_classMethods = sanitize(classMethods);
    if(m_useMethods) {
      m_classNames = new ArrayList<String>(m_classMethods.keySet());
    }
  }

  private Map<String, List<String>> sanitize(Map<String, List<String>> classMethods) {
    Map<String, List<String>> result = Maps.newHashMap();
    for (Map.Entry<String, List<String>> entry : classMethods.entrySet()) {
      String clsName= entry.getKey();
      List<String> methods= entry.getValue();
      if(null == methods || methods.isEmpty()) {
        result.put(clsName, null);
      }
      else {
        List<String> methodNames= Lists.newArrayList();
        for (String meth : methods) {
          if(null != meth && !"".equals(meth)) {
            methodNames.add(meth);
          }
        }
        if(methodNames.isEmpty()) {
          result.put(clsName, null);
        }
        else {
          result.put(clsName, methodNames);
          m_useMethods= true;
        }
      }
    }
    
    return result;
  }

  @Override
  protected String getTestName() {
    return m_classNames.size() == 1 ? (String) m_classNames.iterator().next() : "classes";
  }

  @Override
  protected void classesElement(XMLStringBuffer suiteBuffer) {
    if(m_useMethods) {
      generateClassesWithMethodsElement(suiteBuffer);
    }
    else {
      generateDefaultClassesElement(suiteBuffer, m_classNames);
    }
  }
  
  protected void generateClassesWithMethodsElement(XMLStringBuffer suiteBuffer) {
    suiteBuffer.push("classes");

    for (Map.Entry<String, List<String>> entry : m_classMethods.entrySet()) {
      String className = entry.getKey();
      Properties classAttrs= new Properties();
      classAttrs.setProperty("name", className);
      
      List<String> methodNames = entry.getValue();
      if(null == methodNames) {
        suiteBuffer.addEmptyElement("class", classAttrs);
      }
      else {
        suiteBuffer.push("class", classAttrs);
        suiteBuffer.push("methods");
        for (String name : methodNames) {
          Properties methodAttrs = new Properties();
          methodAttrs.setProperty("name", name);
          suiteBuffer.addEmptyElement("include", methodAttrs);
        }
        
        suiteBuffer.pop("methods");
        suiteBuffer.pop("class");
      }
    }

    suiteBuffer.pop("classes"); 
  }
}

class GroupListSuite extends CustomSuite {
  protected List<String> m_packageNames;
  protected List<String> m_classNames;
  protected List<String> m_groupNames;
  protected StringBuffer m_testName= new StringBuffer("GRP-");
  
  public GroupListSuite(final String projectName,
                        final List<String> packageNames,
                        final List<String> classNames,
                        final List<String> groupNames,
                        final Map<String, String> parameters,
                        final String annotationType,
                        final int logLevel) {
    super(projectName, projectName + " by groups", parameters, annotationType, logLevel);

    m_packageNames= packageNames;
    m_classNames= classNames;
    m_groupNames= groupNames;

    for(Iterator<String> it = groupNames.iterator(); it.hasNext(); ) {
      m_testName.append(it.next());
      if(it.hasNext()) {
        m_testName.append(",");
      }
    }
  }

  @Override
  protected String getTestName() {
    return m_testName.toString();
  }

  @Override
  protected void classesElement(XMLStringBuffer suiteBuffer) {
    generateDefaultClassesElement(suiteBuffer, m_classNames);
  }

  @Override
  protected void groupsElement(XMLStringBuffer suiteBuffer) {
    generateDefaultGroupsElement(suiteBuffer, m_groupNames);
  }

  @Override
  protected void packagesElement(XMLStringBuffer suiteBuffer) {
    generateDefaultPackagesElement(suiteBuffer, m_packageNames);
  }
}

/**
 * A package based generator.
 */
class PackageSuite extends CustomSuite {
  protected List<String> m_packageNames;
  
  public PackageSuite(final String projectName,
                      final List<String> packageNames,
                      final Map<String, String> parameters,
                      final String annotationType,
                      final int logLevel) {
    super(projectName, projectName + " by packages", parameters, annotationType, logLevel);
    m_packageNames= packageNames;
  }

  @Override
  protected String getTestName() {
    return m_packageNames.size() == 1 ? (String) m_packageNames.iterator().next() : m_projectName + " by packages";
  }

  @Override
  protected void packagesElement(XMLStringBuffer suiteBuffer) {
    generateDefaultPackagesElement(suiteBuffer, m_packageNames);
  }
}

/**
 * A suite file that contains several suite files.
 * 
 * @author cbeust
 */
class SuiteSuite extends CustomSuite {
  public SuiteSuite(List<String> suiteFiles, String project) {
    super(suiteFiles, project, 1);
  }

  @Override
  protected String getTestName() {
    return "Suite-of-suites";
  }
}