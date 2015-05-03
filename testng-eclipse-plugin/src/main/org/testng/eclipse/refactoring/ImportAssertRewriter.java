package org.testng.eclipse.refactoring;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.testng.eclipse.ui.conversion.IRewriteProvider;

public class ImportAssertRewriter implements IRewriteProvider {

  private String m_assert;

  public ImportAssertRewriter(String node) {
    m_assert = node;
  }

  public ASTRewrite createRewriter(CompilationUnit astRoot, AST ast) {
    final ASTRewrite result = ASTRewrite.create(astRoot.getAST());
    TestNGVisitor visitor = new TestNGVisitor();
    astRoot.accept(visitor);

    //
    // Add a static import for this method
    //
    ListRewrite lr = result.getListRewrite(astRoot, CompilationUnit.IMPORTS_PROPERTY);
    ImportDeclaration id = ast.newImportDeclaration();
    id.setStatic(true);
    id.setName(ast.newName("org.testng.AssertJUnit." + m_assert));
    lr.insertFirst(id, null);

    return result;
  }

  public String getName() {
    return "Add static import 'org.testng.AssertJUnit." + m_assert + "'";
  }

}
