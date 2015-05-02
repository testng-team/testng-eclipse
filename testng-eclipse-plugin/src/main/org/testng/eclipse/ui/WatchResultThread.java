package org.testng.eclipse.ui;

import org.testng.remote.strprotocol.IRemoteSuiteListener;
import org.testng.remote.strprotocol.IRemoteTestListener;
import org.testng.reporters.XMLReporter;
import org.testng.xml.ResultXMLParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Create a thread that monitors testng-results.xml.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class WatchResultThread {
  private static final boolean DEBUG = false;

  private Runnable m_watchResultRunnable;
  private boolean m_watchResults = true;
  private Thread m_watchResultThread;

  public WatchResultThread(final String path, final IRemoteSuiteListener suiteListener,
      final IRemoteTestListener testListener)
  {
    m_watchResultRunnable = new Runnable() {
      public void run() {
        File f = new File(path, XMLReporter.FILE_NAME);
        long timeStamp = f.lastModified();
        p("Watching " + path);
        while (m_watchResults) {
          long t = f.lastModified();
          p("Comparing " + t + " and " + timeStamp + " for " + f);
          if (t != timeStamp) {
            p("The file changed, updating the view");
            timeStamp = t;
            ResultXMLParser parser = new ResultXMLParser(suiteListener, testListener);
            try {
              parser.parse(path, new FileInputStream(f), false /* don't load classes */);
            } catch (FileNotFoundException e) {
              // Ignore
            }
          }

          try {
            Thread.sleep(5000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        p("No longer watching " + path);
      }
    };
    m_watchResultThread = new Thread(m_watchResultRunnable);
    m_watchResultThread.start();
  }

  private static void p(String string) {
    if (DEBUG) {
      System.out.println("[WatchResultThread] " + string);
    }
  }

  public void stopWatching() {
    m_watchResults = false;
  }
}
