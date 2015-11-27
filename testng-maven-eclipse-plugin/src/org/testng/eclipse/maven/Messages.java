package org.testng.eclipse.maven;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

  private static final String BUNDLE_NAME = Messages.class.getName();
  
  static {
    initializeMessages(BUNDLE_NAME, Messages.class);
  }
  
  public static String prefPrefixFromPomGroupName;
  public static String prefArgLineBtnName;
  public static String prefEnvironBtnName;
  public static String prefSysPropsBtnName;
}
