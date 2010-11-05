package org.testng.eclipse.ui.summary;

import org.eclipse.jface.viewers.Viewer;

/**
 * The sorter used for the "excluded tests" table.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class StringTableSorter extends AbstractTableSorter {

  public StringTableSorter(SummaryTab tab) {
    super(tab);
  }

  @Override
  public int compare(Viewer viewer, Object e1, Object e2) {
    int result = e1.toString().compareTo(e2.toString());

    return adjustDirection(result);
  }
}
