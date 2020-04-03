package org.testng.eclipse;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.testng.eclipse.messages"; //$NON-NLS-1$
  public static String mining_debug;
  public static String mining_run;
  public static String mining_runAll;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
