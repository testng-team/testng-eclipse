package org.testng.eclipse.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.testng.reporters.XMLStringBuffer;


/**
 * This class/interface 
 */
public class MethodsSuite extends CustomSuite {
    protected Collection m_methodNames;
    protected String m_className;
    protected int m_logLevel;

    public MethodsSuite(final String projectName,
                        final String className,
                        final Collection methodNames,
                        final Map parameters,
                        final String annotationType,
                        final int logLevel) {
      super(projectName, className, parameters, annotationType);

      m_className= className;
      m_methodNames= methodNames;
      m_logLevel= logLevel;
    }

    protected String getTestName() {
      return m_className;
    }
    
    protected void initContentBuffer(XMLStringBuffer suiteBuffer) {
      Properties testAttrs= new Properties();
      testAttrs.setProperty("name", getTestName());
      if(m_annotationType != null) {
        testAttrs.setProperty("annotations", m_annotationType);
      }
      testAttrs.setProperty("verbose", String.valueOf(m_logLevel));

      suiteBuffer.push("test", testAttrs);

      suiteBuffer.push("classes");

      Properties classAttrs= new Properties();
      classAttrs.setProperty("name", m_className);

      if((null != m_methodNames) && (m_methodNames.size() > 0)) {
        suiteBuffer.push("class", classAttrs);

        suiteBuffer.push("methods");

        for(Iterator it = m_methodNames.iterator(); it.hasNext(); ) {
          String methodName= (String) it.next();
          Properties methodAttrs= new Properties();
          methodAttrs.setProperty("name", (String) methodName);
          suiteBuffer.addEmptyElement("include", methodAttrs);
        }

        suiteBuffer.pop("methods");
        suiteBuffer.pop("class");
      }
      else {
        suiteBuffer.addEmptyElement("class", classAttrs);
      }
      suiteBuffer.pop("classes");
      suiteBuffer.pop("test");
    }
}
