package org.testng.eclipse.launch;

import java.util.Collection;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchRequestor;

public class AnnotationSearchRequestor extends SearchRequestor {

  private final Collection<IType> fResult;
  private final ITypeHierarchy fHierarchy;

  public AnnotationSearchRequestor(ITypeHierarchy hierarchy, Collection<IType> result) {
    fHierarchy= hierarchy;
    fResult= result;
  }

  public void acceptSearchMatch(SearchMatch match) throws CoreException {
    if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment()) {
      Object element= match.getElement();
      if (element instanceof IType || element instanceof IMethod) {
        IMember member= (IMember) element;
        IType type= member.getElementType() == IJavaElement.TYPE ? (IType) member : member.getDeclaringType();
        addTypeAndSubtypes(type);
      }
    }
  }

  private void addTypeAndSubtypes(IType type) {
    if (fResult.add(type)) {
      IType[] subclasses= fHierarchy.getSubclasses(type);
      for (int i= 0; i < subclasses.length; i++) {
        addTypeAndSubtypes(subclasses[i]);
      }
    }
  }

}
