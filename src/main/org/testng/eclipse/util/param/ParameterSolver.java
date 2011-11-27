package org.testng.eclipse.util.param;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.ui.util.SuiteListSelectionDialog;
import org.testng.xml.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class ParameterSolver {
  /**
   * TODO: this method searches only for parameters of the current <code>javaElement</code>
   * and not all the dependencies.
   */
  public static Map solveParameters(IJavaElement[] javaElements) {
    if(null == javaElements || javaElements.length == 0) return null;
    
    Map paramNames= new HashMap();
    try {
      for(int i= 0; i < javaElements.length; i++) {
        Map params= getParameterNames(javaElements[i]);
        if(null != params) {
          paramNames.putAll(params);
        }
      }
      
      if(paramNames.isEmpty()) {
        return null;
      }
      
      return findParameterValues(javaElements[0].getAncestor(IJavaElement.JAVA_PROJECT).getCorrespondingResource(), paramNames);
    }
    catch(JavaModelException jmex) {
      TestNGPlugin.log(jmex);
    }
    
    return paramNames;
  }

  private static Map getParameterNames(IJavaElement javaElement) throws JavaModelException {
    switch(javaElement.getElementType()) {
      case IJavaElement.PACKAGE_FRAGMENT:
      {
        return solveParameters((IPackageFragment) javaElement);
      }
      
      case IJavaElement.COMPILATION_UNIT:
      {
        return solveParameters((ICompilationUnit) javaElement);
      }
      
      case IJavaElement.TYPE:
      {
        return solveParameters((IType) javaElement);
      }
      
      case IJavaElement.METHOD:
      {
        return solveParameters((IMethod) javaElement);
      }
      
      default:
        return null;
    }
  }
  
  private static Map solveParameters(IPackageFragment packageFragment) throws JavaModelException {
    return parseParameterNames(packageFragment.getCompilationUnits(), new TestNGMethodParameterVisitor());
  }
  
  private static Map solveParameters(ICompilationUnit compilationUnit) throws JavaModelException {
    return parseParameterNames(new ICompilationUnit[] {compilationUnit}, new TestNGMethodParameterVisitor());
  }
  
  private static Map solveParameters(IType type) {
    return parseParameterNames(new ICompilationUnit[] {type.getCompilationUnit()}, new TestNGMethodParameterVisitor(type));
  }
  
  private static Map solveParameters(IMethod method) throws JavaModelException {
    if(method.getNumberOfParameters() > 0) {
      return parseParameterNames(new ICompilationUnit[] {method.getCompilationUnit()}, new TestNGMethodParameterVisitor(method));
    }
    
    return null;
  }
  
  protected static Map parseParameterNames(ICompilationUnit[] units, TestNGMethodParameterVisitor visitor) {
    for(int i= 0; i < units.length; i++) {
      ASTNode node= getParserNode(units[i]);
      node.accept(visitor);
    }
    
    return visitor.hasParameters() ? visitor.getParametersMap() : null;
  }
  
  protected static ASTNode getParserNode(ICompilationUnit unit) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setSource(unit);
    return parser.createAST(null);
  }
  
  private static Map findParameterValues(IResource projectRes, Map parameters) {
    IResource[] suiteFiles= searchSuites(new IResource[] {projectRes});
    IFile selectedSuite= null;
    
    if (suiteFiles.length == 0) {
    	// No parameters.  If they're all @Optional, this will work anyway.
    	// Otherwise, this will ultimately cause the test to fail with a clear error.
    	return new HashMap();
    }   
    
    if(suiteFiles.length > 1) {
      selectedSuite= showSelectionDialog(suiteFiles);
    }
    else {
      selectedSuite= (IFile) suiteFiles[0];
    }

    return selectedSuite != null
        ? extractParameterValues(selectedSuite, parameters)
        : Collections.emptyMap();
  }
  
  private static Map extractParameterValues(IFile file, Map parameters) {
    try {
      InputStream is= file.getContents();
      ParameterValuesContentHandler pvch= new ParameterValuesContentHandler(parameters);
      SAXParserFactory spf = null;
      try {
        spf = SAXParserFactory.newInstance();
      }
      catch(FactoryConfigurationError ex) {
        // If running with JDK 1.4
        try {
          Class cl = Class.forName("org.apache.crimson.jaxp.SAXParserFactoryImpl");
          spf = (SAXParserFactory) cl.newInstance();
        }
        catch(Exception ex2) {
          TestNGPlugin.log(ex2);
        }
      }
      
      if(null == spf) {
        return null;
      }
      
      spf.setValidating(true);
      SAXParser saxParser = spf.newSAXParser();
      saxParser.parse(is, pvch);
    }
    catch(CoreException cex) {
      TestNGPlugin.log(cex);
    }
    catch(ParserConfigurationException pcex) {
      TestNGPlugin.log(pcex);
    }
    catch(SAXException saxex) {
      TestNGPlugin.log(saxex);
    }
    catch(IOException ioex) {
      TestNGPlugin.log(ioex);
    }
    
    return parameters;
  }
  
  protected static IFile showSelectionDialog(IResource[] choices) {
    final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      return null;
    }
    final Shell parent = window.getShell();

    final SuiteListSelectionDialog dialog = new SuiteListSelectionDialog(parent, choices);
    dialog.setTitle("Select parameter definition file");
    final int resultCode = dialog.open();
    if (resultCode != IDialogConstants.OK_ID) {
      return null;
    }

    final Object[] result = dialog.getResult();
    if (result == null || result.length == 0 || result[0] instanceof IFile == false) {
      return null;
    }

    return (IFile) result[0];
  }
  
  protected static IResource[] searchSuites(IResource[] scopeResources) {
    ISearchQuery query= new FileSearchQuery("<!DOCTYPE suite SYSTEM \"http://testng.org/testng-1.0.dtd\" >", 
                                            false /*regexp*/ , 
                                            false /*casesensitive*/, 
                                            FileTextSearchScope.newSearchScope(scopeResources, new String[] {"*.xml"}, false));
    query.run(null);
    FileSearchResult result= (FileSearchResult) query.getSearchResult(); 
    Object[] elements= result.getElements();
    IResource[] resources= new IResource[elements.length];
    for(int i= 0; i < elements.length; i++) {
      resources[i]= (IResource) elements[i];
    }
    
    return resources;
  }
  
  static class ParameterValuesContentHandler extends DefaultHandler {
    private Map m_params;
    
    public ParameterValuesContentHandler(Map parameters) {
      m_params= parameters;
    }
    
    @Override
    public InputSource resolveEntity(String systemId, String publicId) throws SAXException {
      InputSource result = null;
      if (Parser.DEPRECATED_TESTNG_DTD_URL.equals(publicId) || Parser.TESTNG_DTD_URL.equals(publicId)) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(Parser.TESTNG_DTD);
        if (null == is) {
          is = Thread.currentThread().getContextClassLoader().getResourceAsStream(Parser.TESTNG_DTD);
          if (null == is) {
            System.out.println("WARNING: couldn't find in classpath " + publicId + "\n" + "Fetching it from the Web site.");
            try {
              result = super.resolveEntity(systemId, publicId);
            }
            catch(Exception ex) {
              ex.printStackTrace();
            }
          }
          else {
            result = new InputSource(is);
          }
        }
        else {
          result = new InputSource(is);
        }
      }
      else {
        try {
          result = super.resolveEntity(systemId, publicId);
        }
        catch(Exception ex) {
              ex.printStackTrace();
            }
      }

      return result;
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      String name = attributes.getValue("name");

      if ("parameter".equals(qName) && m_params.containsKey(name)) {
        m_params.put(name, attributes.getValue("value")); 
      }
    }
  }
}
