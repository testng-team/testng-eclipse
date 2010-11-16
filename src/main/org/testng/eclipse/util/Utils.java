package org.testng.eclipse.util;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.IWorkbenchPage;

public class Utils {
  public static Object getSelectedProjectOrPackage(IWorkbenchPage page) {
    Object result = null;
    ISelection selection = page.getSelection();

    if (selection instanceof TreeSelection) {
      TreeSelection sel = (TreeSelection) selection;
      TreePath[] paths = sel.getPaths();
      for (TreePath path : paths) {
        int count = path.getSegmentCount();
        if (count == 1) {
          result = path.getFirstSegment();
        } else if (count == 2) {
          result = path.getSegment(1);
        } else {
          System.out.println("Unknown path:" + path);
        }
      }
    }

    return result;
  }

}
