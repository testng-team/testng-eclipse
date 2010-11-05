package org.testng.eclipse.ui.summary;

import org.eclipse.jface.viewers.ViewerSorter;

/**
 * Base class sorter for the tables displayed in the SummaryTab.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class AbstractTableSorter extends ViewerSorter {
  private static final int DESCENDING = 1;
  protected int m_column;
  private int m_direction = 0; // start in ascending order
  protected SummaryTab m_tab;

  public AbstractTableSorter(SummaryTab tab) {
    m_tab = tab;
  }

  protected int adjustDirection(int result) {
    if (m_direction == DESCENDING) {
      result = -result;
    }

    return result;
  }

  public void setColumn(int column) {
    if (column == m_column) {
      // Same column as last sort; toggle the direction
      m_direction = 1 - m_direction;
    } else {
      // New column; do an ascending sort
      m_column = column;
      m_direction = DESCENDING;
    }
  }


}
