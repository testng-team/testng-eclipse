package org.testng.eclipse.ui;

/*
 * A view that shows the contents of a test suite as a tree.
 */
public class SuccessTab extends AbstractTab  {
   
  @Override
  public String getTooltipKey() {
	  return "HierarchyRunView.tab.tooltip";
  }

  @Override
  public String getNameKey() {
    return "HierarchyRunView.tab.title"; //$NON-NLS-1$
  }
}
