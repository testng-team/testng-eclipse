package org.testng.eclipse.launch.components;

import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;


/**
 * Add a file system browser for the current check box table
 * 
 * @author Tim wu
 * 
 */
public class SuiteFileCheckBoxTable extends CheckBoxTable {

  private static final String FILE_SYSTEM_LABEL = "File System...";
  private static final int LABEL_ID = 11111;

  public SuiteFileCheckBoxTable(Shell shell, Collection<String> elements,
      String titleId) {
    super(shell, elements.toArray(new String[elements.size()]), titleId);
  }

  public SuiteFileCheckBoxTable(Shell shell, String[] elements, String titleId) {
    super(shell, elements, titleId);
  }

  //Override this method to add File System... button
  protected Control createButtonBar(Composite parent) {
    Font font = parent.getFont();
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginLeft = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.marginWidth = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    composite.setFont(font);
    
    addFileSystemButton(composite);
    addEmptyLabel(composite);
    addOkAndCancelButton(composite);
    
    addHelpButton(parent);
    return composite;
  }
  
  /**
   * Add the file system button
   */
  private void addFileSystemButton(Composite parent){
    Button btn = createButton(parent, LABEL_ID, FILE_SYSTEM_LABEL, false);
    btn.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        FileDialog fileselect = new FileDialog(getShell(), SWT.SINGLE);
        
        fileselect.setFilterNames(new String[] { "(*.xml or *.yaml)" });
        fileselect.setFilterExtensions(new String[] { "*.xml;*.yaml" });
        String url = fileselect.open();
        m_viewer.setInput(new String[] { url });
        m_viewer.setChecked(url, true);
        // Remove all existing selection element
        removeSelectionElements();
        checkElements(new String[] { url });
        m_viewer.refresh();
      }
    });

    btn.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
    ((GridLayout)parent.getLayout()).numColumns++;
  }
  
  /**
   * Add a empty label as a place holder
   */
  private void addEmptyLabel(Composite parent){
    Label emptyLabel = new Label(parent, 0);
    emptyLabel.setAlignment(SWT.LEFT);
    GridData statusData = new GridData(GridData.FILL_HORIZONTAL);
    emptyLabel.setFont(parent.getFont());
    emptyLabel.setLayoutData(statusData);
    ((GridLayout)parent.getLayout()).numColumns++;
  }
  
  /**
   * Add the ok & cancel button
   */
  private void addOkAndCancelButton(Composite parent){
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.horizontalSpacing = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    composite.setFont(parent.getFont());
    createButtonsForButtonBar(composite);
  }
  
  /**
   * Add the help button
   */
  private void addHelpButton(Composite parent){
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setFont(parent.getFont());
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginHeight = 0;
    layout.marginLeft = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
    layout.marginWidth = 0;
    composite.setLayout(layout);
    createHelpControl(composite);
    setHelpAvailable(true);
  }
}
