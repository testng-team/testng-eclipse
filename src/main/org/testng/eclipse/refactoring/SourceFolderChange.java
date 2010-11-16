package org.testng.eclipse.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.testng.eclipse.ui.conversion.AnnotationRewriter;
import org.testng.eclipse.ui.conversion.JUnitVisitor;

import java.util.List;

/**
 * A composite change that applies to a source folder.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class SourceFolderChange extends CompositeChange {

  public SourceFolderChange(IClasspathEntry source, List<IType> types) {
    super(source.getPath().toOSString());
    for (IType type : types) {
      add(createChange(type));
    }
    // Unchecking all non test folders. I wish I could also collaps them in the
    // tree but I haven't found how to do this yet.
    setEnabled(source.getPath().toOSString().contains("test/"));
  }

  private Change createChange(IType type) {
    TextFileChange result = null;
    ICompilationUnit cu = type.getCompilationUnit();
    // creation of DOM/AST from a ICompilationUnit
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setSource(cu);
    CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
    JUnitVisitor visitor = new JUnitVisitor();
    astRoot.accept(visitor);

//    AST ast = context.getASTRoot().getAST();
    AST ast = astRoot.getAST();
    ASTRewrite rewriter = new AnnotationRewriter().createRewriter(astRoot, ast, visitor);
    try {
      TextEdit edit = rewriter.rewriteAST();
      result = new TextFileChange(cu.getElementName(), (IFile) cu.getResource());
      result.setEdit(edit);
    } catch (JavaModelException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    }

    return result;
  }
}