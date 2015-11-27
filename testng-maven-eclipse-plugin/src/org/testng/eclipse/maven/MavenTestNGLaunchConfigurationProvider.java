package org.testng.eclipse.maven;

import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.ITestNGLaunchConfigurationProvider;
import org.testng.eclipse.launch.LaunchConfigurationHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MavenTestNGLaunchConfigurationProvider implements ITestNGLaunchConfigurationProvider {

  @Override
  public String getVmArguments(ILaunchConfiguration configuration) throws CoreException {
    String vmArgs = null;
    try {
      if (PreferenceUtils.getBoolean(Activator.PREF_ARGLINE)) {
        vmArgs = getVMArgsFromPom(configuration);
      }
    } catch (Exception e) {
      throw new CoreException(TestNGPlugin.createError(e));
    }

    if (vmArgs != null) {
      vmArgs = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(vmArgs);
    }
    return vmArgs;
  }

  @Override
  public List<String> getEnvironment(ILaunchConfiguration configuration) throws CoreException {
    return null;
  }

  /**
   * Get the JVM args from maven pom.xmll.
   * <ul>
   * Here is the return value of different cases:
   * <li>no pom.xml -- return empty String</li>
   * <li>pom.xml exists, but no maven-surefire-plugin or maven-safefail-plugin,
   * in essential, no "argLine" element in the pom.xml -- return empty String
   * </li>
   * <li>there is one "argLine" element -- return the text content of the
   * "argLine" element</li>
   * <li>there are more then one "argLine" elements -- return the first
   * "argLine"<profile></li>
   * </ul>
   * 
   * @param conf
   * @return
   * @throws Exception
   */
  private static String getVMArgsFromPom(ILaunchConfiguration conf) throws Exception {
    StringBuilder vmArgs = new StringBuilder();
    IJavaProject javaProject = LaunchConfigurationHelper.getProject(conf);
    IFile pomFile = javaProject.getProject().getFile("pom.xml");
    if (pomFile.exists()) {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(false);
      DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
      Document doc = documentBuilder.parse(pomFile.getLocation().toFile());

      XPathFactory xpathFactory = XPathFactory.newInstance();
      XPath xpath = xpathFactory.newXPath();

      XPathExpression expr = xpath.compile("//argLine");
      NodeList argLineNodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
      if (argLineNodes.getLength() > 0) {
        Node argLineNode = argLineNodes.item(0);
        vmArgs.append(" ").append(argLineNode.getTextContent());
      }
    }
    return vmArgs.toString();
  }

}
