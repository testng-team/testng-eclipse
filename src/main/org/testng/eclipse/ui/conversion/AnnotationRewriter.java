package org.testng.eclipse.ui.conversion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

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
      String[] annotations = {
          "Test", "Configuration", "Factory",
      };
      for (int i = 0; i < annotations.length; i++) {
        ImportDeclaration id = ast.newImportDeclaration();
        id.setName(ast.newName("org.testng.annotations." + annotations[i]));
        lr.insertFirst(id, null);
      }
    }
    
    //
    // Add @Test annotations
    //
    List testMethods = visitor.getTestMethods();
    for (int i = 0; i < testMethods.size(); i++) {
      NormalAnnotation a = ast.newNormalAnnotation();
      a.setTypeName(ast.newName("Test"));
      addAnnotation(result, (MethodDeclaration) testMethods.get(i), a);        
    }
    
    //
    // Addd @Configuration annotations
    //
    MethodDeclaration setUp = visitor.getSetUp();
    if (null != setUp) {
      addConfiguration(ast, result, setUp, "beforeTestMethod");
    }
    
    MethodDeclaration tearDown = visitor.getTearDown();
    if (null != tearDown) {
      addConfiguration(ast, result, tearDown, "afterTestMethod");
    }

    //
    // suite
    //
    MethodDeclaration suite = visitor.getSuite();
    if (null != suite) {
      NormalAnnotation a = ast.newNormalAnnotation();
      a.setTypeName(ast.newName("Factory"));
      addAnnotation(result, suite, a);        
    }
    
    return result;
  }

  private void addConfiguration(AST ast, final ASTRewrite rewriter, 
      MethodDeclaration md, String annotation) 
  {
    NormalAnnotation a = ast.newNormalAnnotation();
    a.setTypeName(ast.newName("Configuration"));
    List l = a.values();
    MemberValuePair mvp = ast.newMemberValuePair();
    mvp.setName(ast.newSimpleName(annotation));
    BooleanLiteral sl = ast.newBooleanLiteral(true);
    mvp.setValue(sl);
    l.add(mvp);
    addAnnotation(rewriter, md, a);
  }
  
  private void addAnnotation(ASTRewrite rewriter, MethodDeclaration md, 
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
