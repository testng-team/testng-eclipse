package org.testng.eclipse.util;


import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.components.Filters;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.ui.util.FindTestNGResourcesUtil;
import org.testng.eclipse.ui.util.TypeParser;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;


/**
 * Search engine for TestNG related elements.
 * <P/>
 * Original idea from org.eclipse.jdt.internal.junit.util.TestSearchEngine
 *
 * @author <a href='mailto:the_mindstorm@evolva.ro'>Alexandru Popescu</a>
 * @author cedric
 */
public class TestSearchEngine {

  /**
   * Searches for TestNG test types.
   *
   * @param context
   * @param javaProject
   * @return
   * @throws InvocationTargetException
   * @throws InterruptedException
   */
  public static IType[] findTests(IRunnableContext context,
                                  final Object[] elements,
                                  final Filters.ITypeFilter filter) throws InvocationTargetException, InterruptedException {
    final Set<IType> result = new HashSet<IType>();

    if(elements.length != 0) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
          public void run(IProgressMonitor pm) throws InterruptedException {
            result.addAll(doFindTests(elements, pm, filter));
          }
        };
      context.run(true, true, runnable);
    }

    return (IType[]) result.toArray(new IType[result.size()]);
  }

  public static String[] findPackages(IRunnableContext context,
			final IJavaProject javaProject)
			throws InvocationTargetException, InterruptedException {
		final Set<String> result = new HashSet<String>();

    if (javaProject != null) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
        public void run(IProgressMonitor pm) throws InterruptedException {
          result.addAll(findPackages(javaProject, pm));
        }
      };
      context.run(true, true, runnable);
    }

		return (String[]) result.toArray(new String[]{});
	}

  public static String[] findMethods(IRunnableContext context,
			final Object[] elements, final String className)
			throws InvocationTargetException, InterruptedException {
		final Set result = new HashSet();

		if (elements.length != 0) {
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor pm)
						throws InterruptedException {
					doFindMethods(elements, result, pm, className);
				}
			};
			context.run(true, true, runnable);
		}

		return (String[])result.toArray(new String[result.size()]);
	}

  public static File[] findSuites(IRunnableContext context,
                                  final Object[] elements) throws InvocationTargetException, InterruptedException {
    final Set result = new HashSet();

    if(elements.length != 0) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
          public void run(IProgressMonitor pm) throws InterruptedException {
            doFindSuites(elements, result, pm);
          }
        };
      context.run(true, true, runnable);
    }

    return (File[]) result.toArray(new File[result.size()]);
  }

  public static IFile[] findSuites(final Object[] elements) throws InterruptedException,
                                                                   InvocationTargetException {
    final Set result = new HashSet();

    if(elements.length > 0) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
          public void run(IProgressMonitor pm) throws InterruptedException {
            doFindSuites(elements, result, pm);
          }
        };
      PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
    }

    return (IFile[]) result.toArray(new IFile[result.size()]);
  }

  @SuppressWarnings("unchecked")
  public static IType[] findTests(final Object[] elements, final Filters.ITypeFilter filter)
  throws InvocationTargetException, InterruptedException {
    final Set result = new HashSet();

    if(elements.length > 0) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
          public void run(IProgressMonitor pm) throws InterruptedException {
            result.addAll(doFindTests(elements, pm, filter));
          }
        };
      PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
    }

    return (IType[]) result.toArray(new IType[]{});
  }

  public static IType[] findPackages(final Object[] elements)
  throws InvocationTargetException, InterruptedException {
    final Set result = new HashSet();

    if(elements.length > 0) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
          public void run(IProgressMonitor pm) throws InterruptedException {
          result.addAll(findPackages(null, pm));
          }
        };
      PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
    }

    return (IType[]) result.toArray(new IType[result.size()]);
  }


  private static Map/*<IJavaElement,Boolean>*/ s_isTestCache= new HashMap();

  /**
   * Returns true in case the IJavaElement is one of COMPILATION_UNIT, TYPE or METHOD
   * representing a valid TestNG test element.
   */
  public static boolean isTest(IJavaElement ije) {
    Boolean cachedResult= (Boolean) s_isTestCache.get(ije);
    if(null != cachedResult && cachedResult.booleanValue()) {
      return true;
    }

    boolean result= false;
    IType[] types = null;

    if(IJavaElement.METHOD == ije.getElementType()) {
      IMethod iMethod = (IMethod) ije;
      ITestContent content = TypeParser.parseType(iMethod.getDeclaringType());
      if(content.hasTestMethods()) {
        result= content.isTestMethod(iMethod);
        if(result) {
          s_isTestCache.put(ije, Boolean.TRUE);
        }
        return result;
      }

      return false;
    }

    if(IJavaElement.COMPILATION_UNIT == ije.getElementType()) {
      try {
        types = ((ICompilationUnit) ije).getAllTypes();
      }
      catch(JavaModelException jme) {
        TestNGPlugin.log(jme);
      }
    }
    else if(IJavaElement.TYPE == ije.getElementType()) {
      types = new IType[] {(IType) ije};
    }
    else {
      return false;
    }

    if(null != types) {
      for(int i = 0; i < types.length; i++) {
        ITestContent testContent = TypeParser.parseType(types[i]);

        if(testContent.hasTestMethods()) {
          s_isTestCache.put(ije, Boolean.TRUE);
          return true;
        }
      }
    }

    return false;
  }

  private static Set<IType> doFindTests(Object[] elements,
                                  IProgressMonitor pm,
                                  Filters.ITypeFilter filter) throws InterruptedException {
    int nElements = elements.length;
    Set<IType> result = new HashSet<IType>();
    pm.beginTask(ResourceUtil.getString("TestSearchEngine.message.searching"), nElements); //$NON-NLS-1$
    try {
      if (elements != null) {
        for (int i = 0; i < elements.length; i++) {
          if (elements[i] instanceof IJavaElement) {
            Set<IType> searchResults = FindTestNGResourcesUtil.findClasses(
                new IJavaElement[] { (IJavaElement) elements[i] }, pm);
            if (searchResults != null && searchResults.size() > 0) {
              ppp("found " + searchResults.size() + " results");
              result.addAll(searchResults);
            } else {
              ppp("did not find any result");
            }
          }
        }
      }
    } catch (Exception e) {
      TestNGPlugin.log(e);
      if (pm.isCanceled()) {
        throw new InterruptedException();
      }
    } finally {
      pm.done();
    }
    return result;
  }

  private static void doFindMethods(Object[] elements, Set result,
			IProgressMonitor pm, String className)
			throws InterruptedException {
		int nElements = elements.length;
		pm.beginTask(ResourceUtil
				.getString("TestSearchEngine.message.searching"), nElements); //$NON-NLS-1$
		try {
			for (int i = 0; i < nElements; i++) {

				if (elements[i] instanceof IJavaElement) {
					findMethods(((IJavaElement) elements[i]).getJavaProject(),
							result, className);
				}

				if (pm.isCanceled()) {
					throw new InterruptedException();
				}
			}
		} finally {
			pm.done();
		}
	}
  private static boolean isTestNgXmlFile(IFile f) {
    if(!"xml".equals(f.getFileExtension())) {
      return false;
    }

    try {
      return SuiteFileValidator.isSuiteDefinition(f);
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }

    return false;
  }

  private static void doFindSuites(Object[] elements, Set result,
                                   IProgressMonitor pm)
  throws InterruptedException
{
    int nElements = elements.length;
    pm.beginTask(ResourceUtil.getString("TestSearchEngine.message.searching"), nElements); //$NON-NLS-1$

    try {
      for(int i = 0; i < nElements; i++) {
        if(elements[i] instanceof IJavaProject) {
          findSuites(((IJavaProject) elements[i]).getProject(), result);
        }
        if(pm.isCanceled()) {
          throw new InterruptedException();
        }
      }
    }
    finally {
      pm.done();
    }

  }

  private static void findSuites(IContainer ires, Set results) {
    if(null == ires) {
      return;
    }

    try {
      IResource[] children = ires.members();
      for(int i = 0; i < children.length; i++) {
        if(children[i] instanceof IFile) {
          if(isTestNgXmlFile((IFile) children[i])) {
            results.add(children[i]);
          }
        }
        else {
          findSuites((IContainer) children[i], results);
        }
      }
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }
  }

  private static Set<String> findPackages(IJavaProject javaProject,
      IProgressMonitor progressMonitor) throws InterruptedException {
    Set<String> result = new HashSet<String>();
    progressMonitor.beginTask(ResourceUtil.getString("TestSearchEngine.message.searching"), 1); //$NON-NLS-1$
    try {
      result = FindTestNGResourcesUtil.findPackages(javaProject, progressMonitor);
    } catch (Exception e) {
      TestNGPlugin.log(e);
      if (progressMonitor.isCanceled()) {
        throw new InterruptedException();
      }
    } finally {
      progressMonitor.done();
    }
    return result;
  }

  private static void findMethods(IJavaElement ije, Set result, String className) {
		if (IJavaElement.PACKAGE_FRAGMENT > ije.getElementType()) {
			try {
				IJavaElement[] children = ((IParent) ije).getChildren();
				if (children.length == 0) {return;}
				for (int i = 0; i < children.length; i++) {
					findMethods(children[i], result, className);
				}
			} catch (JavaModelException jme) {
				TestNGPlugin.log(jme);
			}
		}

		if (IJavaElement.PACKAGE_FRAGMENT == ije.getElementType()) {
			try {
				ICompilationUnit[] compilationUnits = ((IPackageFragment) ije)
						.getCompilationUnits();

				if (compilationUnits.length == 0) {return;}
				for (int i = 0; i < compilationUnits.length; i++) {
					findMethods(compilationUnits[i], result, className);
				}

			} catch (JavaModelException jme) {
				TestNGPlugin.log(jme);
			}
		}

		if (IJavaElement.COMPILATION_UNIT == ije.getElementType()) {
			try {
				IType[] types = ((ICompilationUnit) ije).getAllTypes();

				for (int i = 0; i < types.length; i++) {
					IType classType;
					if (Filters.SINGLE_TEST.accept(types[i])) {
						if (IJavaElement.TYPE == types[i].getElementType()) {
							classType = (IType)types[i];
						}
						else if (IJavaElement.CLASS_FILE == types[i].getElementType()) {
							classType = ((IClassFile)types[i]).findPrimaryType();
						}
						else {
							classType = null;
						}

						if (classType != null) {
							if (className.equals("") || classType.getFullyQualifiedName().equals(className)) {
								IMethod[] methods = classType.getMethods();
								for (int j = 0; j < methods.length; j++) {
									if (TypeParser.parseType(classType).isTestMethod(methods[j])) {
										result.add(methods[j].getDeclaringType().getFullyQualifiedName() + "." + methods[j].getElementName());
									}
								}
							}
						}
					}
				}
			} catch (JavaModelException jme) {
				TestNGPlugin.log(jme);
			}
		}
	}


  public static void ppp(String s) {
    System.out.println("[TestSearchEngine] " + s);
  }

}
