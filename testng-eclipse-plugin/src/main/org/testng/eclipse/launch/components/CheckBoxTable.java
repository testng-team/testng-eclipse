package org.testng.eclipse.launch.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.core.text.StringMatcher;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.util.ResourceUtil;

/**
 * A Table with checkboxes to present group names.
 */
public class CheckBoxTable extends SelectionStatusDialog {
  private Text pattern;
  protected CheckboxTableViewer m_viewer;

  private String[] m_elements;
  private List<Object> m_selection = new ArrayList<>();
  
  public CheckBoxTable(Shell shell, Collection<String> elements, String titleId) {
    this(shell, elements.toArray(new String[elements.size()]), titleId);
  }
  
  public CheckBoxTable(Shell shell, String[] elements, String titleId) {
    super(shell);
    
    m_elements = elements;
    
    setSelectionResult(null);
    setShellStyle(getShellStyle() | SWT.MAX | SWT.RESIZE);
    setBlockOnOpen(true);
    setTitle(ResourceUtil.getString(titleId));
  }
  
  protected void removeSelectionElements(){
    List<Object> toRemove = new ArrayList<>();
    for (Object element : m_selection) {
      if (!m_viewer.setChecked(element.toString(), true)) {
        toRemove.add(element);
      }
    }
    m_selection.removeAll(toRemove);
  }
  
  /**
   * Handles cancel button pressed event.
   */
  @Override
  protected void cancelPressed() {
    setSelectionResult(null);
    super.cancelPressed();
  }
  
  /**
   * Creates the tree viewer.
   * 
   * @param parent the parent composite
   * @return the tree viewer
   */
  protected CheckboxTableViewer createTableViewer(Composite parent) {
    m_viewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);

    m_viewer.setContentProvider(new GroupNamesContentProvider());
    m_viewer.setLabelProvider(new GroupNameLabelProvider());

    m_viewer.setInput(m_elements);
    removeSelectionElements();
    return m_viewer;
  }

  
  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);

    // create the pattern filter area
    pattern = new Text(composite, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_CANCEL);
    GridData data = new GridData(GridData.FILL_HORIZONTAL);
    pattern.setLayoutData(data);

    // create the table area
    final CheckboxTableViewer tableViewer = createTableViewer(composite);

    final PatternFilter patternFilter = new PatternFilter();
    tableViewer.setFilters(new ViewerFilter[] {patternFilter});

    data = new GridData(GridData.FILL_BOTH);
    data.widthHint = convertWidthInCharsToPixels(60);
    data.heightHint = convertHeightInCharsToPixels(18);

    Table tableWidget = tableViewer.getTable();
    tableWidget.setLayoutData(data);
    tableWidget.setFont(composite.getFont());

    tableViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        Object element = event.getElement();
        boolean add = event.getChecked();
        
        if(add) {
          m_selection.add(element);
        } else {
          m_selection.remove(element);
        }
        
        updateOKStatus();
      }
    });

    pattern.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        patternFilter.setPattern(pattern.getText());
        tableViewer.refresh(true);
      }
    });

    pattern.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.ARROW_DOWN) {
          if (m_viewer.getTable().getItemCount() > 0) {
            m_viewer.getTable().setFocus();
          }
        }
      }
    });

    applyDialogFont(composite);
    
    return composite;
  }
  
  @Override
  public void create() {
    super.create();
    pattern.setFocus();
  }

  protected void updateOKStatus() {
    computeResult();

    if(null != getResult()) {
      updateStatus(TestNGPlugin.createStatus(IStatus.OK, "")); //$NON-NLS-1$
    } else {
      updateStatus(TestNGPlugin.createStatus(IStatus.ERROR, "")); //$NON-NLS-1$
    }
  }

  public String[] getSelectedElements() {
    return m_selection.toArray(new String[m_selection.size()]);
  }

  @Override
  protected void computeResult() {
    setSelectionResult(m_selection.toArray());
  }
  
  private static class GroupNamesContentProvider implements IStructuredContentProvider {
    
    public Object[] getElements(Object inputElement) {      
      if (inputElement instanceof String[]){
        return (String[])inputElement;
      }
      return null;
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  }
  
  private static class GroupNameLabelProvider implements ILabelProvider {
    /**
     * Returns the image
     * 
     * @param element the file
     * @return Image
     */
    public Image getImage(Object element) {
      return null;
    }

    /**
     * Returns the name of the file
     * 
     * @param element the name of the file
     * @return String
     */
    public String getText(Object element) {
      return (String) element;
    }

    /**
     * Adds a listener
     * 
     * @param listener the listener
     */
    public void addListener(ILabelProviderListener listener) {
    // Throw it away
    }

    /**
     * Disposes any created resources
     */
    public void dispose() {
    // Nothing to dispose
    }

    /**
     * Returns whether changing this property for this element affects the label
     * 
     * @param arg0 the element
     * @param arg1 the property
     */
    public boolean isLabelProperty(Object arg0, String arg1) {
      return false;
    }

    /**
     * Removes a listener
     * 
     * @param arg0 the listener
     */
    public void removeListener(ILabelProviderListener arg0) {
    // Ignore
    }
  }

  public void checkElements(String[] elements) {
    for (int i = 0; i < elements.length; i++) {
      m_selection.add(elements[i]);
    }
  }

  private class PatternFilter extends ViewerFilter {

    /**
     * The string pattern matcher used for this pattern filter.  
     */
    private StringMatcher matcher;

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      if (matcher == null)
        return true;

      return matcher.match(element.toString());
    }

    /**
     * The pattern string for which this filter should select 
     * elements in the viewer.
     * 
     * @param patternString
     */
    public void setPattern(String patternString) {
      if (patternString == null || patternString.equals("")) { //$NON-NLS-1$
        matcher = null;
      } else {
        String pattern = "*" + patternString + "*"; //$NON-NLS-1$
        matcher = new StringMatcher(pattern, true, false);
      }
    }
  }
}

