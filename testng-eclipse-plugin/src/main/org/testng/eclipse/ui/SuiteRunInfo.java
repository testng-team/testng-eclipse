package org.testng.eclipse.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.ILaunch;
import org.testng.ITestResult;
import org.testng.remote.strprotocol.GenericMessage;
import org.testng.remote.strprotocol.IRemoteSuiteListener;
import org.testng.remote.strprotocol.IRemoteTestListener;
import org.testng.remote.strprotocol.SuiteMessage;
import org.testng.remote.strprotocol.TestMessage;
import org.testng.remote.strprotocol.TestResultMessage;

/**
 * Holds the result of running a test suite. It is useful for keeping the history of test runs.
 * 
 * @author Jean-Noel Rouvignac
 */
public class SuiteRunInfo implements IRemoteSuiteListener, IRemoteTestListener {

  private IRemoteSuiteListener suiteDelegateListener;
  private IRemoteTestListener testDelegateListener;

  // ~ counters
  private int m_suitesTotalCount;
  private int m_testsTotalCount;
  private int m_methodTotalCount;
  private int m_suiteCount;
  private int m_testCount;
  private int m_methodCount;
  private int m_passedCount;
  private int m_failedCount;
  private int m_skippedCount;
  private int m_successPercentageFailed;

  private long m_startTime;
  private long m_stopTime;

  private List<RunInfo> m_results = new ArrayList<>();

  /**
   * The launcher that has started the test. May be used for reruns.
   */
  private ILaunch launch;

  public SuiteRunInfo(final IRemoteSuiteListener suiteDelegateListener,
      final IRemoteTestListener testDelegateListener, ILaunch launch) {
    this.suiteDelegateListener = suiteDelegateListener;
    this.testDelegateListener = testDelegateListener;
    this.launch = launch;
  }

  public void removeDelegateListeners() {
    this.suiteDelegateListener = null;
    this.testDelegateListener = null;
  }

  public SuiteRunInfo(final int suiteCount, final int testCount) {
    m_suitesTotalCount = suiteCount;
    m_testsTotalCount = testCount;
  }

  boolean hasErrors() {
    return m_failedCount > 0 || m_successPercentageFailed > 0;
  }

  public ILaunch getLaunch() {
    return launch;
  }

  int getStatus() {
    if (hasErrors()) {
      return ITestResult.FAILURE;
    } else if (m_skippedCount > 0) {
      return ITestResult.SKIP;
    }
    return ITestResult.SUCCESS;
  }

  public void onInitialization(GenericMessage genericMessage) {
    m_suiteCount = genericMessage.getSuiteCount();
    m_testCount = genericMessage.getTestCount();
    m_startTime = System.currentTimeMillis();
    if (suiteDelegateListener != null) {
      suiteDelegateListener.onInitialization(genericMessage);
    }
  }

  public void onStart(SuiteMessage suiteMessage) {
    if (suiteDelegateListener != null) {
      suiteDelegateListener.onStart(suiteMessage);
    }
  }

  public void onFinish(SuiteMessage suiteMessage) {
    m_suiteCount++;
    if (isSuiteRunFinished()) {
      m_stopTime = System.currentTimeMillis();
    }
    if (suiteDelegateListener != null) {
      suiteDelegateListener.onFinish(suiteMessage);
    }
  }

  public void onStart(TestMessage tm) {
    m_methodTotalCount += tm.getTestMethodCount();
    if (testDelegateListener != null) {
      testDelegateListener.onStart(tm);
    }
  }

  public void onFinish(TestMessage tm) {
//    m_testCount++;

    // The method count is more accurate than m_methodTotalCount since it also
    // takes data providers and other dynamic invocations into account.
    if (m_methodCount != m_methodTotalCount) {
      m_methodTotalCount = m_methodCount; // trust the methodCount
    }

    if (testDelegateListener != null) {
      testDelegateListener.onFinish(tm);
    }
  }

  public void onTestStart(TestResultMessage trm) {
    if (testDelegateListener != null) {
      testDelegateListener.onTestStart(trm);
    }
  }

  public void onTestSuccess(TestResultMessage trm) {
    m_passedCount++;
    m_methodCount++;
    if (testDelegateListener != null) {
      testDelegateListener.onTestSuccess(trm);
    }
  }

  public void onTestFailure(TestResultMessage trm) {
    m_failedCount++;
    m_methodCount++;
    if (testDelegateListener != null) {
      testDelegateListener.onTestFailure(trm);
    }
  }

  public void onTestSkipped(TestResultMessage trm) {
    m_skippedCount++;
    m_methodCount++;
    if (testDelegateListener != null) {
      testDelegateListener.onTestSkipped(trm);
    }
  }

  public void onTestFailedButWithinSuccessPercentage(TestResultMessage trm) {
    m_successPercentageFailed++;
    m_methodCount++;
    if (testDelegateListener != null) {
      testDelegateListener.onTestFailedButWithinSuccessPercentage(trm);
    }
  }

  public boolean isSuiteRunFinished() {
    return m_suitesTotalCount < m_suiteCount;
  }

  public boolean hasRun() {
    return m_startTime != 0L && m_stopTime != 0L;
  }

  public long getRunDuration() {
    return m_stopTime - m_startTime;
  }

  public int getNewMax() {
    return (m_methodTotalCount * m_testsTotalCount + 1) / (m_testCount + 1);
  }

  public int getMethodCount() {
    return m_methodCount;
  }

  public int getPassedCount() {
    return m_passedCount;
  }

  public int getFailedCount() {
    return m_failedCount;
  }

  public int getSkippedCount() {
    return m_skippedCount;
  }

  public long getStartTime() {
    return m_startTime;
  }

  public int getMethodTotalCount() {
    return m_methodTotalCount;
  }

  public List<RunInfo> getResults() {
    return Collections.unmodifiableList(m_results);
  }

  public int getNbResults() {
    return getResults().size();
  }

  public void add(RunInfo runInfo) {
    this.m_results.add(runInfo);
  }

  public void setSuitesTotalCount(int suitesTotalCount) {
    this.m_suitesTotalCount = suitesTotalCount;
  }
  
  public void setTestsTotalCount(int testsTotalCount) {
    this.m_testsTotalCount = testsTotalCount;
  }
}
