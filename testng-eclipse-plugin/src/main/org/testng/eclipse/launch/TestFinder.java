package org.testng.eclipse.launch;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.junit.util.CoreTestSearchEngine;
import org.testng.eclipse.util.ResourceUtil;

public class TestFinder {

  public static Set<IType> findTests(IJavaElement element, IProgressMonitor pm) throws CoreException {
    TestReferenceCollector requestor = new TestReferenceCollector();
    findTests(element, requestor, pm);
    return requestor.getResult();
  }

  public static void findTests(IJavaElement element, SearchRequestor requestor, IProgressMonitor pm) throws CoreException {
    if (element == null) {
      throw new IllegalArgumentException();
    }

    if (pm == null) {
      pm= new NullProgressMonitor();
    }

    try {
      pm.beginTask(ResourceUtil.getString("TestSearchEngine.message.searching"), 4);
      IJavaElement[] elements = new IJavaElement[] {element};
      IJavaSearchScope scope= SearchEngine.createJavaSearchScope(elements, IJavaSearchScope.SOURCES);
      int matchRule= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
      SearchPattern annotationsPattern= SearchPattern.createPattern("org.testng.annotations.Test", 
                                            IJavaSearchConstants.ANNOTATION_TYPE, 
                                            IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE, matchRule);
      SearchParticipant[] searchParticipants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
      new SearchEngine().search(annotationsPattern, searchParticipants, scope, requestor, new SubProgressMonitor(pm, 2));
    } finally {
      pm.done();
    }
  }

  public void findTestsInContainer(IJavaElement element, Set result, IProgressMonitor pm) throws CoreException {
    if (element == null || result == null) {
      throw new IllegalArgumentException();
    }

    if (element instanceof IType) {
      if (internalIsTest((IType) element, pm)) {
        result.add(element);
        return;
      }
    }

    if (pm == null)
      pm= new NullProgressMonitor();

    try {
      pm.beginTask(ResourceUtil.getString("TestSearchEngine.message.searching"), 4);

      IRegion region= CoreTestSearchEngine.getRegion(element);
      ITypeHierarchy hierarchy= JavaCore.newTypeHierarchy(region, null, new SubProgressMonitor(pm, 1));
      IType[] allClasses= hierarchy.getAllClasses();

      // search for all types with references to RunWith and Test and all subclasses
      Set<IType> candidates= new HashSet<>(allClasses.length);
      SearchRequestor requestor= new AnnotationSearchRequestor(hierarchy, candidates);

      IJavaSearchScope scope= SearchEngine.createJavaSearchScope(allClasses, IJavaSearchScope.SOURCES);
      int matchRule= SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE;
      SearchPattern annotationsPattern= SearchPattern.createPattern("org.testng.annotations.Test", IJavaSearchConstants.ANNOTATION_TYPE, IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE, matchRule);
      SearchParticipant[] searchParticipants= new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
      new SearchEngine().search(annotationsPattern, searchParticipants, scope, requestor, new SubProgressMonitor(pm, 2));

      // find all classes in the region
      for (IType curr : candidates) {
        if (CoreTestSearchEngine.isAccessibleClass(curr) && !Flags.isAbstract(curr.getFlags()) && region.contains(curr)) {
          result.add(curr);
        }
      }
    } finally {
      pm.done();
    }
  }

  private boolean internalIsTest(IType type, IProgressMonitor monitor) throws JavaModelException {
    if (CoreTestSearchEngine.isAccessibleClass(type)) {
      ASTParser parser= ASTParser.newParser(AST.JLS4);
      /* TODO: When bug 156352 is fixed:
      parser.setProject(type.getJavaProject());
      IBinding[] bindings= parser.createBindings(new IJavaElement[] { type }, monitor);
      if (bindings.length == 1 && bindings[0] instanceof ITypeBinding) {
        ITypeBinding binding= (ITypeBinding) bindings[0];
        return isTest(binding);
      }*/

      if (type.getCompilationUnit() != null) {
        parser.setSource(type.getCompilationUnit());
      } else if (!isAvailable(type.getSourceRange())) { // class file with no source
        parser.setProject(type.getJavaProject());
        IBinding[] bindings= parser.createBindings(new IJavaElement[] { type }, monitor);
        if (bindings.length == 1 && bindings[0] instanceof ITypeBinding) {
          ITypeBinding binding= (ITypeBinding) bindings[0];
          return isTest(binding);
        }
        return false;
      } else {
        parser.setSource(type.getClassFile());
      }
      parser.setFocalPosition(0);
      parser.setResolveBindings(true);
      CompilationUnit root= (CompilationUnit) parser.createAST(monitor);
      ASTNode node= root.findDeclaringNode(type.getKey());
      if (node instanceof TypeDeclaration) {
        ITypeBinding binding= ((TypeDeclaration) node).resolveBinding();
        if (binding != null) {
          return isTest(binding);
        }
      }
    }
    return false;
  }
  
  private static boolean isAvailable(ISourceRange range) {
    return range != null && range.getOffset() != -1;
  }
  
  private boolean isTest(ITypeBinding binding) {
    if (Modifier.isAbstract(binding.getModifiers()))
      return false;

    return annotatesAtLeastOneMethod(binding, "org.testng.annotations.Test");
  }

  public boolean annotatesAtLeastOneMethod(ITypeBinding type, String qualifiedName) {
    while (type != null) {
      IMethodBinding[] declaredMethods= type.getDeclaredMethods();
      for (int i= 0; i < declaredMethods.length; i++) {
        IMethodBinding curr= declaredMethods[i];
        if (annotates(curr.getAnnotations(), qualifiedName)) {
          return true;
        }
      }
      type= type.getSuperclass();
    }
    return false;
  }

  private boolean annotates(IAnnotationBinding[] annotations, String qualifiedName) {
    for (int i= 0; i < annotations.length; i++) {
      ITypeBinding annotationType= annotations[i].getAnnotationType();
      if (annotationType != null && (annotationType.getQualifiedName().equals(qualifiedName))) {
        return true;
      }
    }
    return  false;
  }

  public static class TestReferenceCollector extends SearchRequestor {
    Set<IType> fResult = new HashSet<>(50);

    @Override
    public void acceptSearchMatch(SearchMatch match) throws CoreException {
      if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()) {
        Object element= match.getElement();
        if (element instanceof IType || element instanceof IMethod) {
          IMember member= (IMember) element;
          IType type= member.getElementType() == IJavaElement.TYPE ? (IType) member : member.getDeclaringType();
          fResult.add(type);
        }
      }
    }

    public Set<IType> getResult() {
      return this.fResult;
    }
  }
}
