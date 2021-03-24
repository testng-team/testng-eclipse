package org.testng.eclipse.ui.codemining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.ui.texteditor.ITextEditor;
import org.testng.eclipse.Messages;
import org.testng.eclipse.TestNGPlugin;

@SuppressWarnings("restriction")
public class TestCodeMiningProvider extends AbstractCodeMiningProvider {
  private static final String MODE_RUN = "run"; //$NON-NLS-1$

  private static final String MODE_DEBUG = "debug"; //$NON-NLS-1$
  
  private static final String[] EXPECTED_TYPE = {"org.testng.annotations", "Test"}; //$NON-NLS-1$ //$NON-NLS-2$

  @Override
  public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(
      ITextViewer viewer, IProgressMonitor monitor) {
    return CompletableFuture
        .supplyAsync(() -> mineForTests(viewer, monitor, viewer.getDocument()));
  }

  public List<? extends ICodeMining> mineForTests(ITextViewer viewer,
      IProgressMonitor monitor, IDocument document) {
  
    if (monitor.isCanceled()) {
      return null;
    }
  
    final ITextEditor editor = super.getAdapter(ITextEditor.class);
    final ITypeRoot typeRoot = EditorUtility.getEditorInputJavaElement(editor, true);
    if (typeRoot == null) {
      return Collections.emptyList();
    }
    
    try {
      final IPackageFragmentRoot packageFragmentRoot = ((IPackageFragmentRoot) typeRoot
          .getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT));
      // do not process non-test classes
      if(packageFragmentRoot == null || !packageFragmentRoot.getResolvedClasspathEntry().isTest()) {
        return Collections.emptyList();
      }
      
      IJavaElement[] elements = typeRoot.getChildren();
      final List<ICodeMining> minings = new ArrayList<>(elements.length);
  
      processElements(elements, minings, monitor, document);
  
      return minings;
    } catch (Exception e) {
      TestNGPlugin.log(e);
      return Collections.emptyList();
    }
  }

  private void processElements(IJavaElement[] elements,
      List<ICodeMining> minings, IProgressMonitor monitor, IDocument document)
      throws Exception {
    for (IJavaElement element : elements) {
      if (monitor.isCanceled()) {
        return;
      }

      try {
        if (element.getElementType() == IJavaElement.TYPE) {
          final IJavaElement[] children = ((IType) element).getChildren();
          final List<ICodeMining> im = new ArrayList<>(children.length);

          processElements(children, im, monitor, document);

          if (!im.isEmpty()) {
            minings.addAll(im);

            // normally you debug method by method so no need a Debug All
            minings.add(new TestCodeMining(element, document, this, Messages.mining_runAll,
                MODE_RUN));
          }
        } else if (element.getElementType() == IJavaElement.METHOD) {
          if (isTest((IMethod) element)) {
            minings.add(
                new TestCodeMining(element, document, this, Messages.mining_run, MODE_RUN));
            minings.add(new TestCodeMining(element, document, this, Messages.mining_debug,
                MODE_DEBUG));
          }
        }
      } catch (JavaModelException e) {
        TestNGPlugin.log(e);
      }
    }
  }

  private boolean isTest(IMethod method) throws JavaModelException {
    Optional<IAnnotation> annotation = Arrays.stream(method.getAnnotations())
        .filter(a -> "Test".equals(a.getElementName()) || "org.testng.annotations.Test".equals(a.getElementName())).findFirst(); //$NON-NLS-1$ //$NON-NLS-2$
    if(annotation.isPresent()) {
      String[][] resolveType = method.getDeclaringType().resolveType(annotation.get().getElementName());
      return Optional.ofNullable(resolveType)
          .flatMap(r -> Stream.of(r).filter(e -> Arrays.equals(EXPECTED_TYPE, e)).findFirst()).isPresent();
    }
    return false;
  }
}
