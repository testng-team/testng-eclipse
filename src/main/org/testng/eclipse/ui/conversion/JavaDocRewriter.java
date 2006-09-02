package org.testng.eclipse.ui.conversion;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

/**
 * A rewriter that will convert the current JUnit file to TestNG
 * using JavaDoc annotations
 */
public class JavaDocRewriter 
  extends BaseRewriter
  implements IRewriteProvider
{

  public ASTRewrite createRewriter(CompilationUnit astRoot, AST ast,
      JUnitVisitor visitor) 
  {
    final ASTRewrite result = ASTRewrite.create(astRoot.getAST());
    
    performCommonRewrites(astRoot, ast, visitor, result);
    
    //
    // Add @Test annotations
    //
    List testMethods = visitor.getTestMethods();
    for (int i = 0; i < testMethods.size(); i++) {
      MethodDeclaration md = (MethodDeclaration) testMethods.get(i);
      addJavaDocAnnotation(astRoot, ast, result, md, "@testng.test");
    }
    
    //
    // Addd @Configuration annotations
    //
    MethodDeclaration setUp = visitor.getSetUp();
    if (null != setUp) {
      addJavaDocAnnotation(astRoot, ast, result, setUp,
          "@testng.configuration beforeTestMethod = \"true\"");
    }
    
    MethodDeclaration tearDown = visitor.getTearDown();
    if (null != tearDown) {
      addJavaDocAnnotation(astRoot, ast, result, tearDown,
          "@testng.configuration afterTestMethod = \"true\"");
    }
    
    //
    // suite
    //
    MethodDeclaration suite = visitor.getSuite();
    if (null != suite) {
      addJavaDocAnnotation(astRoot, ast, result, suite, "@testng.factory");
    }
    
    return result;
  }
  
  private void addJavaDocAnnotation(CompilationUnit astRoot, AST ast, 
      final ASTRewrite result, MethodDeclaration md, String annotation)
  {
    Javadoc javaDoc = md.getJavadoc(); 
    if (null == javaDoc) {
      javaDoc = astRoot.getAST().newJavadoc();
      result.set(md, MethodDeclaration.JAVADOC_PROPERTY, javaDoc, null);
    }
    ListRewrite lr = result.getListRewrite(javaDoc, Javadoc.TAGS_PROPERTY);
    TagElement te = ast.newTagElement();
    te.setTagName(annotation);
    lr.insertFirst(te, null);
  }  

  public String getName() {
    return "Convert to TestNG (JavaDoc)";
  }    
}
