package org.testng.eclipse.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.testng.TestNG;
import org.testng.reporters.XMLStringBuffer;
import org.testng.xml.LaunchSuite;
import org.testng.xml.Parser;


/**
 * This class/interface 
 */
abstract public class CustomSuite extends LaunchSuite {
    protected String m_projectName;
    protected String m_suiteName;
    protected String m_annotationType;
    protected Map m_parameters;
    private XMLStringBuffer m_suiteBuffer;

    public CustomSuite(final String projectName,
                       final String suiteName,
                       final Map parameters,
                       final String annotationType) {
      super(true);

      m_projectName= projectName;
      m_suiteName= suiteName;
      m_parameters= parameters;
      if("1.4".equals(annotationType) || TestNG.JAVADOC_ANNOTATION_TYPE.equals(annotationType)) {
        m_annotationType= TestNG.JAVADOC_ANNOTATION_TYPE;
      }
      else {
        m_annotationType= TestNG.JDK_ANNOTATION_TYPE;
      }
    }

    protected XMLStringBuffer createContentBuffer() {
      XMLStringBuffer suiteBuffer= new XMLStringBuffer(""); //$NON-NLS-1$
      suiteBuffer.setDocType("suite SYSTEM \"" + Parser.TESTNG_DTD_URL + "\"");

      Properties attrs= new Properties();
      attrs.setProperty("name", m_suiteName);
      suiteBuffer.push("suite", attrs);

      if(m_parameters != null) {
        for(Iterator it= m_parameters.entrySet().iterator(); it.hasNext(); ) {
          Map.Entry entry= (Map.Entry) it.next();
          Properties paramAttrs= new Properties();
          paramAttrs.setProperty("name", (String) entry.getKey());
          paramAttrs.setProperty("value", (String) entry.getValue());
          suiteBuffer.push("parameter", paramAttrs);
          suiteBuffer.pop("parameter");
        }
      }

      initContentBuffer(suiteBuffer);

      suiteBuffer.pop("suite");
      

      return suiteBuffer;
    }

    private XMLStringBuffer getSuiteBuffer() {
      if(null == m_suiteBuffer) {
        m_suiteBuffer= createContentBuffer();
      }

      return m_suiteBuffer;
    }

    protected abstract void initContentBuffer(XMLStringBuffer suiteBuffer);

    public File save(File directory) {
      final File suiteFile= new File(directory, "temp-testng-customsuite.xml");

      saveSuiteContent(suiteFile, getSuiteBuffer());

      return suiteFile;
    }

    protected void saveSuiteContent(final File file, final XMLStringBuffer content) {
      FileWriter fw= null;
      BufferedWriter bw= null;
      try {
        fw= new FileWriter(file);
        bw= new BufferedWriter(fw);
        bw.write(content.getStringBuffer().toString());
        bw.flush();
      }
      catch(IOException ioe) {
      }
      finally {
        if(null != bw) {
          try {
            bw.close();
          }
          catch(IOException ioe) {
          }
        }
        if(null != fw) {
          try {
            fw.close();
          }
          catch(IOException ioe) {
          }
        }
      }
    }
}
