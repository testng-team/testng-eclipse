package org.testng.eclipse.ui.tree;

import org.eclipse.swt.widgets.Tree;
import org.testng.ITestResult;
import org.testng.eclipse.ui.RunInfo;

import java.text.MessageFormat;

/**
 * A node that represents the suite (the root of the tree).
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class SuiteTreeItem extends BaseTreeItem implements ITreeItem {
  private final static String FORMATTED_MESSAGE = "{0} ( {1}/{2}/{3}/{4} ) ({5} s)";
  private int m_passed;
  private int m_failed;
  private int m_skipped;
  private int m_percentage;

  public SuiteTreeItem(Tree parent, RunInfo runInfo) {
    super(parent, runInfo);
    updateView(runInfo);
  }

  public void update(RunInfo runInfo) {
    int status = runInfo.getStatus();
    if (status == ITestResult.SUCCESS) m_passed++;
    else if (status == ITestResult.FAILURE) m_failed++;
    else if (status == ITestResult.SKIP) m_skipped++;
    else m_percentage++;
    updateView(runInfo);
  }

  private void updateView(RunInfo runInfo) {
    getTreeItem().setText(MessageFormat.format(FORMATTED_MESSAGE,
          getRunInfo().getSuiteName(),
          new Integer(m_passed),
          new Integer(m_failed),
          new Integer(m_skipped),
          new Integer(m_percentage),
          getTime() / 1000
      )
    );
    maybeUpdateImage(runInfo);
  }

}
