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
    m_storage.setValue(projectName + TestNGPluginConstants.S_USEPROJECTJAR, selection);
  }
  
  public void storeXmlTemplateFile(String projectName, String xmlFile) {
    m_storage.setValue(projectName + TestNGPluginConstants.S_XML_TEMPLATE_FILE, xmlFile);
  }

  public File getTemporaryDirectory() {
    Random r = new Random(System.currentTimeMillis());
    File result = new File(System.getProperty("java.io.tmpdir") + File.separatorChar
        + "testng-eclipse-" + r.nextInt());

    return result;
  }
  
  public String getXmlTemplateFile(String projectName, boolean projectOnly) {
    String result = m_storage.getString(projectName + TestNGPluginConstants.S_XML_TEMPLATE_FILE);
    if (Utils.isEmptyString(result) && ! projectOnly) {
      result = m_storage.getString(TestNGPluginConstants.S_XML_TEMPLATE_FILE);
    }

    return result;
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
    if(projectOnly || m_storage.contains(projectName + TestNGPluginConstants.S_OUTDIR)) {
      return m_storage.getString(projectName + TestNGPluginConstants.S_OUTDIR);
    }
    if(m_storage.contains(TestNGPluginConstants.S_DEPRECATED_OUTPUT)) {
      m_storage.setValue(TestNGPluginConstants.S_DEPRECATED_OUTPUT, "");
      m_storage.setValue(TestNGPluginConstants.S_OUTDIR,
          m_storage.getString(TestNGPluginConstants.S_DEPRECATED_OUTPUT));
    }
    String outdir= m_storage.getString(TestNGPluginConstants.S_OUTDIR); 
    return !"".equals(outdir) ? outdir : TestNG.DEFAULT_OUTPUTDIR;
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
    if(projectOnly || m_storage.contains(projectName + prefName)) {
      return m_storage.getString(projectName + prefName);
    }
    else {
      return m_storage.getString(prefName);
    }
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
    return m_storage.getBoolean(projectName + TestNGPluginConstants.S_USEPROJECTJAR);
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
}
