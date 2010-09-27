package org.testng.eclipse.ui;

/*
 * A view that shows the contents of a test suite as a tree.
 */
public class SuccessTab extends AbstractHierarchyTab  {
   
  protected String getTooltipKey() {
	  return "HierarchyRunView.tab.tooltip";
  }
 
  /**
   * @see net.noco.testng.ui.TestRunTab#getName()
   */
  public String getName() {
    return getResourceString("HierarchyRunView.tab.title"); //$NON-NLS-1$
  }

  protected String getSelectedTestKey(){
	  return "[TestHierarchyTab.setSelectedTest]";
  }
 
  
}
