package org.testng.eclipse.refactoring;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.testng.eclipse.ui.conversion.IRewriteProvider;

/**
 * A rewriter that pulls all the @Test annotations at the class level.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class PullTestRewriter implements IRewriteProvider {

  public ASTRewrite createRewriter(CompilationUnit astRoot, AST ast) {
    final ASTRewrite result = ASTRewrite.create(astRoot.getAST());
    TestNGVisitor visitor = new TestNGVisitor();
    astRoot.accept(visitor);

    //
    // Remove all the @Test annotations
    //
    for (Annotation a: visitor.getTestMethods().values()) {
      result.remove(a, null);
    }

    //
    // Add @Test at the class level
    //
    MarkerAnnotation test = ast.newMarkerAnnotation();
    test.setTypeName(ast.newName("Test"));
    ListRewrite lr = result.getListRewrite(visitor.getType(), TypeDeclaration.MODIFIERS2_PROPERTY);
    lr.insertFirst(test, null);

    return result;
  }

  public String getName() {
    return "Pull @Test annotations to the class level";
  }

}
