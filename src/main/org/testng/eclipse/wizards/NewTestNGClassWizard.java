package org.testng.eclipse.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.SuiteGenerator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * This is a sample new wizard. Its role is to create a new file 
 * resource in the provided container. If the container resource
 * (a folder or a project) is selected in the workspace 
 * when the wizard is opened, it will accept it as the target
 * container. The wizard creates one file with the extension
 * "java". If a sample multi-page editor (also available
 * as a template) is registered for the same extension, it will
 * be able to open it.
 */
public class NewTestNGClassWizard extends Wizard implements INewWizard {
	private NewTestNGClassWizardPage m_page;
	private ISelection m_selection;

	/**
	 * Constructor for NewTestNGClassWizard.
	 */
	public NewTestNGClassWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	@Override
  public void addPages() {
		m_page = new NewTestNGClassWizardPage(m_selection);
		addPage(m_page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	@Override
  public boolean performFinish() {
		String containerName = m_page.getSourceFolder();
		String className = m_page.getClassName();
		String packageName = m_page.getPackageName();
    try {
      doFinish(containerName, packageName, className, m_page.getXmlFile(),
          new NullProgressMonitor());
    } catch (CoreException e) {
      e.printStackTrace();
    }
//		IRunnableWithProgress op = new IRunnableWithProgress() {
//			public void run(IProgressMonitor monitor) throws InvocationTargetException {
//				try {
//					doFinish(containerName, fileName, monitor);
//				} catch (CoreException e) {
//					throw new InvocationTargetException(e);
//				} finally {
//					monitor.done();
//				}
//			}
//		};
//		try {
//			getContainer().run(true /* fork */, false /* cancelable */, op);
//		} catch (InterruptedException e) {
//			return false;
//		} catch (InvocationTargetException e) {
//			Throwable realException = e.getTargetException();
//			MessageDialog.openError(getShell(), "Error", realException.getMessage());
//			return false;
//		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file(s) if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */
	private void doFinish(String containerName, String packageName, String className,
	    String xmlPath, IProgressMonitor monitor) throws CoreException {
	  //
	  // Create XML file at the root directory, if applicable
	  //
	  if (!Utils.isEmpty(xmlPath)) {
	    openFile(createFile(containerName, "", xmlPath, createXmlContentStream(), monitor), monitor);
	  }

	  //
	  // Create Java file
	  //
	  openFile(createFile(containerName, packageName, className + ".java",
	      createJavaContentStream(className), monitor), monitor);
	}

  private void openFile(final IFile javaFile, IProgressMonitor monitor) {
    monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
      public void run() {
				IWorkbenchPage page =
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, javaFile, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
  }
	
	private IFile createFile(String containerName, String packageName, String fileName,
	    InputStream contentStream, IProgressMonitor monitor) throws CoreException {
    monitor.beginTask("Creating " + fileName, 2);
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IResource resource = root.findMember(new Path(containerName));
    if (!resource.exists() || !(resource instanceof IContainer)) {
      throwCoreException("Container \"" + containerName + "\" does not exist.");
    }
    IContainer container = (IContainer) resource;
    String fullPath = fileName;
    if (packageName != null && ! "".equals(packageName)) {
      fullPath = packageName.replace(".", File.separator) + File.separatorChar + fileName;
    }
    final IFile result = container.getFile(new Path(fullPath));
    try {
      if (result.exists()) {
        result.setContents(contentStream, true, true, monitor);
      } else {
        createResourceRecursively(result, monitor);
        result.setContents(contentStream, IFile.FORCE | IFile.KEEP_HISTORY, monitor);
//        result.create(contentStream, true, monitor);
      }
      contentStream.close();
    } catch (IOException e) {
    }
    monitor.worked(1);

    return result;
	}

  protected void createResourceRecursively(IResource resource, IProgressMonitor monitor)
      throws CoreException {
    if (resource == null || resource.exists()) return;
    if (!resource.getParent().exists()) createResourceRecursively(resource.getParent(), monitor);
    switch (resource.getType()) {
    case IResource.FILE:
      ((IFile) resource).create(new ByteArrayInputStream(new byte[0]), true, monitor);
      break;
    case IResource.FOLDER:
      ((IFolder) resource).create(IResource.NONE, true, monitor);
      break;
    case IResource.PROJECT:
      ((IProject) resource).create(monitor);
      ((IProject) resource).open(monitor);
      break;
    }
  }

	/**
	 * Create the content for the Java file.
	 */
	private InputStream createJavaContentStream(String className) {
	  StringBuilder imports = new StringBuilder("import org.testng.annotations.Test;\n");
	  StringBuilder methods = new StringBuilder();
	  String dataProvider = "";
	  String signature = "()";
	  for (String a : NewTestNGClassWizardPage.ANNOTATIONS) {
	    if (!"".equals(a) && m_page.containsAnnotation(a)) {
	      imports.append("import org.testng.annotations." + a + ";\n");
	      if ("DataProvider".equals(a)) {
	        dataProvider = "(dataProvider = \"dp\")";
	        methods.append("  @DataProvider\n"
	            + "  public Object[][] dp() {\n"
	            + "    return new Object[][] {\n"
	            + "      new Object[] { 1, \"a\" },\n"
              + "      new Object[] { 2, \"b\" },\n"
              + "    };\n"
              + "  }\n\n"
              );
              ;
            signature = "(Integer n, String s)";
	      } else {
  	      methods.append("  @" + a  + "\n"
  	          + "  public void " + toMethod(a) + "() {\n"
  	          + "  }\n\n"
  	          );
	      }
	    }
	  }
	  String contents =
	      "package " + m_page.getPackage() + ";\n\n"
	      + imports
	      + "\n"
	      + "public class " + className + " {\n"
	      + "  @Test" + dataProvider + "\n"
	      + "  public void f" + signature + " {\n"
	      + "  }\n\n"
	      + methods
	      + "}\n"
	      ;
		return new ByteArrayInputStream(contents.getBytes());
	}

  /**
   * Create the content for the XML file.
   */
	private InputStream createXmlContentStream() {
	  String cls = m_page.getClassName();
	  String pkg = m_page.getPackageName();
	  String className = Utils.isEmpty(pkg) ? cls : pkg + "." + cls;
	  return new ByteArrayInputStream(
	      SuiteGenerator.createSingleClassSuite(className).getBytes());
	}

	private String toMethod(String a) {
    return Character.toLowerCase(a.charAt(0)) + a.substring(1);
  }

  private void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, "org.testng.eclipse", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
  public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.m_selection = selection;
	}
}