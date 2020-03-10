package org.testng.eclipse.util.param;

import java.io.IOException;
import java.io.InputStream;
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
import org.testng.eclipse.TestNGPluginConstants;
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
  public static Map<String, String> solveParameters(IJavaElement[] javaElements) {
    Map<String, String> paramNames = new HashMap<>();

    if (null == javaElements || javaElements.length == 0) {
      return paramNames;
    }

    try {
      for(IJavaElement javaElement : javaElements) {
        Map<String, String> params = getParameterNames(javaElement);
        paramNames.putAll(params);
      }

      if(paramNames.isEmpty()) {
        return paramNames;
      }

      return findParameterValues(javaElements[0].getAncestor(IJavaElement.JAVA_PROJECT).getCorrespondingResource(), paramNames);
    }
    catch(JavaModelException jmex) {
      TestNGPlugin.log(jmex);
    }

    return paramNames;
  }

  private static Map<String, String> getParameterNames(IJavaElement javaElement) throws JavaModelException {
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
        return new HashMap<>();
    }
  }

  private static Map<String, String> solveParameters(IPackageFragment packageFragment) throws JavaModelException {
    return parseParameterNames(packageFragment.getCompilationUnits(), new TestNGMethodParameterVisitor());
  }

  private static Map<String, String> solveParameters(ICompilationUnit compilationUnit) throws JavaModelException {
    return parseParameterNames(new ICompilationUnit[] {compilationUnit}, new TestNGMethodParameterVisitor());
  }

  private static Map<String, String> solveParameters(IType type) {
    return parseParameterNames(new ICompilationUnit[] {type.getCompilationUnit()}, new TestNGMethodParameterVisitor(type));
  }

  private static Map<String, String> solveParameters(IMethod method) throws JavaModelException {
    if(method.getNumberOfParameters() > 0) {
      return parseParameterNames(new ICompilationUnit[] {method.getCompilationUnit()}, new TestNGMethodParameterVisitor(method));
    }

    return new HashMap<>();
  }

  protected static Map<String, String> parseParameterNames(ICompilationUnit[] units, TestNGMethodParameterVisitor visitor) {
    for(ICompilationUnit unit : units) {
      ASTNode node= getParserNode(unit);
      node.accept(visitor);
    }

    return visitor.getParametersMap();
  }

  protected static ASTNode getParserNode(ICompilationUnit unit) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setSource(unit);
    return parser.createAST(null);
  }

  private static Map<String, String> findParameterValues(IResource projectRes, Map<String, String> parameters) {
    IResource[] suiteFiles= searchSuites(new IResource[] {projectRes});
    IFile selectedSuite= null;

    if (suiteFiles.length == 0) {
    	// No parameters.  If they're all @Optional, this will work anyway.
    	// Otherwise, this will ultimately cause the test to fail with a clear error.
    	return new HashMap<>();
    }

    if(suiteFiles.length > 1) {
      selectedSuite= showSelectionDialog(suiteFiles);
    }
    else {
      selectedSuite= (IFile) suiteFiles[0];
    }

    if (selectedSuite == null) {
      return new HashMap<>();
    }
    return extractParameterValues(selectedSuite, parameters);
  }

  private static Map<String, String> extractParameterValues(IFile file, Map<String, String> parameters) {
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

      if (null == spf) {
        return parameters;
      }

      spf.setValidating(true);
      SAXParser saxParser = spf.newSAXParser();
      saxParser.parse(is, pvch);
    }
    catch(ParserConfigurationException | SAXException | IOException | CoreException ex) {
      TestNGPlugin.log(ex);
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
    ISearchQuery query= new FileSearchQuery("<!DOCTYPE suite SYSTEM \"(http|https)://testng.org/testng-1.0.dtd\" >", 
                                            true /*regexp*/ , 
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
    private Map<String, String> m_params;

    public ParameterValuesContentHandler(Map<String, String> parameters) {
      m_params= parameters;
    }

    @Override
    public InputSource resolveEntity(String systemId, String publicId) throws SAXException {
      InputSource result = null;
      if (TestNGPluginConstants.DEPRECATED_TESTNG_DTD_URL.equals(publicId) || TestNGPluginConstants.TESTNG_DTD_URL.equals(publicId)) {
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
