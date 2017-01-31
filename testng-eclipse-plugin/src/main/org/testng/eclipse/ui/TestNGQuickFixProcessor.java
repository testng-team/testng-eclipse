package org.testng.eclipse.ui;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.buildpath.BuildPathSupport;

public class TestNGQuickFixProcessor implements IQuickFixProcessor {

  public IJavaCompletionProposal[] getCorrections(IInvocationContext context,
                                                  IProblemLocation[] locations) throws CoreException {
    IJavaProject project = context.getCompilationUnit().getJavaProject();

    if (!isJava5SyntaxSupported(project) || isTestNGContainerOnClasspath(project)) {
      return null;
    }
    List<IJavaCompletionProposal> res = new ArrayList<>();
    for (IProblemLocation problem : locations) {
      int problemId = problem.getProblemId();
      if (isImportProblem(problemId)) {
        res = getAddTestNGToBuildPathProposals(context, problem, res);
      }
    }
    if (res.isEmpty()) {
      return null;
    }

    return (IJavaCompletionProposal[]) res.toArray(new IJavaCompletionProposal[res.size()]);
  }

  private boolean isImportProblem(int problemId) {
    return (problemId == IProblem.UndefinedType) || (problemId == IProblem.ImportNotFound);
  }

  public boolean hasCorrections(ICompilationUnit unit, int problemId) {
    return isImportProblem(problemId);
  }

  private boolean isJava5SyntaxSupported(IJavaProject project) {
    String projectComplianceLevel = getSourceCompatabilityLevel(project);

    return JavaCore.VERSION_1_5.compareTo(projectComplianceLevel) <= 0;
  }

  private boolean isTestNGContainerOnClasspath(IJavaProject project) {
    try {
      return BuildPathSupport.projectContainsClasspathEntry(project, BuildPathSupport.getTestNGClasspathEntry());
    }
    catch (JavaModelException e) {
      return false;
    }
  }

  private List<IJavaCompletionProposal> getAddTestNGToBuildPathProposals(IInvocationContext context,
                                                IProblemLocation location,
                                                List<IJavaCompletionProposal> proposals) {
    try {
      ICompilationUnit compilationUnit = context.getCompilationUnit();
      String s = compilationUnit.getBuffer().getText(location.getOffset(), location.getLength());

      if (maybeTestNGPackage(s)) { 
        proposals.add(new TestNGAddLibraryProposal(context, 11));
      }
      if (maybeTestNGAnnotation(s) && isAnnotation(context, location)) { 
        proposals.add(new TestNGAddLibraryProposal(context, 11, true));
      }
    }
    catch (JavaModelException e) {
      TestNGPlugin.log(e.getStatus());
    }

    return proposals;
  }

  private boolean maybeTestNGPackage(String s) {
    return s.indexOf("org.testng") != -1; //$NON-NLS-1$
  }

  private boolean maybeTestNGAnnotation(String s) {
    return "Test".equals(s) || "Configuration".equals(s) 
      || "BeforeSuite".equals(s) || "AfterSuite".equals(s) || "BeforeTest".equals(s) || "AfterTest".equals(s) 
      || "BeforeGroups".equals(s) || "AfterGroups".equals(s) || "BeforeClass".equals(s) || "AfterClass".equals(s) 
      || "BeforeMethod".equals(s) || "AfterMethod".equals(s); //$NON-NLS-1$
  }
  
  public static String getSourceCompatabilityLevel(IJavaProject project) {
    String complianceLevel = project.getOption(JavaCore.COMPILER_SOURCE, true);

    return complianceLevel;
  }

  private boolean isAnnotation(IInvocationContext context, IProblemLocation location) {
    ASTNode node = location.getCoveredNode(context.getASTRoot());
    return (node != null && node.getLocationInParent() == MarkerAnnotation.TYPE_NAME_PROPERTY);
  }
}
