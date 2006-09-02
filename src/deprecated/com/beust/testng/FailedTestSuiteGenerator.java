package org.testng.reporters;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestRunner;
import org.testng.internal.Utils;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A very custom ISuiteListener that will generate the testng-failed.xml definition.
 * <P/>
 * In case of a failure we need the following info:
 * - suite-verbose, parallel, thread-count, annotations
 * - all parameters declared in the suite
 * - all parameters declared in the enclosing test definition of the failed one
 * 
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a> 
 */
public class FailedTestSuiteGenerator implements ITestListener, ISuiteListener {
  private XmlSuite m_xmlSuite;
  private boolean m_hasFailures;
  private String m_currentTestName;

  private Map<String, XmlTest> m_testNames= new HashMap<String, XmlTest>();
  private Map<String, XmlTest> m_failedTestNames= new HashMap<String, XmlTest>();
  private Map<String, XmlTestMethods> m_testNameMethods= new HashMap<String, XmlTestMethods>();
  private Map<String, String> m_outputs= new HashMap<String, String>();
  
  private Map<Method, ITestNGMethod> m_failedMethodConf= new HashMap<Method, ITestNGMethod>();
  /** Map<class_fqn, Set<Method>>. */
  private Map<String, Set<Method>> m_failedMethodConfPClass= new HashMap<String, Set<Method>>();
  
  private Map<Method, ITestNGMethod> m_failedClassConfs= new HashMap<Method, ITestNGMethod>();
  /** Map<class_fqn, Set<Method>>. */
  private Map<String, Set<Method>> m_failedClassConfPClass= new HashMap<String, Set<Method>>();
  
  private Map<Method, ITestNGMethod> m_failedTestConfs= new HashMap<Method, ITestNGMethod>();
  private Map<Method, ITestNGMethod> m_failedSuiteConfs= new HashMap<Method, ITestNGMethod>();

  public FailedTestSuiteGenerator(XmlSuite xmlSuite) {
    m_xmlSuite= xmlSuite;

    for (XmlTest xt: xmlSuite.getTests()) {
      m_testNames.put(xt.getName(), xt);
    }
  }
  
  public void registerRunner(TestRunner tr) {
    XmlTest xmlTest= tr.getTest();
    String testName= xmlTest.getName();
    
    if (!m_testNames.containsKey(testName)) {
      System.err.println("[ERROR]: the test " + testName + " wasn't registered!");
      m_testNames.put(testName, xmlTest);
    }
    
    m_outputs.put(testName, tr.getOutputDirectory());
    m_testNameMethods.put(testName, new XmlTestMethods(xmlTest, tr.getIClass()));
  }
  
  public void onStart(ISuite suite) {
    ;
  }

  public void onFinish(ISuite suite) {
    if (!m_hasFailures) {
      return; // nothing to generate
    }
    
    for(String testname: m_failedTestNames.keySet()) {
      m_testNameMethods.get(testname).initialize();
    }
    
    Map<String, Set<String>> testperOut= regroupByOutput(m_outputs);
    
    for(String outdir: testperOut.keySet()) {
      Set<String> testnames= testperOut.get(outdir);
      
      XmlSuite newsuite= generateSuite(testnames);
      Utils.writeFile(outdir, "testng-f.xml", newsuite.toXml());
    }
  }

  public void onTestFailure(ITestResult result) {
    registerFailedTest(m_currentTestName, result);
  }

  public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    registerFailedTest(m_currentTestName, result);
  }

  public void onTestSkipped(ITestResult result) {
    registerFailedTest(m_currentTestName, result);
  }

  public void onStart(ITestContext context) {
    m_currentTestName= context.getName();
  }

  public void onFinish(ITestContext context) {
    m_currentTestName= null;
  }

  private void registerFailedTest(final String testName, final ITestResult result) {
    m_hasFailures= true;
    
    ITestNGMethod mth= result.getMethod();

    if (null != testName && mth.isTest()) {
      XmlTestMethods xmlTestMethods= m_testNameMethods.get(testName);
      xmlTestMethods.m_failedTestMethods.add(mth);
      m_failedTestNames.put(testName, m_testNames.get(testName));
    }
    else if (mth.isBeforeMethodConfiguration() || mth.isAfterMethodConfiguration()) {
      m_failedMethodConf.put(mth.getMethod(), mth);
      registerClassMethod(m_failedMethodConfPClass ,mth);
    } 
    else if (mth.isBeforeClassConfiguration() || mth.isAfterClassConfiguration()) {
      m_failedClassConfs.put(mth.getMethod(), mth);
      registerClassMethod(m_failedClassConfPClass, mth);
    }
    else if (mth.isBeforeTestConfiguration() || mth.isAfterTestConfiguration()) {
      m_failedTestConfs.put(mth.getMethod(), mth);
    }
    else if (mth.isBeforeSuiteConfiguration() || mth.isAfterSuiteConfiguration()) {
      m_failedSuiteConfs.put(mth.getMethod(), mth);
    }
  }
  
  private void registerClassMethod(Map<String, Set<Method>> classMethods, ITestNGMethod itm) {
    String classFqn= itm.getRealClass().getName();
    Set<Method> methods= classMethods.get(classFqn);
    
    if (null == methods) {
      methods= new HashSet<Method>();
      classMethods.put(classFqn, methods);
    }
    
    methods.add(itm.getMethod());
  }
  
  private XmlSuite generateSuite(Set<String> testnames) {
//    System.out.println("GenerateSuiteForOutput:[" + output + "]");
    XmlSuite failedSuite= new XmlSuite();
    failedSuite.setAnnotations(m_xmlSuite.getAnnotations());
    failedSuite.setName("Failed Tests suite");
    failedSuite.setParallel(m_xmlSuite.isParallel());
    failedSuite.setThreadCount(m_xmlSuite.getThreadCount());
    failedSuite.setParameters(m_xmlSuite.getParameters());

    for(String name: testnames) {
      // TODO: check also the beforeSuite/afterSuite failed/skipped methods
      if (!m_failedTestNames.containsKey(name)) {
        continue;
      }
      
      XmlTestMethods xtm= m_testNameMethods.get(name);
      
      if (!m_failedSuiteConfs.isEmpty()) {
        // regenerate the full suite as long as a before/afterSuite was failed
      }
      if (xtm.hasTestConfigurationFailures()) {
        // regenerate full XmlTest as long as a before/afterTest was failed
        clone(failedSuite, m_failedTestNames.get(name));
      }
      else {
        generatePartialTest(failedSuite, name);
      }
    }
    
    return failedSuite;
  }
  
  private void generatePartialTest(XmlSuite suite, String testname) {
    XmlTest srcTest= m_testNames.get(testname);
    XmlTest nwXmlTest= cloneTestDef(suite, srcTest);
    XmlTestMethods testMethods= m_testNameMethods.get(testname);
    testMethods.initialize();
    for(XmlClass xmlClass: srcTest.getXmlClasses()) {
      String fqn= xmlClass.getSupportClass().getName();
      
      if (m_failedMethodConfPClass.containsKey(fqn) || m_failedClassConfPClass.containsKey(fqn)) {
        nwXmlTest.getXmlClasses().add(xmlClass);
      }
      else {
        // initially duplicate the class
        XmlClass nwXmlClass= clone(xmlClass);
        ITestClass itc= testMethods.m_testClasses.get(fqn);
        List<ITestNGMethod> nonfailed= getNonFailedMethods(testMethods, fqn);
        
        for(ITestNGMethod itm: nonfailed) {
          nwXmlClass.getExcludedMethods().add(itm.getMethod().getName());
        }
        
        nwXmlTest.getXmlClasses().add(nwXmlClass);
      }
    }
  }
  
  private List<ITestNGMethod> getNonFailedMethods(XmlTestMethods tmethods, String classFqn) {
    ITestClass itc= tmethods.m_testClasses.get(classFqn);
    List<ITestNGMethod> result= new ArrayList<ITestNGMethod>();    
    List<ITestNGMethod> failedMethods= tmethods.m_failedMethodsPClass.get(classFqn);

    if (null != failedMethods) {
      Map<Method, ITestNGMethod> failedMethodsMap= new HashMap<Method, ITestNGMethod>();
    
      for(ITestNGMethod itm: failedMethods) {
        failedMethodsMap.put(itm.getMethod(), itm);
      }
    
     
      for(ITestNGMethod itm: itc.getTestMethods()) {
        if (!failedMethodsMap.containsKey(itm.getMethod())) {
          result.add(itm);
        }
      }
    }
    else {
      for(ITestNGMethod itm: itc.getTestMethods()) {
        result.add(itm);
      }
    }
    
    return result;
  }
  
  /**
   * Clone the <TT>source</TT> <CODE>XmlTest</CODE> by including: 
   * - test attributes
   * - groups definitions
   * - parameters
   * 
   * The &lt;classes&gt; subelement is ignored for the moment.
   * 
   * @param suite
   * @param source
   * @return
   */
  private XmlTest cloneTestDef(XmlSuite suite, XmlTest source) {
    XmlTest result= new XmlTest(suite);
    
    result.setName(source.getName());
    result.setAnnotations(source.getAnnotations());
    result.setIncludedGroups(source.getIncludedGroups());
    result.setExcludedGroups(source.getExcludedGroups());
    result.setJUnit(source.isJUnit());
    result.setParallel(source.isParallel());
    result.setVerbose(source.getVerbose());
    result.setParameters(source.getParameters());
    
    Map<String, List<String>> metagroups= source.getMetaGroups();
    for (String groupName: metagroups.keySet()) {
      result.addMetaGroup(groupName, metagroups.get(groupName));
    }
    
    return result;
  }
  
  private XmlTest clone(XmlSuite suite, XmlTest source) {
    XmlTest result= cloneTestDef(suite, source);
    result.setClassNames(source.getXmlClasses());
    
    return result;
  }
  
  
  private XmlClass clone(XmlClass source) {
    XmlClass newclass= new XmlClass(source.getName());
    newclass.setExcludedMethods(source.getExcludedMethods());
    newclass.setIncludedMethods(source.getIncludedMethods());
    
    return newclass;
  }
  
 
  private void addMethodDefinitions(XmlTest test, Map<String, List<ITestNGMethod>> classMethods) {
    List<XmlClass> classes= new ArrayList<XmlClass>();
    
    for (String className: classMethods.keySet()) {
      XmlClass xmlClass= new XmlClass(className);
      List<String> methodNames= new ArrayList<String>();
      
      for(ITestNGMethod itm: classMethods.get(className)) {
        methodNames.add(itm.getMethod().getName());
      }
      
      xmlClass.setIncludedMethods(methodNames);
      classes.add(xmlClass);
    }
    
    test.setClassNames(classes);
  }
  
  public void onTestSuccess(ITestResult result) {
    ;
  }
  
  private Map<String, Set<String>> regroupByOutput(Map<String, String> outputs) {
    Map<String, Set<String>> result= new HashMap<String, Set<String>>();
    
    for(String testname: outputs.keySet()) {
      String outdir= outputs.get(testname);
      
      Set<String> names= result.get(outdir);
      
      if (null == names) {
        names= new HashSet<String>();
        result.put(outdir, names);
      }
      
      names.add(testname);
    }
    
    return result;
  }
  
  private static class XmlTestMethods {
    private XmlTest m_xmlTest;
    private Map<String, ITestClass> m_testClasses= new HashMap<String, ITestClass>();
    private List<ITestNGMethod> m_failedTestMethods= new ArrayList<ITestNGMethod>();
    
    private Map<String, XmlClass> m_classes= new HashMap<String, XmlClass>();
    private Map<String, List<ITestNGMethod>> m_failedMethodsPClass= new HashMap<String, List<ITestNGMethod>>();
    
    public XmlTestMethods(XmlTest xmlTest, Collection<ITestClass> testClasses) {
      m_xmlTest= xmlTest;
      
      for(ITestClass itc: testClasses) {
        m_testClasses.put(itc.getRealClass().getName(), itc);
      }
    }
    
    public void initialize() {
      for(XmlClass xmlClazz: m_xmlTest.getXmlClasses()) {
        m_classes.put(xmlClazz.getName(), xmlClazz);
      }
      
      for(ITestNGMethod itm: m_failedTestMethods) {
        String fqn= itm.getRealClass().getName();
        List<ITestNGMethod> methods= m_failedMethodsPClass.get(fqn);
        
        if (null == methods) {
          methods= new ArrayList<ITestNGMethod>();
          m_failedMethodsPClass.put(fqn, methods);
        }
        
        methods.add(itm);
      }
    }
    
    /**
     * TODO
     * @return
     */
    public boolean hasTestConfigurationFailures() {
      return false;
    }
  }
}
