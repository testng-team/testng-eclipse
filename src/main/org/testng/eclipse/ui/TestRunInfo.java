/*
 * $Id$
 * $Date$
 */
package org.testng.eclipse.ui;


import java.util.HashMap;
import java.util.Map;


/**
 * Class usage XXX
 *
 * @version $Revision$
 */
public class TestRunInfo {
  protected String m_suiteName;
  protected String m_testName;
  protected int    m_testMethodCount;
  protected int    m_passedTestCount;
  protected int    m_failedTestCount;
  protected int    m_skippedTestCount;
  protected int    m_failedOnPercentangeCount;

  protected Map    m_tests = new HashMap();

  public String getId() {
    return m_suiteName + "." + m_testName;
  }

  private static class TestKey {
    protected String m_className;
    protected String m_methodName;


    /**
     * Returns <code>true</code> if this <code>TestKey</code> is the same as the o argument.
     *
     * @return <code>true</code> if this <code>TestKey</code> is the same as the o argument.
     */
    public boolean equals(Object o) {
      if(this == o) {
        return true;
      }
      if((null == o) || !(o instanceof TestKey)) {
        return false;
      }

      TestKey castedObj = (TestKey) o;

      return (((this.m_className == null) ? (castedObj.m_className == null)
                                          : this.m_className.equals(castedObj.m_className))
             && ((this.m_methodName == null) ? (castedObj.m_methodName == null)
                                          : this.m_methodName.equals(castedObj.m_methodName)));
    }

    /**
     * Override hashCode.
     *
     * @return the Objects hashcode.
     */
    public int hashCode() {
      int hashCode = 1;
      hashCode = (31 * hashCode) + ((m_className == null) ? 0 : m_className.hashCode());
      hashCode = (31 * hashCode) + ((m_methodName == null) ? 0 : m_methodName.hashCode());

      return hashCode;
    }
  }
}
