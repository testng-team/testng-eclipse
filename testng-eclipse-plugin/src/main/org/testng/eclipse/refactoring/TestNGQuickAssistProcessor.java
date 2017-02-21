package org.testng.eclipse.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.testng.eclipse.ui.conversion.BaseQuickAssistProcessor;
import org.testng.eclipse.ui.conversion.IRewriteProvider;
import org.testng.eclipse.ui.conversion.JUnitRewriteCorrectionProposal;

/**
 * The assist processor that presents all the TestNG related code assists.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class TestNGQuickAssistProcessor
  extends BaseQuickAssistProcessor
  implements IQuickAssistProcessor
{
  private TestNGVisitor m_visitor;
  private CompilationUnit m_astRoot;
  private AST m_ast;
  private ICompilationUnit m_compilationUnit;

  public boolean hasAssists(IInvocationContext context) throws CoreException {
    init(context);

    return hasPushAssists(m_visitor) || hasPullAssists(m_visitor)
        || hasAssertImportAssists(m_visitor);
  }

  private static boolean hasAssertImportAssists(TestNGVisitor visitor) {
    return visitor.getAsserts().size() > 0;
  }

  /**
   * Initialize the AST. This method needs to be called every time, otherwise we
   * might store a tree that then gets obsoleted by changes in the editor.
   */
  private void init(IInvocationContext context) {
    //
    // Prepare the AST for rewriting
    //
    m_compilationUnit = context.getCompilationUnit();

    // Create a DOM/AST from a ICompilationUnit
    m_astRoot = createCompilationUnit(m_compilationUnit);
    m_ast = context.getASTRoot().getAST();

    // Populate the TestNGVisitor with the information we'll be needing
    // to do the rewriting
    m_visitor = new TestNGVisitor();
    m_astRoot.accept(m_visitor);
  }

  private static boolean hasPushAssists(TestNGVisitor visitor) {
    return visitor.getTestClassAnnotation() != null;
  }

  private static boolean hasPullAssists(TestNGVisitor visitor) {
    Annotation testClass = visitor.getTestClassAnnotation();
    return visitor.getTestMethods().size() > 0 && testClass == null;
  }

  public IJavaCompletionProposal[] getAssists(IInvocationContext context,
      IProblemLocation[] locations) throws CoreException
  {
    List<IJavaCompletionProposal> vResult = new ArrayList<>();
    init(context);
    if (hasAssists(context)) {

      //
      // Only show applicable TestNG refactorings
      //
      List<IRewriteProvider> providers = new ArrayList<>();
      if (hasPushAssists(m_visitor)) providers.add(new PushTestRewriter());
      if (hasPullAssists(m_visitor)) providers.add(new PullTestRewriter());
      if (hasAssertImportAssists(m_visitor)) {
        String node = findAssertInContext(context);
        if (node != null) providers.add(new ImportAssertRewriter(node));
      }

      for (IRewriteProvider provider : providers) {
        ASTRewrite rewriter = provider.createRewriter(m_astRoot, m_ast);
        vResult.add(new JUnitRewriteCorrectionProposal(
            provider.getName(), m_compilationUnit, rewriter, 1));
      }
    }
    
    return vResult.toArray(new IJavaCompletionProposal[vResult.size()]);
    }

  private String findAssertInContext(IInvocationContext context) {
    ASTNode node = context.getCoveringNode();
    while (node != null) {
      if (node instanceof MethodInvocation) {
        String nodeName = ((MethodInvocation) node).getName().toString();
        if (m_visitor.getAsserts().contains(nodeName)) {
          return nodeName;
        }
      }

      node = node.getParent();
    }

    return null;
  }
}
