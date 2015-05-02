package org.testng.eclipse.ui;

import org.eclipse.swt.graphics.Image;
import org.testng.ITestResult;

public class ImagesUtil {

  public static Image getImage(int state) {
    switch(state) {
      case ITestResult.SUCCESS:
        return Images.getImage(Images.IMG_TEST_OK);
      case ITestResult.FAILURE:
      case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
        return Images.getImage(Images.IMG_TEST_FAIL);
      case ITestResult.SKIP:
        return Images.getImage(Images.IMG_TEST_SKIP);
      default:
        throw new IllegalArgumentException("Illegal state: " + state);
    }
  }

  /**
   * @return the icon to display for a suite with the given state.
   */
  public static Image getSuiteImage(int state) {
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
