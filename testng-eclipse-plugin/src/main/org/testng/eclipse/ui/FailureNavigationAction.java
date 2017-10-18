/*
 * $Id$
 * $Date$
 */
package org.testng.eclipse.ui;

import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.ResourceUtil;

import org.eclipse.jface.action.Action;


/**
 * Class usage XXX
 * 
 * @version $Revision$
 */
public class FailureNavigationAction extends Action {
  private TestRunnerViewPart fPart;

  public FailureNavigationAction(TestRunnerViewPart part) {
    super(ResourceUtil.getString("ShowNextFailureAction.label"));  //$NON-NLS-1$
    setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("dlcl16/select_next.png")); //$NON-NLS-1$
    setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/select_next.png")); //$NON-NLS-1$
    setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/select_next.png")); //$NON-NLS-1$
    setToolTipText(ResourceUtil.getString("ShowNextFailureAction.tooltip")); //$NON-NLS-1$
    fPart= part;
  }
  
  public void run() {
    fPart.selectNextFailure();
  }
}
