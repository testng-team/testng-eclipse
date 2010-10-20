package org.testng.eclipse.launch;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.core.PackageFragment;
import org.eclipse.jdt.internal.core.PackageFragmentRoot;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.collections.Lists;
import org.testng.eclipse.launch.tester.JavaTypeExtender;
import org.testng.eclipse.util.LaunchUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Right-click launcher.
 * 
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 */
public class TestNGLaunchShortcut implements ILaunchShortcut {

  public void launch(ISelection selection, String mode) {
    if(selection instanceof StructuredSelection) {
      List<IType> types = Lists.newArrayList();
      IJavaProject project = null;

      for (Object obj : ((StructuredSelection) selection).toArray()) { 
        IJavaElement element= null;
        // Special case for IMethod: we run it directly here (and it must appear before
        // the test for IJavaElement, which would return true but is more general).
        // Anything above IMethod will be accumulated in the types collection since
        // it can contain more than one TestNG type.
        if (obj instanceof IMethod) {
          run((IMethod) obj, mode);
        }
        else if(obj instanceof IJavaElement) {
          element= (IJavaElement) obj;
        }
        else if(obj instanceof IAdaptable) {
          element= (IJavaElement) ((IAdaptable) obj).getAdapter(IJavaElement.class);
        }
        project = element.getJavaProject();

        try {
          maybeAddJavaElement(element, types);
        } catch (JavaModelException e) {
          TestNGPlugin.log(e);
        }

      }
      
      if (! types.isEmpty()) {
        LaunchUtil.launchTypesConfiguration(project, types, mode);
      }
    }
  }

  private void maybeAddJavaElement(IJavaElement element, List<IType> units)
      throws JavaModelException {
    p("Examining Java element:" + element);
    if (element != null) {
      if (element instanceof JavaProject) {
        JavaProject p = (JavaProject) element;
        for (IJavaElement e : p.getChildren()) {
          maybeAddJavaElement(e, units);
        }
      } else if (element instanceof SourceType) {
        units.add((SourceType) element);
      } else if (element instanceof ICompilationUnit) {
        units.addAll(Arrays.asList(((ICompilationUnit) element).getTypes()));
      } else if (element instanceof PackageFragment) {
        PackageFragment p = (PackageFragment) element;
        for (ICompilationUnit icu : p.getCompilationUnits()) {
          units.addAll(Arrays.asList(icu.getTypes()));
        }
      } else if (element instanceof PackageFragmentRoot) {
        PackageFragmentRoot pfr = (PackageFragmentRoot) element;
        for (IJavaElement e : pfr.getChildren()) {
          if (JavaTypeExtender.isTest(e)) {
            maybeAddJavaElement(e, units);
          }
        }
      } else {
        p("Ignoring non compilation unit selection: " + element);
      }
    }
  }

  private static void p(String s) {
    TestNGPlugin.log("[TestNGLaunchShortcut] " + s);
//    System.out.println("[TestNGLaunchShortcut] " + s);
  }

  public void launch(IEditorPart editor, String mode) {
	  ITypeRoot root = JavaUI.getEditorInputTypeRoot(editor.getEditorInput());
	  if (root != null) {
	    IMethod method = resolveSelectedMethod(editor, root);
	    if (method != null) {
	      run(method, mode);
	    }
	    else if (root instanceof IJavaElement){
	      run(root, mode);
	    }
	  }
  }

  private IMethod resolveSelectedMethod(IEditorPart editor, ITypeRoot root) {
    try {
      ITextSelection selectedText = getTextSelection(editor, root);
      if(selectedText == null) {
        return null;
      }
      IJavaElement selectedElement = SelectionConverter.getElementAtOffset(root, selectedText);
      if(!(selectedElement instanceof IMethod)) {
        return null;
      }
      IMethod method= (IMethod) selectedElement;
      ISourceRange nameRange = method.getNameRange();
      if(nameRange.getOffset() <= selectedText.getOffset() && selectedText.getOffset() + selectedText.getLength() <= nameRange.getOffset() + nameRange.getLength()) {
        return method;
      }
    } catch (JavaModelException jme) {
      ;
    }
    return null;
  }

  private ITextSelection getTextSelection(IEditorPart editor, ITypeRoot root) {
    ISelectionProvider selectionProvider = editor.getSite().getSelectionProvider();
    if(selectionProvider == null) {
      return null;
    }

    ISelection selection = selectionProvider.getSelection();
    if(!(selection instanceof ITextSelection)) {
      return null;
    }

    return (ITextSelection) selection;
  }

  protected void run(IJavaElement ije, String mode) {
    IJavaProject ijp = ije.getJavaProject();

    switch(ije.getElementType()) {
      case IJavaElement.PACKAGE_FRAGMENT:
      {
        LaunchUtil.launchPackageConfiguration(ijp, (IPackageFragment) ije, mode);
        
        return;
      }
      
      case IJavaElement.COMPILATION_UNIT:
      {
        LaunchUtil.launchCompilationUnitConfiguration(ijp,
            Arrays.asList(new ICompilationUnit[] { (ICompilationUnit) ije }), mode); 

        return;
      }
      
      case IJavaElement.TYPE:
      {
        LaunchUtil.launchTypeConfiguration(ijp, (IType) ije, mode);
        
        return;
      }
      
      case IJavaElement.METHOD:
      {
        LaunchUtil.launchMethodConfiguration(ijp, (IMethod) ije, mode); 
        
        return;
      }
      
      default:
        return;
    }    
  }

  /*protected void launchConfiguration(ILaunchConfiguration config, String mode) {
    if(null != config) {
      DebugUITools.launch(config, mode);
    }
  }*/
  
  /*protected ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }*/  
}
