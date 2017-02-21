package org.testng.eclipse.wizards;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.StringUtils;
import org.testng.eclipse.util.SuiteGenerator;
import org.testng.eclipse.util.Utils.JavaElement;

/**
 * The wizard that creates a new TestNG class. This wizard looks at the current
 * selected class and prefills some of its pages based on the information found
 * in these classes.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class NewTestNGClassWizard extends Wizard implements INewWizard {
	private NewTestNGClassWizardPage m_page;
  private TestNGMethodWizardPage m_methodPage;

	/**
	 * Constructor for NewTestNGClassWizard.
	 */
	public NewTestNGClassWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	private boolean hasAtLeastOneMethod(List<JavaElement> elements) {
	  for (JavaElement je : elements) {
	    if (je.compilationUnit != null) return true;
	  }
	  return false;
	}

	/**
	 * Adding the pages to the wizard.
	 */
	@Override
  public void addPages() {
    List<JavaElement> elements = org.testng.eclipse.util.Utils.getSelectedJavaElements();
		if (hasAtLeastOneMethod(elements)) {
		  m_methodPage = new TestNGMethodWizardPage(elements);
		  addPage(m_methodPage);
		}
		m_page = new NewTestNGClassWizardPage();
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
		List<IMethod> methods = m_methodPage != null
		    ? m_methodPage.getSelectedMethods() : Collections.<IMethod>emptyList();
    try {
      return doFinish(containerName, packageName, className, m_page.getXmlFile(), methods,
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
	 *
	 * @return true if the operation succeeded, false otherwise.
	 */
	private boolean doFinish(String containerName, String packageName, String className,
	    String xmlPath, List<IMethod> methods, IProgressMonitor monitor) throws CoreException {
	  boolean result = true;

	  //
	  // Create XML file at the root directory, if applicable
	  //
	  if (!StringUtils.isEmptyString(xmlPath)) {
	    IFile file = createFile(containerName, "", xmlPath, createXmlContentStream(), monitor);
	    if (file != null) org.testng.eclipse.util.Utils.openFile(getShell(), file, monitor);
	    else result = false;
	  }

	  //
	  // Create Java file
	  //
	  if (result) {
  	  IFile file = createFile(containerName, packageName, className + ".java",
          createJavaContentStream(className, methods), monitor);
  	  if (file != null) org.testng.eclipse.util.Utils.openFile(getShell(), file, monitor);
  	  else result = false;
	  }

	  return result;
	}

  private IFile createFile(String containerName, String packageName, String fileName,
	    InputStream contentStream, IProgressMonitor monitor) throws CoreException {
    monitor.beginTask("Creating " + fileName, 2);
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    String fullPath = fileName;
    if (packageName != null && ! "".equals(packageName)) {
      fullPath = packageName.replace(".", File.separator) + File.separatorChar + fileName;
    }
    Path absolutePath = new Path(containerName + File.separatorChar + fullPath);
    final IFile result = root.getFile(absolutePath);
    Utils.createFileWithDialog(getShell(), result, contentStream);

    return result;
	}

	/**
	 * Create the content for the Java file.
	 * @param testMethods 
	 */
	private InputStream createJavaContentStream(String className, List<IMethod> testMethods) {
	  StringBuilder imports = new StringBuilder("import org.testng.annotations.Test;\n");
	  StringBuilder methods = new StringBuilder();
	  String dataProvider = "";
	  String signature = "()";

	  //
	  // Configuration methods
	  //
	  for (String a : NewTestNGClassWizardPage.ANNOTATIONS) {
	    if (!"".equals(a) && m_page.containsAnnotation(a)) {
	      imports.append("import org.testng.annotations." + a + ";\n");
	      if ("DataProvider".equals(a)) {
	        dataProvider = "(dataProvider = \"dp\")";
	        methods.append("\n  @DataProvider\n"
	            + "  public Object[][] dp() {\n"
	            + "    return new Object[][] {\n"
	            + "      new Object[] { 1, \"a\" },\n"
              + "      new Object[] { 2, \"b\" },\n"
              + "    };\n"
              + "  }\n"
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

	  //
	  // Test methods
	  //
	  Set<String> overloadedMethods = new HashSet<>();
	  Set<String> temp = new HashSet<>();
	  for (IMethod m : testMethods) {
	    String name = m.getElementName();
      if (temp.contains(name)) overloadedMethods.add(name);
	    temp.add(name);
	  }

    for (IMethod m : testMethods) {
      methods.append("\n"
          + "  @Test\n"
          + "  public void " + createSignature(m, overloadedMethods) + " {\n"
          + "    throw new RuntimeException(\"Test not implemented\");\n"
          + "  }\n");
    }

    StringBuilder contents = new StringBuilder();
    if (!StringUtils.isEmptyString(m_page.getPackageName())) {
      contents.append("package " + m_page.getPackageName() + ";\n\n");
    }

    contents.append(imports).append("\n");
    contents.append("public class " + className + " {\n");

    if (testMethods.size() == 0 || ! StringUtils.isEmptyString(dataProvider)) {
      contents.append(
          "  @Test" + dataProvider + "\n"
  	      + "  public void f" + signature + " {\n"
  	      + "  }\n");
    }

    contents.append(methods + "}\n");

	  return new ByteArrayInputStream(contents.toString().getBytes());
	}

	/**
	 * @return a suitable signature, possible with its name mangled if it's
	 * overloaded in the class (e.g foo() -> foo(), foo(Integer) -> fooInteger()).
	 */
  private String createSignature(IMethod m, Set<String> overloadedMethods) {
    String elementName = m.getElementName();
    StringBuilder result = new StringBuilder(elementName);
    if (overloadedMethods.contains(elementName)) {
      for (String type : m.getParameterTypes()) {
        result.append(sanitizeSignature(Signature.toString(type)));
      }
    }
    result.append("()");
    return result.toString();
  }

  /**
   * @return a string that can be used as a method name.
   */
  private String sanitizeSignature(String string) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);
      if (Character.isJavaIdentifierPart(c)) {
        result.append(c);
      }
    }

    return result.toString();
  }

  /**
   * Create the content for the XML file.
   */
	private InputStream createXmlContentStream() {
	  String cls = m_page.getClassName();
	  String pkg = m_page.getPackageName();
	  String className = StringUtils.isEmptyString(pkg) ? cls : pkg + "." + cls;
	  return new ByteArrayInputStream(
	      SuiteGenerator.createSingleClassSuite(className).getBytes());
	}

	private String toMethod(String a) {
    return Character.toLowerCase(a.charAt(0)) + a.substring(1);
  }

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
  public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

}