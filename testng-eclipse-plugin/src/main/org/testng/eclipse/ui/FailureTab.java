package org.testng.eclipse.ui;

import org.testng.ITestResult;

/*
 * A view that shows the contents of a test suite as a tree, 
 * with the additional property of showing only the failed 
 * tests.
 */
public class FailureTab extends AbstractTab  {
  
//  public FailureTab() {
//    super(true /* delay creation */);
//  }
   
  @Override
  public String getTooltipKey() {
    return "FailureRunView.tab.tooltip";
  }
 
  @Override
  public String getNameKey() {
    return "FailureRunView.tab.title"; //$NON-NLS-1$
  }

  @Override
  protected boolean acceptTestResult(RunInfo runInfo) {
    int status = runInfo.getStatus();
    return ITestResult.FAILURE == status;
  }

//  @Override
//  public void updateTestResult(RunInfo treeEntry) {
//    if (null == treeEntry) {
//      return;
//    }
//
//    if (RunInfo.RESULT_TYPE == treeEntry.getType()) {
//      if (ITestResult.SUCCESS != treeEntry.getStatus() && ITestResult.SKIP != treeEntry.getStatus()) {
//        super.updateTestResult(treeEntry);
//      }
//    }
//  }
//
//  @Override
//  protected void onPostUpdate(TreeItem ti, int state) {
//    // Horrible hack to remove all the SUCCESS nodes from our view. It's a hack because they
//    // shouldn't have been added there in the first place. Without this hack, this tab
//    // will still show green tree items for tests that have no errors.
//    if (state == ITestResult.SUCCESS) {
//      ti.dispose();
//    }
//  }

}
