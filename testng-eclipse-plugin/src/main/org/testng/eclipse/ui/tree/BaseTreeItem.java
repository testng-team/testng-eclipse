package org.testng.eclipse.ui.tree;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.testng.ITestResult;
import org.testng.eclipse.ui.ImagesUtil;
import org.testng.eclipse.ui.RunInfo;

abstract public class BaseTreeItem implements ITreeItem {
  private static final String DATA_TREE_ITEM = "treeItem";
  private float m_time;
  private TreeItem m_treeItem;
  private RunInfo m_runInfo;
  private Integer m_testState = 0;

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
   * Once a node is in failure, it needs to remain in failure, so only update it if
   * 1) it hasn't received an image yet or 2) it's being updated to something else
   * than a success.
   */
  protected void maybeUpdateImage(RunInfo runInfo) {
    int status = runInfo.getStatus();
    TreeItem treeItem = getTreeItem();
    if (m_testState == 0 || isNewState(status)) {
      treeItem.setImage(ImagesUtil.getSuiteImage(status));
      m_testState = status;
    }
  }

  /**
   * Only keep the most problematic state to give an accurate view to the user.
   *
   * @param state the new state to test
   * @return whether the passed in state will be the new state for this TreeItem.
   */
  private boolean isNewState(int state) {
    switch (m_testState) {
    case ITestResult.STARTED:
      return state != ITestResult.STARTED;
    case ITestResult.SUCCESS:
      return state != ITestResult.STARTED && state != ITestResult.SUCCESS;
    case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
      return state == ITestResult.FAILURE || state == ITestResult.SKIP;
    case ITestResult.SKIP:
      return state == ITestResult.FAILURE;
    case ITestResult.FAILURE:
      return false;
    default:
      throw new IllegalArgumentException("Illegal state: " + state);
    }
  }
}
