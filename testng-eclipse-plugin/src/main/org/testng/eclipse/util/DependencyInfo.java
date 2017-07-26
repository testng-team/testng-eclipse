package org.testng.eclipse.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.TestFinder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * A class that represents all the information about groups in the current project
 * (which types depend on which groups, which types define groups, which methods
 * define on which groups and which methods define groups.
 */
public class DependencyInfo {
  Multimap<String, IType> typesByGroups = ArrayListMultimap.create();
  Multimap<IType, String> groupDependenciesByTypes = ArrayListMultimap.create();
  Multimap<String, IMethod> methodsByGroups = ArrayListMultimap.create();
  Multimap<IMethod, String> groupDependenciesByMethods = ArrayListMultimap.create();
  Multimap<IMethod, IMethod> methodsByMethods = ArrayListMultimap.create();

  public static DependencyInfo createDependencyInfo(final IJavaProject javaProject) {
    final DependencyInfo result = new DependencyInfo();

    final IRunnableWithProgress runnable = new IRunnableWithProgress() {

      public void run(IProgressMonitor monitor)
          throws InvocationTargetException, InterruptedException {
        try {
          monitor.beginTask("Launching", 2000);
          monitor.subTask("Calculating dependencies");

          Set<IType> types = TestFinder.findTests(javaProject, monitor);
          for (IType type : types) {
            for (IMethod method : type.getMethods()) {
              for (IAnnotation annotation : method.getAnnotations()) {
                monitor.worked(1);
                IMemberValuePair[] pairs = annotation.getMemberValuePairs();
                if ("Test".equals(annotation.getElementName()) && pairs.length > 0) {
                  for (IMemberValuePair pair : pairs) {

                    if ("groups".equals(pair.getMemberName())) {
                      Object groups = pair.getValue();
                      if (groups.getClass().isArray()) {
                        for (Object o : (Object[]) groups) {
                          result.typesByGroups.put(o.toString(), type);
                          result.methodsByGroups.put(o.toString(), method);
                        }
                      } else {
                        result.typesByGroups.put(groups.toString(), type);
                        result.methodsByGroups.put(groups.toString(), method);
                      }
                    } else if ("dependsOnGroups".equals(pair.getMemberName())) {
                      Object dependencies = pair.getValue();
                      if (dependencies.getClass().isArray()) {
                        for (Object o : (Object[]) dependencies) {
                          result.groupDependenciesByTypes.put(type, o.toString());
                          result.groupDependenciesByMethods.put(method, o.toString());
                        }
                      } else {
                        result.groupDependenciesByTypes.put(type, dependencies.toString());
                        result.groupDependenciesByMethods.put(method,dependencies.toString());
                      }

                    } else if ("dependsOnMethods".equals(pair.getMemberName())) {
                      Object dependencies = pair.getValue();
                      IType methodType = method.getDeclaringType();
                      if (dependencies.getClass().isArray()) {
                        for (Object o : (Object[]) dependencies) {
                          IMethod depMethod = JDTUtil.fuzzyFindMethodInProject(javaProject, methodType,
                              method, o.toString());
                          if (depMethod != null) {
                            result.methodsByMethods.put(method, depMethod);
                          }
                        }
                      } else {
                        IMethod depMethod = JDTUtil.fuzzyFindMethodInProject(javaProject, methodType,
                            method, dependencies.toString());
                        if (depMethod != null) {
                          result.methodsByMethods.put(method, depMethod);
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        } catch (CoreException e) {
          TestNGPlugin.log(e);
        }
      }
    };

    Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
    ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);

    try {
      dialog.run(true /* fork */, true /* cancelable */, runnable);
    } catch (InvocationTargetException | InterruptedException e) {
      TestNGPlugin.log(e);
    }

    return result;
  }
}
