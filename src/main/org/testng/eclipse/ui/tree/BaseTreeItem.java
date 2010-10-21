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

  public void addToCumulatedTime(float f) {
    m_time += f;
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

}
