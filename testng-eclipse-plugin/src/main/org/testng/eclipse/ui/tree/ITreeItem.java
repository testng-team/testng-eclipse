package org.testng.eclipse.ui.tree;

import org.eclipse.swt.widgets.TreeItem;
import org.testng.eclipse.ui.RunInfo;

/**
 * Every node in the tree item has an object that implements this interface attached
 * to it.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public interface ITreeItem {

  void update(RunInfo runInfo);

  RunInfo getRunInfo();

  void addToCumulatedTime(RunInfo runInfo);

  TreeItem getTreeItem();
}
