package org.testng.eclipse.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.testng.TestNG;
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
  public void storeOutputDir(String projectName, String outdir) {
    m_storage.setValue(projectName + TestNGPluginConstants.S_OUTDIR, outdir);
  }

  public void storeDisabledListeners(String projectName, boolean selection) {
    m_storage.setValue(projectName + ".disabledListeners", selection);
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
    return new Path(project.getPath().toOSString() + "/" + outdir);
  }

  public IPath getOutputAbsolutePath(IJavaProject project) {
    final String projectName= project.getElementName();
    final String outdir= getOutputDir(projectName, false);
    
    return new Path(project.getProject().getLocation().toOSString() + "/" + outdir);
  }

  public String getOutputDir(String projectName, boolean projectOnly) {
    String result = getString(projectName, projectOnly, TestNGPluginConstants.S_OUTDIR);

    return result.isEmpty() ? TestNG.DEFAULT_OUTPUTDIR : result;
  }

  private String getString(String projectName, boolean projectOnly, String prefName) {
    String result = "";
    if (m_storage.contains(projectName + prefName)) {
      result = m_storage.getString(projectName + prefName);
    }
    if (StringUtils.isEmptyString(result) && !projectOnly) {
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
