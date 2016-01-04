package org.testng.eclipse.ui;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.testng.eclipse.TestNGPlugin;
import org.testng.remote.strprotocol.AbstractRemoteTestRunnerClient;
import org.testng.remote.strprotocol.GenericMessage;
import org.testng.remote.strprotocol.IMessageSender;
import org.testng.remote.strprotocol.IRemoteSuiteListener;
import org.testng.remote.strprotocol.IRemoteTestListener;
import org.testng.remote.strprotocol.MessageHelper;
import org.testng.remote.strprotocol.StringMessageSender;
import org.testng.remote.strprotocol.SuiteMessage;
import org.testng.remote.strprotocol.TestMessage;
import org.testng.remote.strprotocol.TestResultMessage;


public class EclipseTestRunnerClient extends AbstractRemoteTestRunnerClient {

  private StringMessageSender m_marshaller;

  @Override
  public synchronized void stopTest() {
    if (m_marshaller != null) {
      m_marshaller.shutDown();
      m_marshaller = null;
    }
    super.stopTest();
  }

  public synchronized void startListening(IRemoteSuiteListener suiteListener,
                                          IRemoteTestListener testListener,
                                          IMessageSender messageMarshaller) 
  {
    ServerConnection srvConnection= new ServerConnection(messageMarshaller) {
      @Override
      protected void handleThrowable(Throwable cause) {
        TestNGPlugin.log(cause);
      };
    };
    
    startListening(new IRemoteSuiteListener[] {suiteListener},
                   new IRemoteTestListener[] {testListener},
                   srvConnection
    );
  }
  
  @Override
  protected void notifyStart(final GenericMessage genericMessage) {
    for(final IRemoteSuiteListener listener : m_suiteListeners) {
      SafeRunner.run(new ListenerSafeRunnable() {
        public void run() {
          listener.onInitialization(genericMessage);
        }
      });
    }
  }

  @Override
  protected void notifySuiteEvents(final SuiteMessage suiteMessage) {
    for(final IRemoteSuiteListener listener : m_suiteListeners) {
      SafeRunner.run(new ListenerSafeRunnable() {
        public void run() {
          if(suiteMessage.isMessageOnStart()) {
            listener.onStart(suiteMessage);
          }
          else {
            listener.onFinish(suiteMessage);
          }
        }
      });
    }
  }

  @Override
  protected void notifyTestEvents(final TestMessage testMessage) {
    for(final IRemoteTestListener listener : m_testListeners) {
      SafeRunner.run(new ListenerSafeRunnable() {
        public void run() {
          if(testMessage.isMessageOnStart()) {
            listener.onStart(testMessage);
          }
          else {
            listener.onFinish(testMessage);
          }
        }
      });
    }
  }

  @Override
  protected void notifyResultEvents(final TestResultMessage testResultMessage) {
    for(final IRemoteTestListener listener : m_testListeners) {
      SafeRunner.run(new ListenerSafeRunnable() {
        public void run() {
          switch(testResultMessage.getResult()) {
            case MessageHelper.TEST_STARTED:
              listener.onTestStart(testResultMessage);
              break;
            case MessageHelper.PASSED_TEST:
              listener.onTestSuccess(testResultMessage);
              break;
            case MessageHelper.FAILED_TEST:
              listener.onTestFailure(testResultMessage);
              break;
            case MessageHelper.SKIPPED_TEST:
              listener.onTestSkipped(testResultMessage);
              break;
            case MessageHelper.FAILED_ON_PERCENTAGE_TEST:
              listener.onTestFailedButWithinSuccessPercentage(testResultMessage);
              break;
          }
        }
      });
    }
  }

  static abstract class ListenerSafeRunnable implements ISafeRunnable {
    public void handleException(Throwable exception) {
      TestNGPlugin.log(exception);
    }
  }
}
