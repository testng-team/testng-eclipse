package org.testng.eclipse.ui.tree;

import org.eclipse.swt.widgets.TreeItem;
import org.testng.eclipse.ui.RunInfo;

public interface ITreeItem {

  void update(RunInfo runInfo);

  RunInfo getRunInfo();

  void addToCumulatedTime(float time);

  TreeItem getTreeItem();
}
