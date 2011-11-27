package org.testng.eclipse.ui.tree;

import org.eclipse.swt.widgets.TreeItem;
import org.testng.eclipse.ui.RunInfo;

/**
 * Note that ClassTreeItems and the node classes above in the tree (TestTreeItem
 * and SuiteTreeItem) don't really own one RunInfo but several (one for each test
 * method underneath them). For this reason, the runInfo property of these items
 * is set to the first one they encounter, but as more updates come in, the new
 * RunInfo objects are passed directly to their update() method, insted of using
 * their own.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class ClassTreeItem extends BaseTreeItem implements ITreeItem {

  public ClassTreeItem(TreeItem parent, RunInfo runInfo) {
    super(parent, runInfo);
    update(runInfo);
  }

  public void update(RunInfo runInfo) {
    getTreeItem().setText(runInfo.getInstanceName());
    maybeUpdateImage(runInfo);
  }

}
