package org.testng.eclipse.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.testng.eclipse.TestNGPluginConstants;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.ui.util.TypeParser;
import org.testng.eclipse.util.signature.IMethodDescriptor;
import org.testng.eclipse.util.signature.MethodDescriptor;
import org.testng.reporters.XMLStringBuffer;
import org.testng.xml.Parser;

/**
 * Utility class that builds a temporary suite definition file.
 * 
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class SuiteBuilder {

  public static File createSuite(IJavaProject ijp, IType[] types, IJavaElement ije) {
    
    XMLStringBuffer xmlSuite = getSuiteBuffer(ije.getElementName());
    
    if(IJavaElement.COMPILATION_UNIT == ije.getElementType()) {
      createClassTest(types, ije, xmlSuite);
    } 
    else if(IJavaElement.TYPE == ije.getElementType()) {
      createClassTest(types, ije, xmlSuite);
    } 
    else if(IJavaElement.METHOD == ije.getElementType()) {
      createMethodTest(types, ije, xmlSuite);
    }

    xmlSuite.pop("suite");
    final String projectPath = ijp.getProject().getLocation().toOSString();
    
    File file = new File(projectPath, ije.getElementName() + ".xml");
    ppp("saving to file " + file.getAbsolutePath());


    saveFileContent(file, xmlSuite);
    return file;
  }

//  private static String convert(String annotationType) {
//    AnnotationTypeEnum annoType= AnnotationTypeEnum.JDK;
//    try {
//      annoType= AnnotationTypeEnum.valueOf(annotationType);
//    }
//    catch(RuntimeException re) {
//      TestNGPlugin.log(new Status(IStatus.INFO, 
//                                  TestNGPlugin.PLUGIN_ID, 
//                                  1, 
//                                  "Unknown annotation type '" + annotationType + "' Using default: " + TestNG.JDK_ANNOTATION_TYPE, 
//                                  null));
//    }
//    
//    return annoType.toString();
//  }
  
  private static void createClassTest(IType[] types, IJavaElement ije, XMLStringBuffer buf) {
    String testName = "Test " + ije.getElementName();
    Properties attrs = new Properties();
    attrs.setProperty("name", testName);
    
    Properties clsAttrs = new Properties();
    for(int i = 0; i < types.length; i++) {
      ITestContent content = TypeParser.parseType(types[i]);
      
      if(content.hasTestMethods()) {
//        attrs.setProperty("annotations", convert(content.getAnnotationType()));
        
        if(i == 0) {
          buf.push("test", attrs);
          buf.push("classes");
        }
        
        clsAttrs.setProperty("name", types[i].getFullyQualifiedName());
        buf.push("class", clsAttrs);
        buf.pop("class");
      }
    }
    
    buf.pop("classes");
    buf.pop("test");
  }
  
  private static void createMethodTest(IType[] types, IJavaElement ije, XMLStringBuffer buf) {
    String testName = "Method test " + ije.getElementName();
    Properties attrs = new Properties();
    attrs.setProperty("name", testName);
    
    IType type = types[0];
    ITestContent content = TypeParser.parseType(type);
    if(!content.hasTestMethods()) {
      return;
    }

    Set<IMethodDescriptor> testMethods = content.getTestMethods();
    IMethodDescriptor testMethodDescriptor = new MethodDescriptor((IMethod) ije);
    Properties methodAttrs = new Properties(); 
    for(IMethodDescriptor imd : testMethods) {
      if(imd.equals(testMethodDescriptor)) {
//        attrs.setProperty("annotations", convert(imd.getAnnotationType()));
        methodAttrs.setProperty("name", imd.getName());
      }
    }
    
    buf.push("test", attrs);
    buf.push("methods");
    buf.push("method", methodAttrs);
    buf.pop("method");
    buf.pop("methods");
    
    buf.push("classes");
    Properties clsAttrs = new Properties();
    clsAttrs.setProperty("name", type.getFullyQualifiedName());
    buf.push("class", clsAttrs);
    buf.pop("class");
    buf.pop("classes");
    buf.pop("test");
  }
  
  private static XMLStringBuffer getSuiteBuffer(String name) {
    XMLStringBuffer xmlBuf = new XMLStringBuffer(); //$NON-NLS-1$
    xmlBuf.setDocType("suite SYSTEM " + TestNGPluginConstants.TESTNG_DTD_URL);
    Properties attrs = new Properties();
    attrs.setProperty("name", name);
    xmlBuf.push("suite", attrs);

    return xmlBuf;
  }
  
  private static final void saveFileContent(final File file, final XMLStringBuffer xmlBuffer) {
    FileWriter fw = null;
    BufferedWriter bw = null;
    try {
      fw = new FileWriter(file);
      bw = new BufferedWriter(fw);
      bw.write(xmlBuffer.getStringBuffer().toString());
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
          ;
        }
      }
      if(null != fw) {
        try {
          fw.close();
        }
        catch(IOException ioe) {
          ;
        }
      }
      bw = null;
      fw = null;
    }
  }
  
  private static void ppp(Object msg) {
    System.out.println("[SuiteBuilder]: " + msg);
  }
}
