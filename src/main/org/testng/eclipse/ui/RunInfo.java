package org.testng.eclipse.ui;


/**
 * Class usage XXX
 * 
 * @version $Revision$
 */
public class RunInfo {
  public static final int SUITE_TYPE = 1;
  public static final int TEST_TYPE = 2;
  public static final int RESULT_TYPE = 3;
  
  protected String m_id;
  protected int m_type;
  protected String m_suiteName;
  protected String m_testName;
  protected String m_className;
  protected String m_methodName;
  protected String m_stackTrace;
  protected int m_methodCount;
  protected int m_passed;
  protected int m_failed;
  protected int m_skipped;
  protected int m_successPercentageFailed;
  protected int m_status;
  
  
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
                 String stackTrace,
                 int status) {
    m_id = suiteName + "." + testName + "." + className + "." + methodName;
    m_suiteName = suiteName;
    m_testName = testName;
    m_className = className;
    m_methodName = methodName;
    m_stackTrace = stackTrace;
    m_type = RESULT_TYPE;
    m_status = status;
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
}
