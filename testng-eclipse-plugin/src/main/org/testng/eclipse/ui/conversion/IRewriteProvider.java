package org.testng.eclipse.ui.conversion;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

/**
 * Interface implemented by all the rewriters
 * 
 * Created on Aug 8, 2005
 * @author cbeust
 */
public interface IRewriteProvider {
  
  public ASTRewrite createRewriter(CompilationUnit astRoot, AST ast);
  
  public String getName();

}
