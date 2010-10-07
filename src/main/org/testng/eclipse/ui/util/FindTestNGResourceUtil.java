package org.testng.eclipse.ui.util;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.ResolvedSourceMethod;
import org.testng.eclipse.launch.components.ITestContent;

public class FindTestNGResourceUtil {

  public static Set<IType> findClasses(IJavaElement[] javaElements, IProgressMonitor progressMonitor) {
    SearchPattern searchPattern = SearchPattern.createPattern("Test",
        IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.CLASS,
        SearchPattern.R_CAMELCASE_MATCH);
    TestClassesSearchRequestor testClassesSearchRequestor = new TestClassesSearchRequestor();
    IJavaSearchScope javaSearchScope = SearchEngine.createJavaSearchScope(javaElements, false);
    SearchEngine searchEngine = new SearchEngine();
    try {
      searchEngine.search(searchPattern,
          new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, javaSearchScope,
          testClassesSearchRequestor, progressMonitor);
    } catch (CoreException e) {
      return null;
    }
    return testClassesSearchRequestor.getResultList();
  }

  public static Set<String> findPackages(IJavaProject javaProject, IProgressMonitor progressMonitor) {
    SearchPattern packageSearchPattern = SearchPattern.createPattern("*",
        IJavaSearchConstants.PACKAGE, IJavaSearchConstants.ALL_OCCURRENCES,
        SearchPattern.R_REGEXP_MATCH);
    IJavaSearchScope javaSearchScope;
    try {
      javaSearchScope = SearchEngine.createJavaSearchScope(javaProject.getChildren(), false);
    } catch (JavaModelException e1) {
      e1.printStackTrace();
      return new HashSet<String>();
    }
    PackageSearchRequestor packageSearchRequestor = new PackageSearchRequestor();
    // finding all the packages is done in PackageSearchRequestor and we need to
    // pass
    // progressMonitor to the packageSearchRequester so that it quits the search
    // loop if pm is cancelled
    packageSearchRequestor.setProgressMonitor(progressMonitor);
    SearchEngine searchEngine = new SearchEngine();
    SearchParticipant[] searchParticipants = new SearchParticipant[] { SearchEngine
        .getDefaultSearchParticipant() };

    try {
      searchEngine.search(packageSearchPattern, searchParticipants, javaSearchScope,
          packageSearchRequestor, progressMonitor);
    } catch (CoreException e) {
      return null;
    }
    return packageSearchRequestor.getResultList();
  }

  public static Set<IType> findMethods(IJavaElement[] javaElements, IProgressMonitor progressMonitor) {
    SearchPattern searchPattern = SearchPattern.createPattern("Test",
        IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.CLASS,
        SearchPattern.R_CAMELCASE_MATCH);
    TestClassesSearchRequestor testClassesSearchRequestor = new TestClassesSearchRequestor();
    IJavaSearchScope javaSearchScope = SearchEngine.createJavaSearchScope(javaElements, false);
    SearchEngine searchEngine = new SearchEngine();
    try {
      searchEngine.search(searchPattern,
          new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, javaSearchScope,
          testClassesSearchRequestor, progressMonitor);
    } catch (CoreException e) {
      return null;
    }
    return testClassesSearchRequestor.getResultList();
  }

  static class TestClassesSearchRequestor extends SearchRequestor {

    private Set<IType> resultList = new HashSet<IType>();

    public Set<IType> getResultList() {
      return resultList;
    }

    @Override
    public void beginReporting() {
      log("search process started - finding classes with test methods");
    }

    @Override
    public void endReporting() {
      log("search process finished after finding " + resultList.size() + " matches");
    }

    private void log(String string) {
      System.out.println("[FindClassUtil] : " + string);
    }

    public void acceptSearchMatch(SearchMatch match) throws CoreException {
      Object obj = match.getElement();
      if (obj instanceof ResolvedSourceMethod) {
        ResolvedSourceMethod resolvedSourceMethod = (ResolvedSourceMethod) obj;
        IJavaElement iJavaElement = resolvedSourceMethod.getParent();
        ITestContent content = TypeParser.parseType(resolvedSourceMethod.getDeclaringType());
        if (content.hasTestMethods()) {
          if (content.isTestMethod(resolvedSourceMethod)) {
            if (!(iJavaElement instanceof IType)) {
              log("errror");
            } else {
              this.resultList.add((IType) iJavaElement);
            }
          }
        }
      }
    }
  }

  static class TestMethodsSearchRequestor extends SearchRequestor {

    private Set<IJavaElement> resultList = new HashSet<IJavaElement>();

    public Set<IJavaElement> getResultList() {
      return resultList;
    }

    @Override
    public void beginReporting() {
      log("search process started - finding test methods");
    }

    @Override
    public void endReporting() {
      log("search process finished after finding " + resultList.size() + " matches");
    }

    private void log(String string) {
      System.out.println("[FindClassUtil] : " + string);
    }

    public void acceptSearchMatch(SearchMatch match) throws CoreException {
      Object obj = match.getElement();
      if (obj instanceof ResolvedSourceMethod) {
        ResolvedSourceMethod resolvedSourceMethod = (ResolvedSourceMethod) obj;
        ITestContent content = TypeParser.parseType(resolvedSourceMethod.getDeclaringType());
        if (content.isTestMethod(resolvedSourceMethod)) {
          this.resultList.add(resolvedSourceMethod);
        }
      }
    }
  }

  static class PackageSearchRequestor extends SearchRequestor {

    private IProgressMonitor progressMonitor;
    private Set<String> resultList = new HashSet<String>();

    public void setProgressMonitor(IProgressMonitor progressMonitor) {
      this.progressMonitor = progressMonitor;
    }

    public Set<String> getResultList() {
      return resultList;
    }

    @Override
    public void beginReporting() {
      log("search process started - finding packages");
    }

    @Override
    public void endReporting() {
      log("search process finished after finding " + resultList.size() + " matches");
    }

    private void log(String string) {
      System.out.println("[FindClassUtil] : " + string);
    }

    public void acceptSearchMatch(SearchMatch match) throws CoreException {
      Object obj = match.getElement();
      if (obj instanceof PackageFragment) {
        PackageFragment packageFragment = (PackageFragment) obj;
        if (packageFragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
          this.resultList.add(packageFragment.getElementName());
          ICompilationUnit[] compilationUnits = packageFragment.getCompilationUnits();
          boolean foundObeTestMethodInPackage = false;
          if (compilationUnits != null) {
            for (int i = 0; i < compilationUnits.length; i++) {
              if (this.progressMonitor != null && this.progressMonitor.isCanceled()) {
                return;
              }
              // check if pm is cancelled , in that case just quit with whatever
              // already found
              IType[] types = compilationUnits[i].getTypes();
              if (types != null) {
                for (int j = 0; j < types.length; j++) {
                  if (types[j] != null) {
                    ITestContent content = TypeParser.parseType(types[j]);
                    if (content.hasTestMethods()) {
                      this.resultList.add(packageFragment.getElementName());
                      break;
                    }
                  }

                }
                if (foundObeTestMethodInPackage) {
                  break;
                }
              }

            }
          }
        }
      }
    }
  }
}
