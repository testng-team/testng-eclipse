package org.testng.eclipse.refactoring;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.Utils;
import org.testng.internal.Yaml;
import org.testng.xml.Parser;
import org.testng.xml.XmlSuite;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Convert a TestNG XML file to YAML.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class ConvertToYamlAction extends AbstractHandler {

  public Object execute(ExecutionEvent event) throws ExecutionException {
    IStructuredSelection selection =
        (IStructuredSelection) HandlerUtil.getActiveMenuSelection(event);
    Iterator it = selection.iterator();
    while (it.hasNext()) { 
      Object o = it.next();
      if (o instanceof IAdaptable) {
        IFile file = (IFile) ((IAdaptable) o).getAdapter(IFile.class);
        if (file != null) {
          IPath location = file.getLocation();
          Parser p = new Parser(location.toOSString());
          p.setLoadClasses(false);

          try {
            Collection<XmlSuite> suites = p.parse();
            StringBuilder yamlSb = Yaml.toYaml(suites.iterator().next());

            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IPath yamlPath =
                new Path(file.getProject().getName() + "/"
                    + file.getProjectRelativePath().toString().replace(".xml", ".yaml"));
            IFile yamlFile = root.getFile(yamlPath);
            ByteArrayInputStream is = new ByteArrayInputStream(yamlSb.toString().getBytes("UTF-8"));
            yamlFile.create(is, true /* force */, new NullProgressMonitor());
            Utils.openFile(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                yamlFile, new NullProgressMonitor());
          } catch (IOException e1) {
            TestNGPlugin.log(e1);
          } catch (CoreException e) {
            TestNGPlugin.log(e);
          }
        }
      }
    }

    return null;
  }

}
