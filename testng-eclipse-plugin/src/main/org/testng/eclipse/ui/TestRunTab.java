package org.testng.eclipse.ui;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.testng.eclipse.util.ResourceUtil;

import java.util.List;

/**
 * A TestRunTab is shown as a tab in a tabbed folder.
 */
public abstract class TestRunTab {

  /**
   * Create the tab control
   * @param parent the containing tab folder
   * @param clipboard the clipboard to be used by the tab
   * @param runner the testRunnerViewPart containing the tab folder
   */
  public abstract Composite createTabControl(Composite parent, TestRunnerViewPart runner);

  /**
   * @return the id of the currently selected node in the tree view or null if no nodes
   * are selected. If the user selects an item on a tab and then switches to another tab,
   * the new tab can then try to make a best effort to keep the same item selected.
   */
  public abstract String getSelectedTestId();

  /**
   * Ask this tab to select the item with the given id, if possible. Used when the user
   * switches tabs.
   */
  public void setSelectedTest(String testId) {
  }

  /**
   * Activates the TestRunView.
   */
  public void activate() {
  }

  /**
   * Sets the focus in the TestRunView.
   */
  public void setFocus() {
  }

  /**
   * Informs that the suite is about to start.
   */
  public void aboutToStart() {
  }

  /**
   * Called by the TestRunnerViewPart whenenver a new test result is received.
   * @param expand true if the item should be expanded in the tree
   */
  public void updateTestResult(RunInfo resultInfo, boolean expand) {
  }

  /**
   * Select next test failure.
   */
  public void selectNext() {
  }

  /**
   * Select previous test failure.
   */
  public void selectPrevious() {
  }

  protected String getResourceString(String key) {
    return ResourceUtil.getString(key);
  }

  public void updateSearchFilter(String text) {
  }

  /**
   * @return the resource key to display as a tooltip for this tab.
   */
  abstract public String getTooltipKey();

  /**
   * @return the resource key to display as as the name for this tab.
   */
  abstract public String getNameKey();

  /**
   * @return the icon for this tab
   */
  public Image getImage() {
    return null;
  }

  public void restoreState(IMemento memento) {
  }

  public void saveState(IMemento memento) {
  }

  public void setOrientation(boolean horizontal) {
  }

  public void updateTestResult(List<RunInfo> results) {
  }
}
