package org.testng.eclipse.ui.tree;

import org.eclipse.swt.widgets.TreeItem;
import org.testng.eclipse.ui.RunInfo;

/**
 * A tree node representing the parameters of a test method.
 * 
 * @author Cedric Beust <cedric@beust.com>
 */
public class TestMethodParametersTreeItem extends BaseTestMethodTreeItem {

  public TestMethodParametersTreeItem(TreeItem parent, RunInfo runInfo) {
    super(parent, runInfo);
  }

  protected String getLabel() {
    String result = getRunInfo().getParametersDisplay();
    result = result.replace("(", "");
    result = result.replace(")", "");
    return result;
  }

}

