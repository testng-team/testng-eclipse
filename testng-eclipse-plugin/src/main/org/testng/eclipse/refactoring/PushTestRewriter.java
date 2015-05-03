package org.testng.eclipse.refactoring;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.testng.eclipse.ui.conversion.IRewriteProvider;

/**
 * A rewriter that removes the @Test annotation on the class and moves it
 * to all the public methods.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class PushTestRewriter implements IRewriteProvider {

  public ASTRewrite createRewriter(CompilationUnit astRoot, AST ast) {
    final ASTRewrite result = ASTRewrite.create(astRoot.getAST());
    TestNGVisitor visitor = new TestNGVisitor();
    astRoot.accept(visitor);

    //
    // Remove the class @Test annotation
    //
    result.remove(visitor.getTestClassAnnotation(), null);

    //
    // Add a @Test annotation on all the public methods that don't already
    // have a TestNG annotation.
    //
    for (MethodDeclaration md : visitor.getPublicMethods()) {
      ListRewrite lr = result.getListRewrite(md, MethodDeclaration.MODIFIERS2_PROPERTY);
      MarkerAnnotation test = ast.newMarkerAnnotation();
      test.setTypeName(ast.newSimpleName("Test"));
      lr.insertFirst(test, null);
    }

    return result;
  }

  public String getName() {
    return "Move the @Test class annotation into the class";
  }

}
