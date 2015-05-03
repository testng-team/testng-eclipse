package org.testng.eclipse.launch.components;

import org.testng.eclipse.util.JDTUtil;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;


public class SelectionTableComposite {
  private static final int DEFAULT_WIDTH = 50;
  protected CheckboxTableViewer m_viewer;
  protected AbstractSelectionTableProvider m_labelProvider;
  protected IStructuredContentProvider m_contentProvider;
  protected Object m_content;
  protected TableColumn[] m_columns;
  protected boolean[] m_packableColumns;
  
  public SelectionTableComposite(final Composite parent, 
                                 final String[] colNames,
                                 final boolean[] packable,
                                 final AbstractSelectionTableProvider tableProvider) {
    Table table = new Table(parent, SWT.CHECK | SWT.HIDE_SELECTION);
    table.setHeaderVisible(true);
    table.setRedraw(false);
    
    m_packableColumns = packable;
    m_columns = new TableColumn[colNames.length];
    for(int i = 0; i < colNames.length; i++) {
      m_columns[i] = new TableColumn(table, SWT.LEFT);
      m_columns[i].setResizable(true);
      m_columns[i].setText(colNames[i]);
    }
    table.setRedraw(true);
    m_viewer = new CheckboxTableViewer(table);

    m_labelProvider = tableProvider;
    m_contentProvider = new SelectionTableContentProvider();
    
    m_viewer.setLabelProvider(m_labelProvider);
    m_viewer.setContentProvider(m_contentProvider);
  }
  
  public Table getTable() {
    return m_viewer.getTable();
  }
  
  public void setInput(Object input) {
    m_content = input;
    try {
      m_viewer.getTable().setRedraw(false);
      m_viewer.setInput(input);
      m_viewer.setAllChecked(true);
      int maxWidth = m_viewer.getTable().getSize().x;
      int nonPackable = 0;
      for(int i = 0; i < m_columns.length; i++) {
        if(!m_packableColumns[i]) {
          nonPackable++;
        }
      }
      int colWidth = nonPackable != 0 ? maxWidth/nonPackable : DEFAULT_WIDTH;
      for(int i = 0; i < m_columns.length; i++) {
        if(m_packableColumns[i]) {
          m_columns[i].pack();
        }
        else {
          m_columns[i].setWidth(colWidth);
        }
      }
    } 
    finally {
      m_viewer.getTable().setRedraw(true);
    }
  }
  
  public Object[] getCheckedElements() {
    return m_viewer.getCheckedElements();
  }
  
  public static abstract class AbstractSelectionTableProvider extends LabelProvider 
  implements ITableLabelProvider {
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    protected String getResourceType(IResource resource) {
      return JDTUtil.getResourceType(resource);
    }
  }
  

  public static class SelectionTableContentProvider implements IStructuredContentProvider {
    public Object[] getElements(Object inputElement) {
      IResource[] rows = new IResource[0];
      
      if(inputElement instanceof IStructuredSelection) {
        IStructuredSelection elements = (IStructuredSelection) inputElement;
        rows = new IResource[elements.size()];
        int i = 0;
        for(Iterator it = elements.iterator(); it.hasNext(); ) {
          rows[i++] = (IResource) it.next();
        }
      }
      
      return rows;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  }
}
