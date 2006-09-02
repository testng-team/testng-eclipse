package org.testng.eclipse.ui;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.testng.eclipse.buildpath.BuildPathSupport;
import org.testng.eclipse.util.ResourceUtil;

public class TestNGAddLibraryProposal implements IJavaCompletionProposal {

  private IInvocationContext invocationContext;

  private boolean importTestNGAnnotationPackage;

  private int relevance;

  public TestNGAddLibraryProposal(IInvocationContext context, int relevance) {
    this(context, relevance, false);
  }

  public TestNGAddLibraryProposal(IInvocationContext context,
                                  int relevance,
                                  boolean alsoImportTestNGAnnotationPackage) {
    this.invocationContext = context;
    this.relevance = relevance;
    this.importTestNGAnnotationPackage = alsoImportTestNGAnnotationPackage;
  }

  public int getRelevance() {
    return relevance;
  }

  public void apply(IDocument document) {
    IJavaProject project = invocationContext.getCompilationUnit().getJavaProject();
    IClasspathEntry entry = BuildPathSupport.getTestNGClasspathEntry();
    if (entry != null) {
      Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      try {
        addToClasspath(shell, project, entry, new BusyIndicatorRunnableContext());
      }
      catch (JavaModelException e) {
        ErrorDialog.openError(shell,
                              ResourceUtil.getString("AddTestNGLibraryProposal.error"), //$NON-NLS-1$
                              ResourceUtil.getString("AddTestNGLibraryProposal.cannotAdd"), //$NON-NLS-1$
                              e.getStatus());

      }
    }
    forceReconcile(document);
  }

  private void forceReconcile(IDocument document) {
    try {
      // force a reconcile
      int offset = invocationContext.getSelectionOffset();
      int length = invocationContext.getSelectionLength();
      String s = document.get(offset, length);
      document.replace(offset, length, s);
    }
    catch (BadLocationException e) {
      // ignore
    }
  }

  public String getAdditionalProposalInfo() {
    return ResourceUtil.getString("AddTestNGLibraryProposal.info"); //$NON-NLS-1$
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public String getDisplayString() {
    return ResourceUtil.getString("AddTestNGLibraryProposal.label"); //$NON-NLS-1$
  }

  public Image getImage() {
    return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_LIBRARY);
  }

  public Point getSelection(IDocument document) {
    // TODO Auto-generated method stub
    return null;
  }

  private static boolean addToClasspath(Shell shell,
                                        final IJavaProject project,
                                        IClasspathEntry entry,
                                        IRunnableContext context) throws JavaModelException {
    if (BuildPathSupport.projectContainsClasspathEntry(project, entry)) {
      // We don't need to add the TestNG Classpath Entry again
      return true;
    }
    IClasspathEntry[] oldEntries = project.getRawClasspath();
    int nEntries = oldEntries.length;
    final IClasspathEntry[] newEntries = new IClasspathEntry[nEntries + 1];
    System.arraycopy(oldEntries, 0, newEntries, 0, nEntries);
    newEntries[nEntries] = entry;
    try {
      context.run(true, false, new IRunnableWithProgress() {
          public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
              project.setRawClasspath(newEntries, monitor);
            }
            catch (JavaModelException e) {
              throw new InvocationTargetException(e);
            }
          }
        });

      return true;
    }
    catch (InvocationTargetException e) {
      Throwable t = e.getTargetException();
      if (t instanceof CoreException) {
        ErrorDialog.openError(shell,
                              ResourceUtil.getString("AddTestNGLibraryProposal.error"), //$NON-NLS-1$
                              ResourceUtil.getString("AddTestNGLibraryProposal.cannotAdd"), //$NON-NLS-1$
                              ((CoreException) t).getStatus());
      }

      return false;
    }
    catch (InterruptedException e) {
      return false;
    }

  }

}
