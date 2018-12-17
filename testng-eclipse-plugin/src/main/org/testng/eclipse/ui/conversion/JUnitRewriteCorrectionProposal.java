package org.testng.eclipse.ui.conversion;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
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
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.ui.Images;

/**
 * This class implements the proposal to convert a JUnit class into a TestNG one.
 * 
 * Created on Aug 8, 2005
 * @author cbeust
 */
public class JUnitRewriteCorrectionProposal implements IJavaCompletionProposal 
{
  private ASTRewrite m_rewriter;
  private ICompilationUnit m_cu;
  private String m_name;

  public JUnitRewriteCorrectionProposal(String name, ICompilationUnit cu,
      ASTRewrite rewriter, int i) {
    m_name = name;
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
    document.setInitialLineDelimiter(getLineDelimiterUsed(cu));
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

  /* This method is based on the implementation from org.eclipse.jdt.internal.core.manipulation.StubUtility */
  private static String getLineDelimiterUsed(ICompilationUnit elem) {
    IOpenable openable= elem.getOpenable();
    if (openable instanceof ITypeRoot) {
      try {
        return openable.findRecommendedLineSeparator();
      } catch (JavaModelException exception) {
        // Use project setting
      }
    }
    IJavaProject javaProject= elem.getJavaProject();
    IProject project= null;
    if (javaProject != null) {
      project= javaProject.getProject();
    }
    String lineDelimiter= getLineDelimiterPreference(project);
    if (lineDelimiter != null) {
      return lineDelimiter;
    }
    return System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /* This method is based on the implementation from org.eclipse.jdt.internal.core.manipulation.StubUtility */
  private static String getLineDelimiterPreference(IProject project) {
    IScopeContext[] scopeContext;
    if (project != null) {
      // project preference
      scopeContext= new IScopeContext[] { new ProjectScope(project) };
      String lineDelimiter= Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, null, scopeContext);
      if (lineDelimiter != null) {
        return lineDelimiter;
      }
    }
    // workspace preference
    scopeContext= new IScopeContext[] { InstanceScope.INSTANCE };
    String platformDefault= System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
    return Platform.getPreferencesService().getString(Platform.PI_RUNTIME, Platform.PREF_LINE_SEPARATOR, platformDefault, scopeContext);
  }

  protected void addEdits(IDocument document, TextEdit editRoot) throws CoreException {
//    super.addEdits(document, editRoot);
    ASTRewrite rewrite = m_rewriter;
    if (rewrite != null) {
      try {
        TextEdit edit= rewrite.rewriteAST();
        editRoot.addChild(edit);
      } catch (IllegalArgumentException e) {
        throw new CoreException(TestNGPlugin.createError(e));
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

  /**
   * @return an HTML version of the change that's about to be made, with changes
   * highlighted in bold.
   */
  private String getHtml() {
    final StringBuffer buf= new StringBuffer();

    try {
      final TextChange change= getTextChange();

      change.setKeepPreviewEdits(true);
      final IDocument previewContent= change.getPreviewDocument(new NullProgressMonitor());
      final TextEdit rootEdit= change.getPreviewEdit(change.getEdit());

      class EditAnnotator extends TextEditVisitor {
        private int m_writtenToPos = 0;

        public void unchangedUntil(int pos) {
          if (pos > m_writtenToPos) {
            appendContent(previewContent, m_writtenToPos, pos, buf, true);
            m_writtenToPos = pos;
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
          m_writtenToPos = edit.getExclusiveEnd();
          return false;
        }
      }
      EditAnnotator ea = new EditAnnotator();
      rootEdit.accept(ea);

      // Final pre-existing region
      ea.unchangedUntil(previewContent.getLength());
    } catch (CoreException e) {
      TestNGPlugin.log(e);
    }
    return buf.toString();
  }

  private final int m_surroundLines= 1;

  private void appendContent(IDocument text, int startOffset, int endOffset, StringBuffer buf, 
      boolean surroundLinesOnly) {
    try {
      int startLine= text.getLineOfOffset(startOffset);
      int endLine= text.getLineOfOffset(endOffset);

      boolean dotsAdded= false;
      if (surroundLinesOnly && startOffset == 0) { // no surround lines for the top no-change range
        startLine= Math.max(endLine - m_surroundLines, 0);
        buf.append("...<br>"); //$NON-NLS-1$
        dotsAdded= true;
      }

      for (int i= startLine; i <= endLine; i++) {
        if (surroundLinesOnly) {
          if ((i - startLine > m_surroundLines) && (endLine - i > m_surroundLines)) {
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
        if (to == end && to != endOffset) {
          // new line when at the end of the line, and not end of range
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
    return m_name;
  }

  public Image getImage() {
    return Images.getTestNGImage();
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public int getRelevance() {
    return 0;
  }

}
