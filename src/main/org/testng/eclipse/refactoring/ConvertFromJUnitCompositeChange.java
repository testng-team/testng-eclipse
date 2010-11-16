package org.testng.eclipse.refactoring;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.components.Filters.ITypeFilter;
import org.testng.eclipse.ui.conversion.AnnotationRewriter;
import org.testng.eclipse.ui.conversion.JUnitVisitor;
import org.testng.eclipse.util.TestSearchEngine;
import org.testng.eclipse.util.Utils;

import java.lang.reflect.InvocationTargetException;

public class ConvertFromJUnitCompositeChange extends CompositeChange {

  private IProgressMonitor m_pm;
  private IWorkbenchWindow m_window;
  private IWorkbenchPage m_page;

  public ConvertFromJUnitCompositeChange(IProgressMonitor pm,
      IWorkbenchWindow window, IWorkbenchPage page) {
    super("Composite change");
    m_pm = pm;
    m_window = window;
    m_page = page;
    computeChanges();
  }

  private void computeChanges() {
    TestNGPlugin.asyncExec(new Runnable() {
      public void run() {
        IRunnableContext context = new FindTestsRunnableContext();
        Object selection = Utils.getSelectedProjectOrPackage(m_page);
        IJavaProject project = (IJavaProject)
            (selection instanceof IJavaProject ? selection : null);
        IPackageFragmentRoot pfr = (IPackageFragmentRoot)
            (selection instanceof IPackageFragmentRoot ? selection : null);
        try {
          ITypeFilter filter = new ITypeFilter() {

            public boolean accept(IType type) {
              IResource obj = (IResource) type.getAdapter(IResource.class);
              IContainer container = null;
              if (obj instanceof IContainer) {
                container = (IContainer) obj;
              } else if (obj != null) {
                container = ((IResource) obj).getParent();
              }
              if (container != null) {
                String sourcePath = container.getFullPath().toString();
                return sourcePath.contains("/test/");
//                return type.getFullyQualifiedName().contains("JUnit3Test2");
              } else {
                return false;
              }
            }
            
          };
          IType[] types = TestSearchEngine.findTests(context, new Object[] { project }, filter);
          for (IType type : types) {
            
            add(createChange(type));
            System.out.println("  type:" + type.getFullyQualifiedName());
          }
        } catch (InvocationTargetException e) {
          e.printStackTrace();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        
      }
    });
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
    System.out.println("Finished rewriting " + getName());

    return result;
  }
  @Override
  public String getName() {
    return "src/test/java";
  }

  @Override
  public void initializeValidationData(IProgressMonitor pm) {
  }

  @Override
  public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException,
      OperationCanceledException {
    return null;
  }

  @Override
  public Object getModifiedElement() {
    return null;
  }

}
