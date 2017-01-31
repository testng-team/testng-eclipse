package org.testng.eclipse.ui;


import java.util.Map;
import java.util.regex.Pattern;

import org.testng.ITestResult;
import org.testng.eclipse.util.CustomSuite;
/**
 * Carries along information about a test result.
 * 
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class RunInfo {
  public static final int SUITE_TYPE = 1;
  public static final int TEST_TYPE = 2;
  public static final int RESULT_TYPE = 3;
  private static final Pattern NEWLINES= Pattern.compile("\n");
  private static final Pattern CARRAGERETURN= Pattern.compile("\r");
  
  private String m_id;
  private String m_idWithDesc;
  private int m_type;
  private String m_suiteName = CustomSuite.DEFAULT_SUITE_TAG_NAME;
  private String m_testName = CustomSuite.DEFAULT_TEST_TAG_NAME;
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
  private String m_jvmArgs;
  private Map<String, String> m_environment;
  private long m_time;
  private int m_invocationCount;
  private int m_currentInvocationCount;
  private String m_instanceName;

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

//  public RunInfo(TestResultMessage trm) {
//    this(trm.getSuiteName(), trm.getName(), trm.getTestClass(), trm.getMethod(),
//        trm.getTestDescription(), trm.getParameters(), trm.getParameterTypes(),
//        trm.getEndMillis() - trm.getStartMillis(), trm.getStackTrace(),
//        trm.getResult());
//  }

  public RunInfo(String suiteName, 
                 String testName, 
                 String className, 
                 String methodName,
                 String testDesc,
                 String instanceName,
                 String[] params,
                 String[] paramTypes,
                 long time,
                 String stackTrace,
                 int status,
                 int invocationCount,
                 int currentInvocationCount) {
    m_id = suiteName + "." + testName + "." + className + "."
        + methodName + toString(params, paramTypes);
    if (testDesc != null) m_idWithDesc = m_id + "." + testDesc;
    else m_idWithDesc = m_id;
    m_suiteName = suiteName;
    m_testName = testName;
    m_className = className;
    m_methodName = methodName;
    m_testDescription= testDesc != null ? (testDesc.equals(methodName) ? null : testDesc) : null;
    m_instanceName = instanceName;
    m_parameters= params;
    m_parameterTypes= paramTypes;
    m_time = time >= 0 ? time : 0;
    m_stackTrace = stackTrace;
    m_type = RESULT_TYPE;
    m_status = status;
    m_invocationCount = invocationCount;
    m_currentInvocationCount = currentInvocationCount;
  }

  public long getTime() {
    return m_time;
  }
  
  /**
   * @param params
   * @param paramTypes
   * @return
   */
  private String toString(String[] params, String[] paramTypes) {
    if(null == params || params.length == 0) return "()";
    
    StringBuffer buf= new StringBuffer("(");
    for(int i= 0; i < params.length; i++) {
      if(i > 0) buf.append(",");
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

  /*String escapeNewLines(String s) {
    if(s.indexOf('\n') != -1 || s.indexOf('\r') != -1) {
      return s.replace("\n", "\\n").replace("\r", "\\r");
    }
    
    return s;
  }*/
  
  String escapeNewLines2(String s) {
    String result= NEWLINES.matcher(s).replaceAll("\\\\n");
    return CARRAGERETURN.matcher(result).replaceAll("\\\\r");
  }
  
  String escapeNewLines3(String s) {
    String result= Pattern.compile("\n").matcher(s).replaceAll("\\\\n");
    return Pattern.compile("\r").matcher(result).replaceAll("\\\\r");
  }
  
  /**
   * Override hashCode.
   *
   * @return the Objects hashcode.
   */
  @Override
  public int hashCode() {
    return m_id.hashCode();
  }
  
  /**
   * Returns <code>true</code> if this <code>RunInfo</code> is the same as the o argument.
   *
   * @return <code>true</code> if this <code>RunInfo</code> is the same as the o argument.
   */
  @Override
  public boolean equals(Object o) {
    if(this == o) {
      return true;
    }
    if(null == o || !(o instanceof RunInfo)) {
      return false;
    }

    return m_id.equals(((RunInfo) o).m_id);
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("RunInfo[");
    buffer.append("status:" + getStatusName());
    buffer.append(" type:" + typeToString(m_type));
    buffer.append(" id:");
    buffer.append(m_id);
//    if (m_suiteName != null) {
//      buffer.append(" suiteName:");
//      buffer.append(m_suiteName);
//    }
//    if (m_testName != null) {
//      buffer.append(" testName:");
//      buffer.append(m_testName);
//    }
//    if (m_className != null) {
//      buffer.append(" className:");
//      buffer.append(m_className);
//    }
//    if (m_methodName != null) {
//      buffer.append(" methodName:");
//      buffer.append(m_methodName);
//    }
//    if (m_methodCount != 0) {
//      buffer.append("   methodCount:");
//      buffer.append(m_methodCount);
//    }
    if (m_passed != 0) {
      buffer.append(" passed:");
      buffer.append(m_passed);
    }
    if (m_failed > 0) {
      buffer.append(" failed:");
      buffer.append(m_failed);
    }
    if (m_skipped > 0) {
      buffer.append(" skipped:");
      buffer.append(m_skipped);
    }
//    buffer.append(" successPercentageFailed:");
//    buffer.append(m_successPercentageFailed);
    buffer.append("]");
    
    return buffer.toString();
  }

  private String typeToString(int type) {
    if (type == SUITE_TYPE) return "Suite";
    else if (type == TEST_TYPE) return "Test";
    else if (type == RESULT_TYPE) return "Result";
    else return "<unknown>";
  }

  /**
   * FIXME: rename to getMethodFQN()
   * @return
   */
  public String getMethodDisplay() {
    StringBuffer buf= new StringBuffer(m_className);
    buf.append(m_methodName).append(getParametersDisplay())
      .append(getInvocationCountDisplay())
    ;

    return buf.toString();
  }

  public String getTestDescription() {
    if (null == m_testDescription || "".equals(m_testDescription.trim())) return "";
    else return m_testDescription;
//    return m_testDescription.substring(m_testDescription.indexOf('('));
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

  public String getStatusName() {
    return m_status == ITestResult.FAILURE ? "failure"
        : (m_status == ITestResult.SUCCESS ? "success"
            : (m_status == ITestResult.SKIP ? "skipped" : "unknown"));
  }

  public String getStackTrace() {
    return m_stackTrace;
  }
  
  public String getJvmArgs() {
	return m_jvmArgs;
  }

  public void setJvmArgs(String m_jvmArgs) {
    this.m_jvmArgs = m_jvmArgs;
  }

  public Map<String, String> getEnvironmentVariables() {
    return m_environment;
  }

  public void setEnvironmentVariables(Map<String, String> environment) {
    this.m_environment = environment;
  }
  
  /**
   * The string that will be displayed in the tree view of AbstractHierarchyTab.
   */
  public String getTreeLabel() {
    return getMethodName()
        + getParametersDisplay()
        + "(" + ((float) getTime() / (float) 1000) + " s) "
        ;
  }

  public String getTestId() {
    return m_suiteName + "." + m_testName;
  }

  public String getMethodId() {
    return getTestId() + "." + getMethodDisplay();
  }

  public String getClassId() {
    return getTestId() + "." + m_className;
  }

  public int getInvocationCount() {
    return m_invocationCount;
  }

  public int getCurrentInvocationCount() {
    return m_currentInvocationCount;
  }

  public String getInvocationCountDisplay() {
    int ic = getInvocationCount();
    if (ic > 1) {
      return " " + getCurrentInvocationCount() + "/" + ic;
    } else {
      return "";
    }
  }

  public String getInstanceName() {
    return m_instanceName;
  }

  /*public static void main(String[] args) {
    String test1= "something\nwrong";
    String test2= "something\rwrong";
    String test3= "something\ndefinitely\rwrong";
    String test4= "something\\n not\\r wrong";
    String test5= "something not wrong\n";
    String test6= "something not wrong\n\r";
    String test7= "something not wrong\r\n";
    
    RunInfo info= new RunInfo("doesntmatter");
    System.out.println("1: " + info.escapeNewLines(test1));
    System.out.println("2: " + info.escapeNewLines(test2));
    System.out.println("3: " + info.escapeNewLines(test3));
    System.out.println("4: " + info.escapeNewLines(test4));
    System.out.println("5: " + info.escapeNewLines(test5));
    System.out.println("6: " + info.escapeNewLines(test6));
    System.out.println("7: " + info.escapeNewLines(test7));
    System.out.println("1: " + info.escapeNewLines2(test1));
    System.out.println("2: " + info.escapeNewLines2(test2));
    System.out.println("3: " + info.escapeNewLines2(test3));
    System.out.println("4: " + info.escapeNewLines2(test4));
    System.out.println("5: " + info.escapeNewLines2(test5));
    System.out.println("6: " + info.escapeNewLines2(test6));
    System.out.println("7: " + info.escapeNewLines2(test7));
  }*/
}
