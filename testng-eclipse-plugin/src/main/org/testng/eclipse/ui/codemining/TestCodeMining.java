package org.testng.eclipse.ui.codemining;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.jface.viewers.StructuredSelection;
import org.testng.eclipse.launch.TestNGLaunchShortcut;

public class TestCodeMining extends LineHeaderCodeMining {

  public TestCodeMining(IJavaElement element, IDocument document,
      ICodeMiningProvider provider, String label, String mode) throws BadLocationException, JavaModelException {
    super(resolveLine(element, document), document, provider,e -> handle(element, mode));
    super.setLabel(label);
    
    
  }

  private static void handle(IJavaElement element, String mode) {
    TestNGLaunchShortcut shortcut = new TestNGLaunchShortcut();
    shortcut.launch(new StructuredSelection(element), mode);
  }

  private static int resolveLine(IJavaElement element, IDocument document) throws BadLocationException, JavaModelException {
    return document.getLineOfOffset(((ISourceReference)element).getNameRange().getOffset());
  }
}
