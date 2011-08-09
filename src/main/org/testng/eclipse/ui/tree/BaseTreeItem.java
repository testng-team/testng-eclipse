package org.testng.eclipse.ui.tree;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.testng.ITestResult;
import org.testng.eclipse.ui.Images;
import org.testng.eclipse.ui.RunInfo;

abstract public class BaseTreeItem implements ITreeItem {
  private static final String DATA_TREE_ITEM = "treeItem";
  private float m_time;
  private TreeItem m_treeItem;
  private RunInfo m_runInfo;
  private boolean m_hasImage = false;

  public static ITreeItem getTreeItem(TreeItem ti) {
    return (ITreeItem) ti.getData(DATA_TREE_ITEM);
  }

  public BaseTreeItem(TreeItem parent, RunInfo runInfo) {
    m_treeItem = new TreeItem(parent, SWT.None);
    init(runInfo);
  }

  public BaseTreeItem(Tree parent, RunInfo runInfo) {
    m_treeItem = new TreeItem(parent, SWT.None);
    init(runInfo);
  }
  
  private void init(RunInfo runInfo) {
    m_runInfo = runInfo;
    m_treeItem.setExpanded(true);
    m_treeItem.setData(DATA_TREE_ITEM, this);
  }

  public void addToCumulatedTime(RunInfo runInfo) {
    m_time += runInfo.getTime();
    update(runInfo);
  }

  protected float getTime() {
    return m_time;
  }

  public TreeItem getTreeItem() {
    return m_treeItem;
  }

  public RunInfo getRunInfo() {
    return m_runInfo;
  }

  /**
   * @return the icon to display for a suite with the given state.
   */
  protected Image getSuiteImage(int state) {
    switch(state) {
      case ITestResult.SUCCESS:
        return Images.getImage(Images.IMG_SUITE_OK);
      case ITestResult.FAILURE:
      case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
        return Images.getImage(Images.IMG_SUITE_FAIL);
      case ITestResult.SKIP:
        return Images.getImage(Images.IMG_SUITE_SKIP);
      default:
        throw new IllegalArgumentException("Illegal state:" + state);
    }
  }

  /**
   * Once a node is in failure, it needs to remain in failure, so only update it if
   * 1) it hasn't received an image yet or 2) it's being updated to something else
   * than a success.
   */
  protected void maybeUpdateImage(RunInfo runInfo) {
    int status = runInfo.getStatus();
    TreeItem treeItem = getTreeItem();
    if (! m_hasImage || status != ITestResult.SUCCESS) {
      treeItem.setImage(getSuiteImage(status));
      m_hasImage = true;
    }
  }
}
