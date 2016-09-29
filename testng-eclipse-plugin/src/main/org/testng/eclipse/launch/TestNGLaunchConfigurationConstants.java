package org.testng.eclipse.launch;

import org.testng.eclipse.TestNGPlugin;


/**
 * Constants used to pass information from the launch manager to the launcher.
 * 
 * @author cbeust
 */
public abstract class TestNGLaunchConfigurationConstants {
  public static final String ID_TESTNG_APPLICATION = "org.testng.eclipse.launchconfig"; //$NON-NLS-1$
  
  private static String make(String s) {
    return TestNGPlugin.PLUGIN_ID + "." + s;
  }
  
  /**
   * Root directory of the tests.  If this property is set,
   * TestNG will run all the tests contained in this directory.
   */
  public static final String DIRECTORY_TEST_LIST = make("DIRECTORY_TEST_LIST"); //$NON-NLS-1$
  
  /**
   * List of classes
   */
  public static final String CLASS_TEST_LIST =  make("CLASS_TEST_LIST"); //$NON-NLS-1$

  /**
   * List of methods. This is replaced by {@link #ALL_METHODS_LIST}.
   */
  public static final String METHOD_TEST_LIST = make("METHOD_TEST_LIST"); //$NON-NLS-1$
  
  public static final String ALL_METHODS_LIST = make("ALL_CLASS_METHODS"); //$NON-NLS-1$
  
  /**
   * List of packages
   */
  public static final String PACKAGE_TEST_LIST = make("PACKAGE_TEST_LIST"); //$NON-NLS-1$
  
  /**
   * List of sources
   */
  public static final String SOURCE_TEST_LIST = make("SOURCE_TEST_LIST"); //$NON-NLS-1$
  
  /**
   * List of groups
   */
  public static final String GROUP_LIST = make("GROUP_LIST"); //$NON-NLS-1$

  public static final String GROUP_CLASS_LIST = make("GROUP_LIST_CLASS");

  /**
   * default log level for nothing
   */
  public static final int DEFAULT_LOG_LEVEL = 2;
  
  /**
   * List of suites
   */
  public static final String SUITE_TEST_LIST = make("SUITE_TEST_LIST");  //$NON-NLS-1$

  /**
   * Port of the launcher
   */
  public static final String PORT = make("PORT"); //$NON-NLS-1$
  
  /**
   * Name of the project
   */
  public static final String PROJECT_NAME = make("PROJECT_NAME"); //$NON-NLS-1$
  
  public static final String TESTNG_RUN_NAME_ATTR = make("SUBNAME"); //$NON-NLS-1$

  public static final String TEMP_SUITE_LIST = make("TEMP_SUITE_LIST"); //$NON-NLS-1$
  
  public static final String TYPE = make("TYPE");  //$NON-NLS-1$ 
  
  public static final String LOG_LEVEL = make("LOG_LEVEL");  //$NON-NLS-1$ 
  
  public static final String VERBOSE = make("VERBOSE");  //$NON-NLS-1$
  
  public static final String DEBUG = make("DEBUG");  //$NON-NLS-1$
  
  public static final String PROTOCOL = make("PROTOCOL");  //$NON-NLS-1$

  public static final String RUNTIME_TESTNG_VERSION = make("RUNTIME_TESTNG_VERSION");  //$NON-NLS-1$

  public static final String VM_ENABLEASSERTION_OPTION = "-ea";

  public static final String PRE_DEFINED_LISTENERS = make("PRE_DEFINED_LISTENERS");

  // What kind of run we are doing
  // This would be a nice place for an enum when jdk1.5 or later can be 
  // required.
  static public enum LaunchType {
    UNDEFINED(-1),
    CLASS(1),
    GROUP(2),
    SUITE(3),
    METHOD(4),
    PACKAGE(5);

    private int m_type;

    LaunchType(int type) {
      m_type = type;
    }

    public static LaunchType fromInt(int result) {
      for (LaunchType lt : values()) {
        if (lt.m_type == result) return lt;
      }
      return null;
    }
  }

  public static final Protocols[] SERIALIZATION_PROTOCOLS = {Protocols.JSON, Protocols.OBJECT, Protocols.STRING};
  public static final Protocols DEFAULT_SERIALIZATION_PROTOCOL = SERIALIZATION_PROTOCOLS[0];

  public static enum Protocols {
    OBJECT("object"),
    STRING("string"),
    JSON("json");

    private final String text;

    /**
     * @param text
     */
    private Protocols(final String text) {
        this.text = text;
    }

    public static Protocols get(String text) {
      switch (text) {
      case "object":
        return OBJECT;
      case "string":
        return STRING;
      case "json":
        return JSON;
      default:
        throw new IllegalArgumentException("Unrecognized protocol: " + text);
      }
    }

    @Override
    public String toString() {
        return text;
    }
  }

}

