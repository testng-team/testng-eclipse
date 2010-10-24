package org.testng.eclipse.ui.tree;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TreeItem;
import org.testng.ITestResult;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.ui.Images;
import org.testng.eclipse.ui.RunInfo;

/**
 * A tree node representing a test method.
 * 
 * @author cbeust
 */
public class TestMethodTreeItem extends BaseTreeItem implements ITreeItem {
  public TestMethodTreeItem(TreeItem parent, RunInfo runInfo) {
    super(parent, runInfo);
    update(runInfo);
  }

  public void update(RunInfo runInfo) {
    long time = runInfo.getTime();
    String description = TestNGPlugin.isEmtpy(runInfo.getTestDescription())
        ? ""
        : " [" + runInfo.getTestDescription() + "]";
    String label = runInfo.getMethodName()
        + runInfo.getParametersDisplay()
        + description
        + " (" + ((float) time / 1000) + " s) "
        ;

    getTreeItem().setText(label);
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
