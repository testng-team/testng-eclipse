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
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A rewriter that will convert the current JUnit file to TestNG
 * using JDK5 annotations
 */
public class AnnotationRewriter 
  extends BaseRewriter 
  implements IRewriteProvider
{
  public ASTRewrite createRewriter(CompilationUnit astRoot,
      AST ast,
      JUnitVisitor visitor
      ) 
  {
    final ASTRewrite result = ASTRewrite.create(astRoot.getAST());
    
    performCommonRewrites(astRoot, ast, visitor, result);
    
    //
    // Add TestNG imports
    //
    {
      ListRewrite lr = result.getListRewrite(astRoot, CompilationUnit.IMPORTS_PROPERTY);
      if (visitor.getSetUp() != null) {
        ImportDeclaration id = ast.newImportDeclaration();
        id.setName(ast.newName("org.testng.annotations.BeforeMethod"));
        lr.insertFirst(id, null);
      }

      if (visitor.getTearDown() != null) {
        ImportDeclaration id = ast.newImportDeclaration();
        id.setName(ast.newName("org.testng.annotations.AfterMethod"));
        lr.insertFirst(id, null);
      }

      if (visitor.hasTestMethods()) {
        ImportDeclaration id = ast.newImportDeclaration();
        id.setName(ast.newName("org.testng.annotations.Test"));
        lr.insertFirst(id, null);          
      }

      if (visitor.getSuite() != null) {
        ImportDeclaration id = ast.newImportDeclaration();
        id.setName(ast.newName("org.testng.annotations.Factory"));
        lr.insertFirst(id, null);          
      }
    }
    
    for (MethodDeclaration md : visitor.getTestMethods()) {
      NormalAnnotation a = ast.newNormalAnnotation();
      a.setTypeName(ast.newName("Test"));
      addAnnotation(ast, visitor, result, md, a);
    }
    
    //
    // Addd @BeforeMethod/@AfterMethod annotations
    //
    MethodDeclaration setUp = visitor.getSetUp();
    if (null != setUp) {
      NormalAnnotation a = ast.newNormalAnnotation();
      a.setTypeName(ast.newName("BeforeMethod"));
      addAnnotation(ast, visitor, result, setUp, a);
    }
    
    MethodDeclaration tearDown = visitor.getTearDown();
    if (null != tearDown) {
      NormalAnnotation a = ast.newNormalAnnotation();
      a.setTypeName(ast.newName("AfterMethod"));
      addAnnotation(ast, visitor, result, tearDown, a);
    }

    //
    // suite
    //
    MethodDeclaration suite = visitor.getSuite();
    if (null != suite) {
      NormalAnnotation a = ast.newNormalAnnotation();
      a.setTypeName(ast.newName("Factory"));
      addAnnotation(ast, visitor, result, suite, a);        
    }

    //
    // Replace "Assert" with "AssertJUnit"
    //
    Set<Expression> asserts = visitor.getAsserts();
    for (Expression exp : asserts) {
      Name name = ast.newName("AssertJUnit");
      result.replace(exp, name, null);
    }

    //
    // Replace "fail()" with "AssertJUnit.fail()"
    //
    for (MethodInvocation fail : visitor.getFails()) {
      SimpleName exp = ast.newSimpleName("AssertJUnit");
      result.set(fail, MethodInvocation.EXPRESSION_PROPERTY, exp, null);
    }

    return result;
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
