package org.testng.eclipse.ui.conversion;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIStatus;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.CopyTargetEdit;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditVisitor;
import org.eclipse.text.edits.UndoEdit;

/**
 * 
 * This class should implement the interface directly instead of extending
 * an internal class (ASTRewriteCorrectionProposal). Need to fix that.
 * 
 * Created on Aug 8, 2005
 * @author cbeust
 */
public class JUnitRewriteCorrectionProposal implements IJavaCompletionProposal 
{
  private ASTRewrite m_rewriter;
  private ICompilationUnit m_cu;

  public JUnitRewriteCorrectionProposal(String name, ICompilationUnit cu,
      ASTRewrite rewriter, int i)
  {
    m_rewriter = rewriter;
    m_cu = cu;
  }

  public void apply(IDocument document) {
    TextEdit edits = m_rewriter.rewriteAST(document, null);
    UndoEdit undo = null;
    try {
        undo = edits.apply(document);
    } catch(MalformedTreeException e) {
        e.printStackTrace();
    } catch(BadLocationException e) {
        e.printStackTrace();
    }

  }

  public Point getSelection(IDocument document) {
    // TODO Auto-generated method stub
    return null;
  }

  public String getAdditionalProposalInfo() {
    return getHtml();
  }

  protected TextChange createTextChange() throws CoreException {
    ICompilationUnit cu = m_cu;
    String name = "TestNG";
    TextChange change = null;
//    if (!cu.getResource().exists()) {
//      String source;
//      try {
//        source= cu.getSource();
//      } catch (JavaModelException e) {
//        JavaPlugin.log(e);
//        source= new String(); // empty
//      }
//    }
    Document document= new Document(cu.getSource());
    document.setInitialLineDelimiter(StubUtility.getLineDelimiterUsed(cu));
    change= new DocumentChange(name, document);
//    } else {
//      Document doc = new Document(m_cu.getSource());
//      CompilationUnitChange cuChange = new CompilationUnitChange(name, cu);
//      cuChange.setSaveMode(TextFileChange.LEAVE_DIRTY);
//      change= cuChange;
//    }
    TextEdit rootEdit= new MultiTextEdit();
    change.setEdit(rootEdit);

    // initialize text change
//    IDocument document= change.getCurrentDocument(new NullProgressMonitor());
    addEdits(change.getCurrentDocument(new NullProgressMonitor()), rootEdit);
    return change;
  }

  protected void addEdits(IDocument document, TextEdit editRoot) throws CoreException {
//    super.addEdits(document, editRoot);
    ASTRewrite rewrite = m_rewriter;
    if (rewrite != null) {
      try {
        TextEdit edit= rewrite.rewriteAST();
        editRoot.addChild(edit);
      } catch (IllegalArgumentException e) {
        throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
      }
    }
//    if (fImportRewrite != null) {
//      editRoot.addChild(fImportRewrite.rewriteImports(new NullProgressMonitor()));
//    }
  }

  private TextChange getTextChange() {
    try {
      return createTextChange();
    } catch (CoreException e) {
      e.printStackTrace();
    }
    return null;
  }

  private String getHtml() {
    final StringBuffer buf= new StringBuffer();

    try {
      final TextChange change= getTextChange();

      change.setKeepPreviewEdits(true);
      final IDocument previewContent= change.getPreviewDocument(new NullProgressMonitor());
      final TextEdit rootEdit= change.getPreviewEdit(change.getEdit());

      class EditAnnotator extends TextEditVisitor {
        private int fWrittenToPos = 0;

        public void unchangedUntil(int pos) {
          if (pos > fWrittenToPos) {
            appendContent(previewContent, fWrittenToPos, pos, buf, true);
            fWrittenToPos = pos;
          }
        }

        public boolean visit(MoveTargetEdit edit) {
          return true; //rangeAdded(edit);
        }

        public boolean visit(CopyTargetEdit edit) {
          return true; //return rangeAdded(edit);
        }

        public boolean visit(InsertEdit edit) {
          return rangeAdded(edit);
        }

        public boolean visit(ReplaceEdit edit) {
          if (edit.getLength() > 0)
            return rangeAdded(edit);
          return rangeRemoved(edit);
        }

        public boolean visit(MoveSourceEdit edit) {
          return rangeRemoved(edit);
        }

        public boolean visit(DeleteEdit edit) {
          return rangeRemoved(edit);
        }

        private boolean rangeRemoved(TextEdit edit) {
          unchangedUntil(edit.getOffset());
          return false;
        }

        private boolean rangeAdded(TextEdit edit) {
          unchangedUntil(edit.getOffset());
          buf.append("<b>"); //$NON-NLS-1$
          appendContent(previewContent, edit.getOffset(), edit.getExclusiveEnd(), buf, false);
          buf.append("</b>"); //$NON-NLS-1$
          fWrittenToPos = edit.getExclusiveEnd();
          return false;
        }
      }
      EditAnnotator ea = new EditAnnotator();
      rootEdit.accept(ea);

      // Final pre-existing region
      ea.unchangedUntil(previewContent.getLength());
    } catch (CoreException e) {
      JavaPlugin.log(e);
    }
    return buf.toString();
  }

  private final int surroundLines= 1;

  private void appendContent(IDocument text, int startOffset, int endOffset, StringBuffer buf, 
      boolean surroundLinesOnly) {
    try {
      int startLine= text.getLineOfOffset(startOffset);
      int endLine= text.getLineOfOffset(endOffset);

      boolean dotsAdded= false;
      if (surroundLinesOnly && startOffset == 0) { // no surround lines for the top no-change range
        startLine= Math.max(endLine - surroundLines, 0);
        buf.append("...<br>"); //$NON-NLS-1$
        dotsAdded= true;
      }

      for (int i= startLine; i <= endLine; i++) {
        if (surroundLinesOnly) {
          if ((i - startLine > surroundLines) && (endLine - i > surroundLines)) {
            if (!dotsAdded) {
              buf.append("...<br>"); //$NON-NLS-1$
              dotsAdded= true;
            } else if (endOffset == text.getLength()) {
              return; // no surround lines for the bottom no-change range
            }
            continue;
          }
        }

        IRegion lineInfo= text.getLineInformation(i);
        int start= lineInfo.getOffset();
        int end= start + lineInfo.getLength();

        int from= Math.max(start, startOffset);
        int to= Math.min(end, endOffset);
        String content= text.get(from, to - from);
        if (surroundLinesOnly && (from == start) && containsOnlyWhitespaces(content)) {
          continue; // ignore empty lines except when range started in the middle of a line
        }
        for (int k= 0; k < content.length(); k++) {
          char ch= content.charAt(k);
          if (ch == '<') {
            buf.append("&lt;"); //$NON-NLS-1$
          } else if (ch == '>') {
            buf.append("&gt;"); //$NON-NLS-1$
          } else {
            buf.append(ch);
          }
        }
        if (to == end && to != endOffset) { // new line when at the end of the line, and not end of range
          buf.append("<br>"); //$NON-NLS-1$
        }
      }
    } catch (BadLocationException e) {
      // ignore
    }
  }

  private static boolean containsOnlyWhitespaces(String s) {
    int size= s.length();
    for (int i= 0; i < size; i++) {
      if (!Character.isWhitespace(s.charAt(i)))
        return false;
    }
    return true;
  }

  public String getDisplayString() {
    return "Convert to TestNG";
  }

  public Image getImage() {
    // TODO Auto-generated method stub
    return null;
  }

  public IContextInformation getContextInformation() {
    // TODO Auto-generated method stub
    return null;
  }

  public int getRelevance() {
    // TODO Auto-generated method stub
    return 0;
  }
//  public JUnitRewriteCorrectionProposal(String name, ICompilationUnit cu, ASTRewrite rewrite, int relevance) {
//    super(name, cu, rewrite, relevance, TestNGMainTab.getTestNGImage());
//    // TODO Auto-generated constructor stub
//  }

}
