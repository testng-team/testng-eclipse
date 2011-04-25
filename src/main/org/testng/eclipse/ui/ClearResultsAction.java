package org.testng.eclipse.ui;

import org.eclipse.jface.action.Action;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.ResourceUtil;

import java.util.List;

public class ClearResultsAction extends Action {
  private List<TestRunTab> m_tabs;

  public ClearResultsAction(List<TestRunTab> tabs) {
    m_tabs = tabs;
    setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/clear.gif"));
    setToolTipText(ResourceUtil.getString("TestRunnerViewPart.clearResults.tooltip")); //$NON-NLS-1$
  }

  public void run() {
    for (TestRunTab tab : m_tabs) {
      tab.aboutToStart();
    }
  }

}
