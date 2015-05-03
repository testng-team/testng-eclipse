package org.testng.eclipse.ui.conversion;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.List;

/**
 * Base class for all our visitors.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class Visitor extends ASTVisitor {
  @SuppressWarnings("unchecked")
  protected Annotation getAnnotation(MethodDeclaration md, String annotation) {
    return getAnnotation(md.modifiers(), annotation);
  }
  
  @SuppressWarnings("unchecked")
  protected Annotation getAnnotation(TypeDeclaration td, String annotation) {
    return getAnnotation(td.modifiers(), annotation);
  }

  private Annotation getAnnotation(List<IExtendedModifier> modifiers, String annotation) {
    for (IExtendedModifier m : modifiers) {
      if (m.isAnnotation()) {
        Annotation a = (Annotation) m;
        if (annotation.equals(a.getTypeName().toString())) {
          return a;
        }
      }
    }

    return null;
  }

  /**
   * @return true if the given method is annotated with the annotation
   */
  protected boolean hasAnnotation(MethodDeclaration md, String annotation) {
    return getAnnotation(md, annotation) != null;
  }

}
