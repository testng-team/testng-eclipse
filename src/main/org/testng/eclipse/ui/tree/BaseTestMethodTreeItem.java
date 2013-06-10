package org.testng.eclipse.ui.tree;

import java.text.MessageFormat;

import org.eclipse.swt.widgets.TreeItem;
import org.testng.eclipse.ui.RunInfo;

/**
 * Base class for the tree nodes that represent a method.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
abstract public class BaseTestMethodTreeItem extends BaseTreeItem implements ITreeItem {

  private final static String FORMATTED_MESSAGE = "{0} {1} ({2,number,#.###} s)";

  public BaseTestMethodTreeItem(TreeItem parent, RunInfo runInfo) {
    super(parent, runInfo);
    update(runInfo);
  }

  public void update(RunInfo runInfo) {
    float time = getTime() / 1000;

    getTreeItem().setText(MessageFormat.format(FORMATTED_MESSAGE, getLabel(), "", time));
    maybeUpdateImage(runInfo);
  }

  protected abstract String getLabel();
}
