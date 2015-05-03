package org.testng.eclipse.ui.summary;

import org.eclipse.jface.viewers.Viewer;

public class StringFilter extends AbstractFilter {

  @Override
  public boolean select(Viewer viewer, Object parentElement, Object element) {
    if (m_searchString == null || m_searchString.length() == 0) {
      return true;
    }

    String s = element.toString();

    return s.toLowerCase().matches(m_searchString.toLowerCase());
  }

}
