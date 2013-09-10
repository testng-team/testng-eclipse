package org.testng.eclipse.launch.components;

import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * Add a file system browser for the current check box table
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

  protected Control createButtonBar(Composite parent) {
     super.createButtonBar(parent);
     //Add a new button at the left corner
     Composite composite = new Composite(parent, SWT.NULL);
     GridLayout layout = new GridLayout();
     layout.numColumns = 1;
     layout.marginHeight = 0;
     layout.marginLeft = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
     layout.marginWidth = 0;
     composite.setLayout(layout);
     composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
     Button btn = createButton(composite, LABEL_ID,
         FILE_SYSTEM_LABEL, false);
     btn.addSelectionListener(new SelectionAdapter() {
     public void widgetSelected(SelectionEvent e) {
       FileDialog fileselect = new FileDialog(getShell(), SWT.SINGLE);
       fileselect.setFilterExtensions(new String[] { "*.xml" });
       String url = fileselect.open();
       m_viewer.setInput(new String[] { url });
       m_viewer.setChecked(url, true);
       //Remove all existing selection element
       removeSelectionElements();
       checkElements(new String[] { url });
       m_viewer.refresh();
     }
   });

     return parent;
  }

}
