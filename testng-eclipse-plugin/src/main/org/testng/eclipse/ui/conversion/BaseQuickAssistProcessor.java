package org.testng.eclipse.ui.conversion;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 * The base class for our AST visitors.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class BaseQuickAssistProcessor {

  public static CompilationUnit createCompilationUnit(ICompilationUnit cu) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setSource(cu);
    parser.setResolveBindings(true);

    return (CompilationUnit) parser.createAST(null);
  }

}
