package org.testng.eclipse.ui.conversion;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

/**
 * Base class for rewriters
 * 
 * Created on Aug 8, 2005
 * @author cbeust
 */
public class BaseRewriter {
  /**
   * Rewrites that need to be done for both JDK 1.4 and JDK5:
   * - Remove the JUnit imports
   * - Remove "extends TestCase"
   */
  public void performCommonRewrites(CompilationUnit astRoot,
      AST ast,
      JUnitVisitor visitor, final ASTRewrite result)
  {
    //
    // Remove all the JUnit imports
    //
    List oldImports = visitor.getJUnitImports();
    for (int i = 0; i < oldImports.size(); i++) {
      result.remove((ImportDeclaration) oldImports.get(i), null);
    }
    
    //
    // Add import for AssertJUnit
    //
    if (visitor.hasAsserts()) {
      ListRewrite lr = result.getListRewrite(astRoot, CompilationUnit.IMPORTS_PROPERTY);
      ImportDeclaration id = ast.newImportDeclaration();
      id.setName(ast.newName("org.testng.AssertJUnit"));
      lr.insertFirst(id, null);
    }
    
    //
    // Remove "extends TestCase"
    //
    {
      SimpleType td = visitor.getTestCase();
      if (null != td) {
        result.remove(td, null);
      }
    }
  }
}
