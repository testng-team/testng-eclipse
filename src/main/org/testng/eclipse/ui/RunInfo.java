package org.testng.eclipse.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class usage XXX
 * 
 * @version $Revision$
 */
public class RunInfo {
  public static final int SUITE_TYPE = 1;
  public static final int TEST_TYPE = 2;
  public static final int RESULT_TYPE = 3;
  private static final Pattern NEWLINES= Pattern.compile("\n", Pattern.LITERAL);
  private static final Pattern CARRAGERETURN= Pattern.compile("\r", Pattern.LITERAL);
  
  private String m_id;
  private int m_type;
  private String m_suiteName;
  private String m_testName;
  private String m_className;
  private String m_methodName;
  private String[] m_parameters;
  private String[] m_parameterTypes;
  private String m_stackTrace;
  protected int m_methodCount;
  protected int m_passed;
  protected int m_failed;
  protected int m_skipped;
  protected int m_successPercentageFailed;
  private int m_status;
  private String m_testDescription;
  
  
  public RunInfo(String suiteName) {
    m_id = suiteName;
    m_suiteName = suiteName;
    m_type = SUITE_TYPE;
  }
  
  public RunInfo(String suiteName, String testName) {
    m_id = suiteName + "." + testName;
    m_suiteName = suiteName;
    m_testName = testName;
    m_type = TEST_TYPE;
  }
  
  public RunInfo(String suiteName, 
                 String testName, 
                 String className, 
                 String methodName,
                 String testDesc,
                 String[] params,
                 String[] paramTypes,
                 String stackTrace,
                 int status) {
    m_id = suiteName + "." + testName + "." + className + "." + methodName + toString(params, paramTypes);
    m_suiteName = suiteName;
    m_testName = testName;
    m_className = className;
    m_methodName = methodName;
    m_testDescription= testDesc != null ? (testDesc.equals(methodName) ? null : testDesc) : null;
    m_parameters= params;
    m_parameterTypes= paramTypes;
    m_stackTrace = stackTrace;
    m_type = RESULT_TYPE;
    m_status = status;
  }
  
  
  /**
   * @param params
   * @param paramTypes
   * @return
   */
  private String toString(String[] params, String[] paramTypes) {
    if(null == params || params.length == 0) return "";
    
    StringBuffer buf= new StringBuffer("(");
    for(int i= 0; i < params.length; i++) {
      if(i > 0) buf.append(", ");
      if("java.lang.String".equals(paramTypes[i]) && !("null".equals(params[i]) || "\"\"".equals(params[i]))) {
        String p= escapeNewLines2(params[i]);
        buf.append("\"").append(p).append("\"");
      }
      else {
        buf.append(params[i]);
      }
    }
    
    return buf.append(")").toString();
  }

/*  String escapeNewLines(String s) {
    if(s.indexOf('\n') != -1 || s.indexOf('\r') != -1) {
      return s.replace("\n", "\\n").replace("\r", "\\r");
    }
    
    return s;
  }
*/  
  String escapeNewLines2(String s) {
    String result= NEWLINES.matcher(s).replaceAll("\\\\n");
    return CARRAGERETURN.matcher(result).replaceAll("\\\\r");
  }
  
  /**
   * Override hashCode.
   *
   * @return the Objects hashcode.
   */
  public int hashCode() {
    return m_id.hashCode();
  }
  
  /**
   * Returns <code>true</code> if this <code>RunInfo</code> is the same as the o argument.
   *
   * @return <code>true</code> if this <code>RunInfo</code> is the same as the o argument.
   */
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(null == o || !(o instanceof RunInfo)) {
      return false;
    }

    return m_id.equals(((RunInfo) o).m_id);
  }
  
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("RunInfo[");
    buffer.append("m_id: ");
    buffer.append(m_id);
    buffer.append(";m_suiteName: ");
    buffer.append(m_suiteName);
    buffer.append(";m_testName: ");
    buffer.append(m_testName);
    buffer.append(";m_className: ");
    buffer.append(m_className);
    buffer.append(";m_methodName: ");
    buffer.append(m_methodName);
    buffer.append(";m_methodCount: ");
    buffer.append(m_methodCount);
    buffer.append(";m_passed: ");
    buffer.append(m_passed);
    buffer.append(";m_failed: ");
    buffer.append(m_failed);
    buffer.append(";m_skipped: ");
    buffer.append(m_skipped);
    buffer.append(";m_successPercentageFailed: ");
    buffer.append(m_successPercentageFailed);
    buffer.append("]");
    
    return buffer.toString();
  }

  /**
   * FIXME: rename to getMethodFQN()
   * @return
   */
  public String getMethodDisplay() {
    StringBuffer buf= new StringBuffer(m_className);
    buf.append(getTestDescription())
      .append(".").append(m_methodName).append(getParametersDisplay())
    ;
    
    return buf.toString();
  }

  public String getTestDescription() {
    if(null == m_testDescription || "".equals(m_testDescription.trim())) return "";
    
    return m_testDescription.substring(m_testDescription.indexOf('('));
  }
  
  /**
   * @return
   */
  public String getParametersDisplay() {
    if(null == m_parameters || m_parameters.length == 0) return "";

    return toString(m_parameters, m_parameterTypes);
  }

  /**
   * @return
   */
  public String getClassName() {
    return m_className;
  }

  /**
   * @return
   */
  public String getMethodName() {
    return m_methodName;
  }

  /**
   * @return
   */
  public String[] getParameterTypes() {
    return m_parameterTypes;
  }
  
  public String getId() {
    return m_id;
  }
  
  public String getSuiteName() {
    return m_suiteName;
  }
  
  public String getTestName() {
    return m_testName;
  }
  
  public String getTestFQN() {
    return m_suiteName + "." + m_testName;
  }
  
  public int getType() {
    return m_type;
  }
  
  public int getStatus() {
    return m_status;
  }
  
  public String getStackTrace() {
    return m_stackTrace;
  }
  
  public static void main(String[] args) {
    String test1= "something\nwrong";
    String test2= "something\rwrong";
    String test3= "something\ndefinitely\rwrong";
    String test4= "something\\n not\\r wrong";
    
    RunInfo info= new RunInfo("doesntmatter");
    System.out.println("1: " + info.escapeNewLines(test1));
    System.out.println("2: " + info.escapeNewLines(test2));
    System.out.println("3: " + info.escapeNewLines(test3));
    System.out.println("4: " + info.escapeNewLines(test4));
    System.out.println("1: " + info.escapeNewLines2(test1));
    System.out.println("2: " + info.escapeNewLines2(test2));
    System.out.println("3: " + info.escapeNewLines2(test3));
    System.out.println("4: " + info.escapeNewLines2(test4));
  }
}
