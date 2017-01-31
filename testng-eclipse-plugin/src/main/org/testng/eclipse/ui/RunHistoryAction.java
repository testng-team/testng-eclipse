package org.testng.eclipse.ui;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.testng.eclipse.TestNGPlugin;

/**
 * Action that displays the test run history. It can:
 * <ul>
 * <li>open up a menu showing all the test runs with an icon displaying their
 * result</li>
 * <li>show the test run for which results are displayed</li>
 * <li>allow to select one test run to display its results</li>
 * </ul>
 */
public class RunHistoryAction extends Action implements IMenuCreator {

  private TestRunnerViewPart testRunnerViewPart;
  private Menu fMenu;

  private LinkedList<SuiteRunInfo> runs = new LinkedList<SuiteRunInfo>();
  private SuiteRunInfo currentlyDisplayedRun;

  public RunHistoryAction(TestRunnerViewPart testRunnerViewPart) {
    this.testRunnerViewPart = testRunnerViewPart;

    setToolTipText("Test Run History...");
    setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/history_list.gif")); //$NON-NLS-1$
    setMenuCreator(this);
  }

  public void dispose() {
    if (fMenu != null) {
      fMenu.dispose();
      fMenu = null;
    }
  }

  public Menu getMenu(Menu parent) {
    return null;
  }

  public Menu getMenu(Control parent) {
    if (fMenu != null) {
      fMenu.dispose();
    }

    fMenu = new Menu(parent);
    for (final SuiteRunInfo run : runs) {
      Action filterAction = new Action(getText(run)) {
        @Override
        public void run() {
          currentlyDisplayedRun = run;
          testRunnerViewPart.reset(run);
        }
      };

      addActionToMenu(fMenu, filterAction, ImagesUtil.getImage(run.getStatus()));
    }

    new MenuItem(fMenu, SWT.SEPARATOR);

    addActionToMenu(fMenu, new Action("Clear History") {
      @Override
      public void run() {
        runs.clear();
        currentlyDisplayedRun = null;
        testRunnerViewPart.reset();
      }
    }, null);

    return fMenu;
  }

  private String getText(SuiteRunInfo run) {
    String prefix = "";
    if (this.currentlyDisplayedRun == run) {
      prefix = "\u26ab "; // put a bullet in front
    }
    final Date date = new Date(run.getStartTime());
    final String dateStr = DateFormat.getDateTimeInstance().format(date);
    final ILaunch launch = run.getLaunch();
    if (launch != null) {
      return prefix + launch.getLaunchConfiguration().getName() + " ("
          + dateStr + ")";
    }
    return prefix + "(" + dateStr + ")";
  }

  protected void addActionToMenu(Menu parent, Action action, Image image) {
    final ActionContributionItem item = new ActionContributionItem(action);
    item.fill(parent, -1);
    final MenuItem menuItem = (MenuItem) item.getWidget();
    if (image != null) {
      menuItem.setImage(image);
    }
  }

  public void add(SuiteRunInfo run) {
    this.runs.addFirst(run);
    // limit the number of displayed runs
    if (this.runs.size() > 20) {
      this.runs.removeLast();
    }
    this.currentlyDisplayedRun = run;
  }
}
