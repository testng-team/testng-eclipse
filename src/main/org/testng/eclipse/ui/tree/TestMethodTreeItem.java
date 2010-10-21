package org.testng.eclipse.ui.tree;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TreeItem;
import org.testng.ITestResult;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.ui.Images;
import org.testng.eclipse.ui.RunInfo;

import java.text.MessageFormat;

/**
 * A tree node representing a test method.
 * 
 * @author Cedric Beust <cedric@beust.com>
 */
public class TestMethodTreeItem extends BaseTreeItem implements ITreeItem {
  private final static String FORMATTED_MESSAGE = "{0} {1} ({2} s)";

  public TestMethodTreeItem(TreeItem parent, RunInfo runInfo) {
    super(parent, runInfo);
    update(runInfo);
  }

  public void update(RunInfo runInfo) {
    String label = runInfo.getMethodName() + runInfo.getParametersDisplay();
    String description = TestNGPlugin.isEmtpy(runInfo.getTestDescription())
        ? ""
        : " [" + runInfo.getTestDescription() + "]";
    float time = ((float) runInfo.getTime() / 1000);

    getTreeItem().setText(MessageFormat.format(FORMATTED_MESSAGE, label, description, time));
    getTreeItem().setImage(getImage(runInfo.getStatus()));
  }

  private Image getImage(int state) {
    switch(state) {
      case ITestResult.SUCCESS:
        return Images.getImage(Images.IMG_TEST_OK);
      case ITestResult.FAILURE:
      case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
        return Images.getImage(Images.IMG_TEST_FAIL);
      case ITestResult.SKIP:
        return Images.getImage(Images.IMG_TEST_SKIP);
      default:
        throw new IllegalArgumentException("Illegal state: state");
    }
  }

}
