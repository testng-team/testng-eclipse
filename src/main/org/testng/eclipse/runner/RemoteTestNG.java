package org.testng.eclipse.runner;



import java.util.List;
import java.util.Map;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestRunnerFactory;
import org.testng.TestNG;
import org.testng.TestNGCommandLineArgs;
import org.testng.TestRunner;
import org.testng.remote.strprotocol.GenericMessage;
import org.testng.remote.strprotocol.MessageHelper;
import org.testng.remote.strprotocol.RemoteMessageSenderTestListener;
import org.testng.remote.strprotocol.StringMessageSenderHelper;
import org.testng.remote.strprotocol.SuiteMessage;
import org.testng.reporters.JUnitXMLReporter;
import org.testng.reporters.TestHTMLReporter;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;


/**
 * Extension of TestNG registering a remote TestListener.
 *
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class RemoteTestNG extends TestNG {
  protected String m_host;
  protected int    m_port;

  public void setConnectionParameters(String host, int port) {
    if((null == host) || "".equals(host)) {
      m_host = "127.0.0.1";
    }
    else {
      m_host = host;
    }

    m_port = port;
  }


  public void run() {
    final StringMessageSenderHelper msh = new StringMessageSenderHelper(m_host, m_port);
    try {
      if(msh.connect()) {
        if(m_suites.size() > 0) {

          int testCount = 0;
          
          for(int i = 0; i < m_suites.size(); i++) {
            testCount += ((XmlSuite) m_suites.get(i)).getTests().size();
          }
          
          GenericMessage gm = new GenericMessage(MessageHelper.GENERIC_SUITE_COUNT);
          gm.addProperty("suiteCount", m_suites.size())
              .addProperty("testCount", testCount)
              ;
          msh.sendMessage(gm);
          
          addListener(new ISuiteListener() {
            public void onFinish(ISuite suite) {
              msh.sendMessage(new SuiteMessage(suite, false /*start*/));
            }
            
            public void onStart(ISuite suite) {
              msh.sendMessage(new SuiteMessage(suite, true /*start*/));
            }
          });
          
          m_testRunnerFactory= new ITestRunnerFactory() {
            public TestRunner newTestRunner(ISuite suite, XmlTest xmlTest) {
              TestRunner runner = new TestRunner(suite, xmlTest);
              runner.addTestListener(new RemoteMessageSenderTestListener(suite, xmlTest, msh));
              runner.addTestListener(new TestHTMLReporter());
              runner.addTestListener(new JUnitXMLReporter());

              return runner;
            }
          };
 
          super.run();
        }
        else {
          System.err.println("WARNING: No test suite found.  Nothing to run");
        }
      }
      else {
        System.err.println("Cannot connect to " + m_host + " on " + m_port);
      }
    }
    catch(Throwable cause) {
      cause.printStackTrace(System.err);
    }
    finally {
      msh.shutDown();
      System.exit(0);
    }
  }
  
  public static void main(String[] args) {
    Map commandLineArgs = TestNGCommandLineArgs.parseCommandLine(args);

    RemoteTestNG testNG = new RemoteTestNG();
    
    testNG.setOutputDirectory((String) commandLineArgs.get(TestNGCommandLineArgs.OUTDIR_COMMAND_OPT));
    testNG.setSourcePath((String) commandLineArgs.get(TestNGCommandLineArgs.SRC_COMMAND_OPT));
    
    List classes = (List) commandLineArgs.get(TestNGCommandLineArgs.TESTCLASS_COMMAND_OPT);
    if(null != classes) {
      testNG.setTestClasses((Class[]) classes.toArray(new Class[classes.size()]));
    }
    
    List suites = (List) commandLineArgs.get(TestNGCommandLineArgs.SUITE_DEF_OPT);
    if(null != suites) {
      testNG.setTestSuites(suites);
    }

    testNG.setTarget((String) commandLineArgs.get(TestNGCommandLineArgs.TARGET_COMMAND_OPT));
    testNG.setTestJar((String) commandLineArgs.get(TestNGCommandLineArgs.TESTJAR_COMMAND_OPT));
    testNG.setConnectionParameters((String) commandLineArgs.get(TestNGCommandLineArgs.HOST_COMMAND_OPT),
                                   Integer.parseInt((String) commandLineArgs.get(TestNGCommandLineArgs.PORT_COMMAND_OPT)));

    testNG.run();

  }
}
