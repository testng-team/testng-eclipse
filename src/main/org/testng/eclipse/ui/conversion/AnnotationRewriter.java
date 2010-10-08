package org.testng.eclipse.ui.conversion;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A rewriter that will convert the current JUnit file to TestNG
 * using JDK5 annotations
 *
 * @author CŽdric Beust <cedric@beust.com>
 */
public class AnnotationRewriter implements IRewriteProvider
{
  public ASTRewrite createRewriter(CompilationUnit astRoot,
      AST ast,
      JUnitVisitor visitor
      ) 
  {
    final ASTRewrite result = ASTRewrite.create(astRoot.getAST());
    
    //
    // Remove all the JUnit imports
    //
    List<ImportDeclaration> oldImports = visitor.getJUnitImports();
    for (int i = 0; i < oldImports.size(); i++) {
      result.remove((ImportDeclaration) oldImports.get(i), null);
    }
    
    //
    // Add imports as needed
    //
    maybeAddImport(ast, result, astRoot, visitor.hasAsserts(), "org.testng.AssertJUnit");
    maybeAddImport(ast, result, astRoot, visitor.hasFail(), "org.testng.Assert");
    maybeAddImport(ast, result, astRoot, visitor.getSetUp() != null,
        "org.testng.annotations.BeforeMethod");
    maybeAddImport(ast, result, astRoot, visitor.hasTestMethods(), "org.testng.annotations.Test");
    maybeAddImport(ast, result, astRoot, visitor.getTearDown() != null,
        "org.testng.annotations.AfterMethod");
    maybeAddImport(ast, result, astRoot, visitor.getSuite() != null,
        "org.testng.annotations.Factory");

    //
    // Remove "extends TestCase"
    //
    SimpleType td = visitor.getTestCase();
    if (null != td) {
      result.remove(td, null);
    }

    //
    // Addd the annotations as needed
    //
    maybeAddAnnotations(ast, visitor, result, visitor.getTestMethods(), "Test");
    maybeAddAnnotation(ast, visitor, result, visitor.getSetUp(), "BeforeMethod");
    maybeAddAnnotation(ast, visitor, result, visitor.getTearDown(), "AfterMethod");
    maybeAddAnnotation(ast, visitor, result, visitor.getSuite(), "Factory");

    //
    // Replace "Assert" with "AssertJUnit"
    //
    Set<MethodInvocation> asserts = visitor.getAsserts();
    for (MethodInvocation m : asserts) {
      Expression exp = m.getExpression();
      Name name = ast.newName("AssertJUnit");
      if (exp != null) {
        result.replace(exp, name, null);
      } else {
        result.set(m, MethodInvocation.EXPRESSION_PROPERTY, name, null);
      }
    }

    //
    // Replace "fail()" with "Assert.fail()"
    //
    for (MethodInvocation fail : visitor.getFails()) {
      SimpleName exp = ast.newSimpleName("Assert");
      result.set(fail, MethodInvocation.EXPRESSION_PROPERTY, exp, null);
    }

    return result;
  }

  private void maybeAddImport(AST ast, ASTRewrite rewriter, CompilationUnit astRoot, boolean add,
      String imp) {
    if (add) {
      addImport(ast, rewriter, astRoot, imp);
    }
  }
  private void addImport(AST ast, ASTRewrite rewriter, CompilationUnit astRoot, String imp) {
    ListRewrite lr = rewriter.getListRewrite(astRoot, CompilationUnit.IMPORTS_PROPERTY);
    ImportDeclaration id = ast.newImportDeclaration();
    id.setName(ast.newName(imp));
    lr.insertFirst(id, null);
  }

  /**
   * Add the given annotation if the method is non null
   */
  private void maybeAddAnnotation(AST ast, JUnitVisitor visitor, ASTRewrite rewriter,
      MethodDeclaration method, String annotation)
  {
    if (null != method) {
      NormalAnnotation a = ast.newNormalAnnotation();
      a.setTypeName(ast.newName(annotation));
      addAnnotation(ast, visitor, rewriter, method, a);
    }
  }

  /**
   * Add the given annotation if the method is non null
   */
  private void maybeAddAnnotations(AST ast, JUnitVisitor visitor, ASTRewrite rewriter,
      List<MethodDeclaration> methods, String annotation)
  {
    for (MethodDeclaration method : methods) {
      maybeAddAnnotation(ast, visitor, rewriter, method, annotation);
    }
  }

  private void addAnnotation(AST ast, JUnitVisitor visitor, ASTRewrite rewriter, MethodDeclaration md, 
      NormalAnnotation a) 
  {
    List oldModifiers = md.modifiers();
    List newModifiers = new ArrayList();
    for (int k = 0; k < oldModifiers.size(); k++) {
      newModifiers.add(oldModifiers.get(k));
    }
    ListRewrite lr = rewriter.getListRewrite(md, MethodDeclaration.MODIFIERS2_PROPERTY);
    lr.insertFirst(a, null);
  }

  public String getName() {
    return "Convert to TestNG (Annotations)";
  }  
}
