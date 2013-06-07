package org.testng.eclipse.ui.summary;

import org.eclipse.jface.viewers.Viewer;
import org.testng.eclipse.ui.RunInfo;

/**
 * A viewer filter that uses a string to perform its filterin.
 *
 * @author C�dric Beust <cedric@beust.com>
 */
public class RunInfoFilter extends AbstractFilter {

  @Override
  public boolean select(Viewer viewer, Object parentElement, Object element) {
    if (m_searchString == null || m_searchString.length() == 0) {
      return true;
    }

    RunInfo p = (RunInfo) element;

    return p.getTestName().toLowerCase().matches(m_searchString.toLowerCase());
  }

}
