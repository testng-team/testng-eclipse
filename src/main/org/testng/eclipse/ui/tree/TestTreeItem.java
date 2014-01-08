package org.testng.eclipse.ui.tree;

import org.eclipse.swt.widgets.TreeItem;
import org.testng.eclipse.ui.RunInfo;

import java.text.MessageFormat;

/**
 * The representation in the tree of a test method.
 * 
 * @author Cedric Beust <cedric@beust.com>
 */
public class TestTreeItem extends BaseTreeItem implements ITreeItem {
  private final static String FORMATTED_MESSAGE = "{0} ( {1} s)";

  public TestTreeItem(TreeItem parent, RunInfo runInfo) {
    super(parent, runInfo);
    update(runInfo);
  }

  public void update(RunInfo runInfo) {
    getTreeItem().setText(MessageFormat.format(FORMATTED_MESSAGE,
        getRunInfo().getTestName(),
        getTime() / 1000
      )
    );

    maybeUpdateImage(runInfo);
  }
}
