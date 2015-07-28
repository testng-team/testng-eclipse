package org.testng.eclipse;

import org.testng.remote.RemoteTestNG;

/**
 * Constants used by the plug-in. 
 */
public abstract class TestNGPluginConstants {
  public static final int LAUNCH_ERROR = 1001;
  public static final int LAUNCH_ERROR_JVM_VER_NOT_COMPATIBLE = 1002;

  public static final String TESTNG_HOME= "TESTNG_HOME"; //$NON-NLS-1$
  public static final String MAIN_RUNNER = RemoteTestNG.class.getName();
  
  public static final String S_OUTDIR= ".outdir"; //$NON-NLS-1$
  public static final String S_ABSOLUTEPATH= ".absolutepath"; //$NON-NLS-1$
  public static final String S_USEPROJECTJAR= ".useProjectJar"; //$NON-NLS-1$
  public static final String S_USEPROJECTJAR_GLOBAL= "_TESTNG_ECLIPSE" + S_USEPROJECTJAR; //$NON-NLS-1$
//  public static final String S_REPORTERS = ".reporters";
  public static final String S_DISABLEDLISTENERS = ".disabledListeners";
//  public static final String S_PARALLEL = ".parallel";
  public static final String S_WATCH_RESULTS = ".watchResults";
  public static final String S_WATCH_RESULT_DIRECTORY = ".watchResultDirectory";
  
  public static final String S_DEPRECATED_OUTPUT = "generalOutput";
  public static final String S_DEPRECATED_ABSOLUTEPATH = "generalOutputRelative";

  public static final String S_FAILED_TESTS = "failedTests";
  public static final String S_XML_TEMPLATE_FILE = "xmlTemplateFile";
  public static final String S_JVM_ARGS = "jvmArgs";
  public static final String S_EXCLUDED_STACK_TRACES = "excludedStackTraces";
  public static final String S_SUITE_METHOD_TREATMENT = "suiteMethodTreatment";
  public static final String S_PRE_DEFINED_LISTENERS = "preDefinedListeners";
  public static final String S_SHOW_VIEW_WHEN_TESTS_COMPLETE = "showViewWhenTestComplete";

  private TestNGPluginConstants() {}
}
