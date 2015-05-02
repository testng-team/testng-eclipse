package org.testng.eclipse.ui.summary;

import org.eclipse.jface.viewers.ViewerFilter;

abstract public class AbstractFilter extends ViewerFilter {
  protected String m_searchString;

  public void setFilter(String text) {
    m_searchString = ".*" + text + ".*"; 
  }
}
