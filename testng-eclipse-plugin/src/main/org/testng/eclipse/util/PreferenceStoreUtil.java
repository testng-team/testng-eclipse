package org.testng.eclipse.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.testng.TestNG;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.TestNGPluginConstants;

import java.io.File;
import java.util.Random;

/**
 * Utility methods to store and retrieve data in the preference store.
 * 
 * XXX The whole preference store parts are used in wrong way, 
 *      need to be refactored to follow eclipse scoped preference story
 *      which require to change both <code>ProjectPropertyPage</code> and
 *      <code>WorkspacePreferencepage</code>, and also need to consider back compitability 
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class PreferenceStoreUtil {
  private IPreferenceStore m_storage;

  public PreferenceStoreUtil(IPreferenceStore storage) {
    m_storage= storage;
  }

  /**
   * Saves the output directory information.
   */
  public void storeOutputDir(String projectName, String outdir, boolean isAbsolute) {
    m_storage.setValue(projectName + TestNGPluginConstants.S_OUTDIR, outdir);
    m_storage.setValue(projectName + TestNGPluginConstants.S_ABSOLUTEPATH, isAbsolute);
  }

  public void storeDisabledListeners(String projectName, boolean selection) {
    m_storage.setValue(projectName + ".disabledListeners", selection);
  }

  public void storeUseProjectJar(String projectName, boolean selection) {
    String strUsePrjJar = String.valueOf(selection);
    // here store the string value rather than boolean is to prevent the value being removed from store,
    // which cause it's hard to know wheter the value should read from global or project level
    // now by explicitly set the value "true" or "false" to know it's at project level,
    // otherwise, it comes from global level
    m_storage.setValue(projectName + TestNGPluginConstants.S_USEPROJECTJAR, strUsePrjJar);
  }

  public void storeXmlTemplateFile(String projectName, String xmlFile) {
    m_storage.setValue(projectName + TestNGPluginConstants.S_XML_TEMPLATE_FILE, xmlFile);
  }
  
  public void storePreDefinedListeners(String projectName, String listeners){
    m_storage.setValue(projectName + TestNGPluginConstants.S_PRE_DEFINED_LISTENERS, listeners);
  }

  public String getExcludedStackTraces(String projectName) {
    return getString(projectName, false, TestNGPluginConstants.S_EXCLUDED_STACK_TRACES);
  }

  public File getTemporaryDirectory() {
    Random r = new Random(System.currentTimeMillis());
    File result = new File(System.getProperty("java.io.tmpdir") + File.separatorChar
        + "testng-eclipse-" + r.nextInt());

    return result;
  }
  
  public String getXmlTemplateFile(String projectName, boolean projectOnly) {
    return getString(projectName, projectOnly, TestNGPluginConstants.S_XML_TEMPLATE_FILE);
  }
  
  public String getPreDefinedListeners(String projectName, boolean projectOnly){
    return getString(projectName, projectOnly, TestNGPluginConstants.S_PRE_DEFINED_LISTENERS);
  }

  public IPath getOutputDirectoryPath(IJavaProject project) {
    final String projectName= project.getElementName();
    final String outdir= getOutputDir(projectName, false);
    boolean isAbsolute= isOutputAbsolutePath(projectName, false);
    return new Path(isAbsolute ? outdir : project.getPath().toOSString() + "/" + outdir);
  }

  public IPath getOutputAbsolutePath(IJavaProject project) {
    final String projectName= project.getElementName();
    final String outdir= getOutputDir(projectName, false);
    boolean isAbsolute= isOutputAbsolutePath(projectName, false);
    
    return new Path(isAbsolute ? outdir : project.getProject().getLocation().toOSString() + "/" + outdir);
  }

  public String getOutputDir(String projectName, boolean projectOnly) {
    String result = getString(projectName, projectOnly, TestNGPluginConstants.S_OUTDIR);

    // Convert the deprecated property into the new one
    if (StringUtils.isEmptyString(result)) {
      if (m_storage.contains(TestNGPluginConstants.S_DEPRECATED_OUTPUT)) {
        m_storage.setValue(TestNGPluginConstants.S_DEPRECATED_OUTPUT, "");
        m_storage.setValue(TestNGPluginConstants.S_OUTDIR,
            m_storage.getString(TestNGPluginConstants.S_DEPRECATED_OUTPUT));
      }

      String outDir = m_storage.getString(TestNGPluginConstants.S_OUTDIR);
      result = !"".equals(outDir) ? outDir : TestNG.DEFAULT_OUTPUTDIR;
    }

    return result;
  }

  public boolean isOutputAbsolutePath(String projectName, boolean projectOnly) {
    if(projectOnly || m_storage.contains(projectName + TestNGPluginConstants.S_ABSOLUTEPATH)) {
      return m_storage.getBoolean(projectName + TestNGPluginConstants.S_ABSOLUTEPATH);
    }
    // backward compatibility 5.6.20070407
    if(m_storage.contains(TestNGPluginConstants.S_DEPRECATED_ABSOLUTEPATH)) {
      m_storage.setValue(TestNGPluginConstants.S_DEPRECATED_ABSOLUTEPATH, false);
      m_storage.setValue(TestNGPluginConstants.S_ABSOLUTEPATH, 
          m_storage.getBoolean(TestNGPluginConstants.S_DEPRECATED_ABSOLUTEPATH));
    }
    
    return m_storage.getBoolean(TestNGPluginConstants.S_ABSOLUTEPATH);
  }

  private String getString(String projectName, boolean projectOnly, String prefName) {
    String result = "";
    if (m_storage.contains(projectName + prefName)) {
      result = m_storage.getString(projectName + prefName);
    }
    if (StringUtils.isEmptyString(result) && ! projectOnly) {
      result = m_storage.getString(prefName);
    }

    return result;
  }

//  public String getReporters(String projectName, boolean projectOnly) {
//    return getString(projectName, projectOnly, TestNGPluginConstants.S_REPORTERS);
//  }

//  public String getParallel(String projectName, boolean projectOnly) {
//    String result = getString(projectName, projectOnly, TestNGPluginConstants.S_PARALLEL);
//    return Utils.isEmpty(result) ? "false" : result;
//  }

  public boolean hasDisabledListeners(String projectName, boolean projectOnly) {
    if(projectOnly || m_storage.contains(projectName + TestNGPluginConstants.S_DISABLEDLISTENERS)) {
      return m_storage.getBoolean(projectName + TestNGPluginConstants.S_DISABLEDLISTENERS);  
    }
    else {
      return m_storage.getBoolean(TestNGPluginConstants.S_DISABLEDLISTENERS);
    }
  }

  public boolean getUseProjectJar(String projectName) {
    String strUsePrjJar = m_storage.getString(projectName + TestNGPluginConstants.S_USEPROJECTJAR);
    // if no project level setting, query from global
    if (strUsePrjJar == null || strUsePrjJar.isEmpty()) {
      return TestNGPlugin.getDefault().getPreferenceStore()
          .getBoolean(TestNGPluginConstants.S_USEPROJECTJAR_GLOBAL);
    }
    return Boolean.valueOf(strUsePrjJar).booleanValue();
  }

  public boolean getWatchResults(String projectName) {
    return m_storage.getBoolean(projectName + TestNGPluginConstants.S_WATCH_RESULTS);
  }

  public String getWatchResultDirectory(String projectName) {
    return m_storage.getString(projectName + TestNGPluginConstants.S_WATCH_RESULT_DIRECTORY);
  }

  public void storeWatchResults(String projectName, boolean selection) {
    m_storage.setValue(projectName + TestNGPluginConstants.S_WATCH_RESULTS, selection);
  }

  public void storeWatchResultLocation(String projectName, String text) {
    m_storage.setValue(projectName + TestNGPluginConstants.S_WATCH_RESULT_DIRECTORY, text);
  }

  public static enum SuiteMethodTreatment {
    REMOVE("Remove"),
    COMMENT_OUT("Comment out"),
    DONT_TOUCH("Don't touch");

    private String m_label;

    private SuiteMethodTreatment(String label) {
      m_label = label;
    }

    public String getLabel() {
      return m_label;
    }
  };

  public void storeSuiteMethodTreatement(int value) {
    m_storage.setValue(TestNGPluginConstants.S_SUITE_METHOD_TREATMENT, value);
  }

  public SuiteMethodTreatment getSuiteMethodTreatement() {
    int n = m_storage.getInt(TestNGPluginConstants.S_SUITE_METHOD_TREATMENT);
    SuiteMethodTreatment result = SuiteMethodTreatment.REMOVE;
    switch(n) {
      case 1: result = SuiteMethodTreatment.COMMENT_OUT; break;
      case 2: result = SuiteMethodTreatment.DONT_TOUCH; break;
    }

    return result;
  }
}
