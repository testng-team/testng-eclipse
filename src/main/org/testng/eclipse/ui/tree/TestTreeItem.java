package org.testng.eclipse.ui.tree;

import org.eclipse.swt.widgets.TreeItem;
import org.testng.eclipse.ui.RunInfo;

/**
 * The representation in the tree of a test method.
 * 
 * @author cbeust
 */
public class TestTreeItem extends BaseTreeItem implements ITreeItem {
  public TestTreeItem(TreeItem parent, RunInfo runInfo) {
    super(parent, runInfo);
  }

  public void update(RunInfo runInfo) {
    getTreeItem().setText(runInfo.getTestName());
  }
}
