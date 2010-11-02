package org.testng.eclipse.ui.summary;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.testng.eclipse.ui.RunInfo;

/**
 * The sorter used for the test table.
 *
 * @author Cedric Beust <cedric@beust.com>
 */
public class TestTableSorter extends ViewerSorter {
  private static final int DESCENDING = 1;
  private int m_column;
  private int m_direction = 0; // start in ascending order
  private SummaryTab m_tab;

  public TestTableSorter(SummaryTab tab) {
    m_tab = tab;
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

  public int compare(Viewer viewer, Object e1, Object e2) {
    int result = 0;

    RunInfo r1 = (RunInfo) e1;
    RunInfo r2 = (RunInfo) e2;
    switch(m_column) {
      // Test name
      case 0 : result = r1.getTestName().compareTo(r2.getTestName()); break;

      // Time
      case 1: result = (int) (m_tab.getTestTime(r1.getTestId())
          - m_tab.getTestTime(r2.getTestId()));
          break;

      // Class count
      case 2: result = (int) (m_tab.getTestClassCount(r1.getTestId())
          - m_tab.getTestClassCount(r2.getTestId()));
          break;

      // Method count
      case 3: result = (int) (m_tab.getTestMethodCount(r1.getTestId())
          - m_tab.getTestMethodCount(r2.getTestId()));
          break;
    }

    if (m_direction == DESCENDING) {
      result = -result;
    }

    return result;
  }
}
