package org.testng.eclipse.ui.summary;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.testng.eclipse.ui.RunInfo;

/**
 * A viewer filter that uses a string to perform its filterin.
 *
 * @author CŽdric Beust <cedric@beust.com>
 */
public class TestNameFilter extends ViewerFilter {

  private String m_searchString;

  @Override
  public boolean select(Viewer viewer, Object parentElement, Object element) {
    if (m_searchString == null || m_searchString.length() == 0) {
      return true;
    }

    RunInfo p = (RunInfo) element;
    if (p.getTestName().toLowerCase().matches(m_searchString.toLowerCase())) {
      return true;
    }

    return false;  }

  public void setFilter(String text) {
    m_searchString = ".*" + text + ".*"; 
  }

}
