package org.testng.eclipse.ui.conversion;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import java.util.List;
import java.util.Map;
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
    maybeAddImport(ast, result, astRoot, !visitor.getBeforeMethods().isEmpty(),
        "org.testng.annotations.BeforeMethod");
    maybeAddImport(ast, result, astRoot, visitor.hasTestMethods(), "org.testng.annotations.Test");
    maybeAddImport(ast, result, astRoot, !visitor.getAfterMethods().isEmpty(),
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
    maybeAddAnnotations(ast, visitor, result, visitor.getTestMethods(), "Test", null);
    maybeAddAnnotations(ast, visitor, result, visitor.getBeforeMethods(), "BeforeMethod",
        "@Before" /* annotation to remove */);
    maybeAddAnnotations(ast, visitor, result, visitor.getAfterMethods(), "AfterMethod",
        "@After" /* annotation to remove */);
    maybeAddAnnotation(ast, visitor, result, visitor.getSuite(), "Factory", null);

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

    //
    // Replace @Test(expected) with @Test(expectedExceptions)
    //
    for (MemberValuePair mvp : visitor.getTestsWithExpected()) {
      result.replace(mvp.getName(), ast.newSimpleName("expectedExceptions"), null);
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
      MethodDeclaration method, String annotation, String annotationToRemove)
  {
    if (method != null) {
      addAnnotation(ast, visitor, rewriter, method, createAnnotation(ast, annotation),
          annotationToRemove);
    }
  }

  private NormalAnnotation createAnnotation(AST ast, String name) {
    NormalAnnotation result = ast.newNormalAnnotation();
    result.setTypeName(ast.newName(name));
    return result;
  }
  /**
   * Add the given annotation if the method is non null
   */
  private void maybeAddAnnotations(AST ast, JUnitVisitor visitor, ASTRewrite rewriter,
      List<MethodDeclaration> methods, String annotation, String annotationToRemove)
  {
    for (MethodDeclaration method : methods) {
      maybeAddAnnotation(ast, visitor, rewriter, method, annotation, annotationToRemove);
    }
  }

  private void addAnnotation(AST ast, JUnitVisitor visitor, ASTRewrite rewriter, MethodDeclaration md, 
      NormalAnnotation a, String annotationToRemove)
  {
    ListRewrite lr = rewriter.getListRewrite(md, MethodDeclaration.MODIFIERS2_PROPERTY);

    // Remove the annotation if applicable
    if (annotationToRemove != null) {
      List modifiers = md.modifiers();
      for (int k = 0; k < modifiers.size(); k++) {
        Object old = modifiers.get(k);
        if (old instanceof Annotation && old.toString().equals(annotationToRemove)) {
          lr.remove((Annotation) old, null);
          break;
        }
      }
    }

    // Add the annotation
    lr.insertFirst(a, null);
  }

  public String getName() {
    return "Convert to TestNG (Annotations)";
  }  
}
