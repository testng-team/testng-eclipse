package org.testng.eclipse.ui.conversion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

public class JUnitConverterQuickAssistProcessor
  extends BaseQuickAssistProcessor
  implements IQuickAssistProcessor
{

  /**
   * This method needs to return fast since the QuickAssist pop up is waiting on it.
   */
  public boolean hasAssists(IInvocationContext context) throws CoreException {
    IImportDeclaration[] imports = context.getCompilationUnit().getImports();
    // See if we have any JUnit import
    for (int i = 0; i < imports.length; i++) {
      IImportDeclaration id = imports[i];
      String name = id.getElementName();
      if (name.indexOf("junit") != -1) {
        return true;
      }
    }

    // Nothing in the imports, try to make a guess based on the class name and superclass
    IType[] types = context.getCompilationUnit().getTypes();
    for (IType type : types) {
      if (type.getFullyQualifiedName().contains("Test")) {
        return true;
      }
    }
//    ppp("HAS ASSISTS:" + result);
    return false;
  }

  public IJavaCompletionProposal[] getAssists(IInvocationContext context,
      IProblemLocation[] locations) 
    throws CoreException 
  {
    List<IJavaCompletionProposal> vResult = new ArrayList<>();
    
    if (hasAssists(context)) {
      //
      // Prepare the AST rewriting
      //
      ICompilationUnit cu = context.getCompilationUnit();

      // Creatie of DOM/AST from a ICompilationUnit
      CompilationUnit astRoot = createCompilationUnit(cu);
      AST ast = context.getASTRoot().getAST();

      IRewriteProvider[] providers = new IRewriteProvider[] {
          new AnnotationRewriter(),
      };

      for (int i = 0; i < providers.length; i++) {
        ASTRewrite rewriter = providers[i].createRewriter(astRoot, ast);
        vResult.add(new JUnitRewriteCorrectionProposal(
            providers[i].getName(), cu, rewriter, 1));
      }
    }
    
    return vResult.toArray(new IJavaCompletionProposal[vResult.size()]);
  }

}
