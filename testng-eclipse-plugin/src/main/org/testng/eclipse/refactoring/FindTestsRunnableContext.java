package org.testng.eclipse.refactoring;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import java.lang.reflect.InvocationTargetException;

public class FindTestsRunnableContext implements IRunnableContext {

  public void run(boolean fork, boolean cancelable,
      IRunnableWithProgress runnable) throws InvocationTargetException,
      InterruptedException {
    runnable.run(new NullProgressMonitor());
  }

}
