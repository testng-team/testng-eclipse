package org.testng.eclipse.util;


import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jface.preference.IPreferenceStore;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;
import org.testng.eclipse.collections.Lists;
import org.testng.eclipse.collections.Maps;
import org.testng.internal.Utils;
import org.testng.remote.RemoteTestNG;
import org.testng.reporters.XMLStringBuffer;
import org.testng.xml.LaunchSuite;
import org.testng.xml.Parser;
import org.testng.xml.XmlMethodSelector;
import org.testng.xml.XmlSuite;
import org.xml.sax.SAXException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
  /**
   * The name of the <suite> tag used when the user is launching something else than
   * an XML file.
   */
  public static final String DEFAULT_SUITE_TAG_NAME = "Default suite";
  
  /**
   * The name of the <test> tag used when the user is launching something else than
   * an XML file.
   */
  public static final String DEFAULT_TEST_TAG_NAME = "Default test";

  protected String m_projectName;
  protected String m_suiteName;
  protected Map<String, String> m_parameters;
  protected int m_logLevel;

  private XMLStringBuffer m_suiteBuffer;
//  private List<String> m_suiteFiles;

//  public CustomSuite(List<String> suiteFiles, String projectName, int logLevel) {
//    super(false);
//    init(suiteFiles, projectName, "Suites", Collections.<String, String>emptyMap(),
//        TestNG.JDK_ANNOTATION_TYPE, logLevel);
//  }

  public CustomSuite(final String projectName,
                     final String suiteName,
                     final Map<String, String> parameters,
                     final int logLevel) {
    super(true);
    init(Collections.<String>emptyList(), projectName, suiteName, parameters,
        logLevel);
  }

  private void init(List<String> suiteFiles, String projectName,
      final String suiteName,
      final Map<String, String> parameters,
      final int logLevel) {

//    m_suiteFiles = suiteFiles;
    m_projectName= projectName;
    m_suiteName= suiteName;
    m_parameters= parameters;

    m_logLevel= logLevel;
    
  }
  abstract protected String getTestName();

  protected String getSuiteName() {
    return m_suiteName;
  }

  private void put(Properties p, String key, Object value) {
    if (value != null) p.put(key, value);
  }

  protected XMLStringBuffer createContentBuffer() {
    IPreferenceStore storage = TestNGPlugin.getDefault().getPreferenceStore();
    String xmlFile = storage.getString(TestNGPluginConstants.S_XML_TEMPLATE_FILE);
    boolean hasEclipseXmlFile = !Utils.isStringEmpty(xmlFile);
    XMLStringBuffer suiteBuffer = new XMLStringBuffer(""); //$NON-NLS-1$
    suiteBuffer.setDocType("suite SYSTEM \"" + Parser.TESTNG_DTD_URL + "\"");

    if (hasEclipseXmlFile) {
      createXmlFileFromTemplate(suiteBuffer, xmlFile);
    } else {
      createXmlFileFromParameters(suiteBuffer);
    }

    // Done with the top of the XML file, now generate the <test> elements
    initContentBuffer(suiteBuffer);

    suiteBuffer.pop("suite");

    return suiteBuffer;
  }

  private void createXmlFileFromParameters(XMLStringBuffer suiteBuffer) {
    Properties attrs= new Properties();
    attrs.setProperty("name", getSuiteName());
//    attrs.setProperty("parallel", TestNGPlugin.getPluginPreferenceStore()
//        .getParallel(m_projectName, false /* not project only */));
    suiteBuffer.push("suite", attrs);
 
    if (m_parameters != null) {
      for (Map.Entry<String, String> entry : m_parameters.entrySet()) {
        Properties paramAttrs= new Properties();
        paramAttrs.setProperty("name", entry.getKey());
        paramAttrs.setProperty("value", entry.getValue());
        suiteBuffer.addEmptyElement("parameter", paramAttrs);
      }
    }
 
 //    if (m_suiteFiles.size() > 0) {
 //      suiteBuffer.push("suite-files");
 //      for (String suite : m_suiteFiles) {
 //        Properties s = new Properties();
 //        s.put("path", suite);
 //        suiteBuffer.addEmptyElement("suite-file", s);
 //      }
 //      suiteBuffer.pop("suite-files");
 //    }
  }

  /**
   * Fill the top of the XML suiteBuffer with the top of the XML template file
   */
  private void createXmlFileFromTemplate(XMLStringBuffer suiteBuffer, String fileName) {
    try {
      Collection<XmlSuite> suites = new Parser(fileName).parse();
      if (suites.size() > 0) {
        XmlSuite s = suites.iterator().next();

        // Retrieve the <suite> attributes from the template file and transfer
        // them in the suite we are creating.
        Properties attr = new Properties();
        put(attr, "name", s.getName());
        put(attr, "junit", s.isJUnit());
        put(attr, "verbose", s.getVerbose());
        put(attr, "parallel", s.getParallel());
        put(attr, "thread-count", s.getThreadCount());
//        put(attr, "annotations", s.getAnnotations());
        put(attr, "time-out", s.getTimeOut());
        put(attr, "skipfailedinvocationcounts", s.skipFailedInvocationCounts());
        put(attr, "configfailurepolicy", s.getConfigFailurePolicy());
        put(attr, "data-provider-thread-count", s.getDataProviderThreadCount());
        put(attr, "object-factory", s.getObjectFactory());
        suiteBuffer.push("suite", attr);

        // Children of <suite>
        
        // Listeners
        if (s.getListeners().size() > 0) {
          suiteBuffer.push("listeners");
          for (String l : s.getListeners()) {
            Properties p = new Properties();
            p.put("class-name", l);
            suiteBuffer.addEmptyElement("listener", p);
          }
          suiteBuffer.pop("listeners");
        }

        // Parameters
        for (Map.Entry<String, String> parameter : s.getParameters().entrySet()) {
          Properties p = new Properties();
          p.put("name", parameter.getKey());
          p.put("value", parameter.getValue());
          suiteBuffer.addEmptyElement("parameter", p);
        }

        // Method selectors
        if (s.getMethodSelectors().size() > 0) {
          suiteBuffer.push("method-selectors");
          for (XmlMethodSelector ms : s.getMethodSelectors()) {
            String cls = ms.getClassName();
            if (cls != null && cls.length() > 0) {
              suiteBuffer.push("method-selector");
              Properties p = new Properties();
              p.put("name", cls);
              p.put("priority", ms.getPriority());
              suiteBuffer.push("selector-class", p);
              suiteBuffer.pop("method-selector");
            }
            // TODO: <script> tag of method-selector
          }          
          suiteBuffer.pop("method-selectors");
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private XMLStringBuffer getSuiteBuffer() {
    if(null == m_suiteBuffer) {
      m_suiteBuffer= createContentBuffer();
    }

    return m_suiteBuffer;
  }

  public String getFileName() {
    return RemoteTestNG.DEBUG_SUITE_FILE;
  }

  /** 
   * Generate the current suite to a file.
   * @see org.testng.xml.LaunchSuite#save(java.io.File)
   */
  @Override
  public File save(File directory) {
    directory.mkdirs();
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
//    if(m_annotationType != null) {
//      testAttrs.setProperty("annotations", m_annotationType);
//    }
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
                           final int logLevel) {
    super(projectName, DEFAULT_SUITE_TAG_NAME, parameters, logLevel);
    m_classNames = classNames;
    m_classMethods = classMethods != null ? sanitize(classMethods): classMethods;
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
    return DEFAULT_TEST_TAG_NAME;
//    return m_classNames.size() == 1 ? (String) m_classNames.iterator().next()
//        : DEFAULT_TEST_TAG_NAME;
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
                        final int logLevel) {
    super(projectName, projectName + " by groups", parameters, logLevel);

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
                      final int logLevel) {
    super(projectName, projectName + " by packages", parameters, logLevel);
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
