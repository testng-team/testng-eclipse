package org.testng.eclipse.ui.conversion;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;
import org.testng.eclipse.collections.Lists;

import java.util.List;

public class JUnitConverterQuickAssistProcessor implements
    IQuickAssistProcessor {

  public boolean hasAssists(IInvocationContext context) throws CoreException {
    boolean result = false;
    
    IImportDeclaration[] imports = context.getCompilationUnit().getImports();
    for (int i = 0; i < imports.length; i++) {
      IImportDeclaration id = imports[i];
      String name = id.getElementName();
      if (name.indexOf("junit") != -1) {
        result = true;
        break;
      }
    }
    
//    ppp("HAS ASSISTS:" + result);
    return result;
  }

  public IJavaCompletionProposal[] getAssists(IInvocationContext context,
      IProblemLocation[] locations) 
    throws CoreException 
  {
    List<IJavaCompletionProposal> vResult = Lists.newArrayList();
    
    if (hasAssists(context)) {
      //
      // Prepare the AST rewriting
      //
      ICompilationUnit cu = context.getCompilationUnit();
      // creation of DOM/AST from a ICompilationUnit
      ASTParser parser = ASTParser.newParser(AST.JLS3);
      parser.setSource(cu);
      CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
      AST ast = context.getASTRoot().getAST();
      JUnitVisitor visitor = new JUnitVisitor();
      astRoot.accept(visitor);
      
      IRewriteProvider[] providers = new IRewriteProvider[] {
          new AnnotationRewriter(),
//          new JavaDocRewriter(),
      };

      for (int i = 0; i < providers.length; i++) {
        ASTRewrite rewriter = providers[i].createRewriter(astRoot, ast, visitor);
        vResult.add(new JUnitRewriteCorrectionProposal(
            providers[i].getName(), cu, rewriter, 1));
      }
    }
    
    return (IJavaCompletionProposal[])
      vResult.toArray(new IJavaCompletionProposal[vResult.size()]);
  }

  private static void ppp(String s) {
    System.out.println("[JUnitConverterQuickAssistProcessor] " + s);
  }

}
