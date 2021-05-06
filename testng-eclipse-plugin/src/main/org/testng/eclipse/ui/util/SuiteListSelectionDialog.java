package org.testng.eclipse.ui.util;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.core.text.StringMatcher;
import org.eclipse.ui.model.WorkbenchLabelProvider;


/**
 * This class/interface 
 */
public class SuiteListSelectionDialog extends SelectionDialog {
    
  private static final String DIALOG_SETTINGS_SECTION = "ResourceListSelectionDialogSettings"; //$NON-NLS-1$
  
  Table resourceNames;

  Table folderNames;

  int typeMask;

  private static Collator collator = Collator.getInstance();

  StringMatcher stringMatcher;

  ResourceDescriptor[] descriptors;

  int descriptorsSize;

  WorkbenchLabelProvider labelProvider = new WorkbenchLabelProvider();
  
  boolean okEnabled = false;

  static class ResourceDescriptor implements Comparable {
      String label;

      ArrayList resources = new ArrayList();

      boolean resourcesSorted = true;

      public int compareTo(Object o) {
          return collator.compare(label, ((ResourceDescriptor) o).label);
      }
  }

  /**
   * Creates a new instance of the class.
   * 
   * @param parentShell shell to parent the dialog on
   * @param resources resources to display in the dialog
   */
  public SuiteListSelectionDialog(Shell parentShell, IResource[] resources) {
      super(parentShell);
      setShellStyle(getShellStyle() | SWT.RESIZE);
      initDescriptors(resources);
  }

  
  /**
   * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
   */
  protected void cancelPressed() {
      setResult(null);
      super.cancelPressed();
  }

  /**
   * @see org.eclipse.jface.window.Window#close()
   */
  public boolean close() {
      boolean result = super.close();
      labelProvider.dispose();
      return result;
  }

  /**
   * @see org.eclipse.jface.window.Window#create()
   */
  public void create() {
      super.create();
      getButton(IDialogConstants.OK_ID).setEnabled(okEnabled);
  }

  /**
   * Creates the contents of this dialog, initializes the
   * listener and the update thread.
   * 
   * @param parent parent to create the dialog widgets in
   */
  protected Control createDialogArea(Composite parent) {
      Composite dialogArea = (Composite) super.createDialogArea(parent);
      Label l = new Label(dialogArea, SWT.NONE);
      l = new Label(dialogArea, SWT.NONE);
      l.setText(IDEWorkbenchMessages.ResourceSelectionDialog_matching);
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      l.setLayoutData(data);
      resourceNames = new Table(dialogArea, SWT.SINGLE | SWT.BORDER
              | SWT.V_SCROLL);
      data = new GridData(GridData.FILL_BOTH);
      data.heightHint = 12 * resourceNames.getItemHeight();
      resourceNames.setLayoutData(data);

      l = new Label(dialogArea, SWT.NONE);
      l.setText(IDEWorkbenchMessages.ResourceSelectionDialog_folders);
      data = new GridData(GridData.FILL_HORIZONTAL);
      l.setLayoutData(data);

      folderNames = new Table(dialogArea, SWT.SINGLE | SWT.BORDER
              | SWT.V_SCROLL | SWT.H_SCROLL);
      data = new GridData(GridData.FILL_BOTH);
      data.widthHint = 300;
      data.heightHint = 4 * folderNames.getItemHeight();
      folderNames.setLayoutData(data);

      resourceNames.addSelectionListener(new SelectionAdapter() {
          public void widgetSelected(SelectionEvent e) {
              updateFolders((ResourceDescriptor) e.item.getData());
          }

          public void widgetDefaultSelected(SelectionEvent e) {
              okPressed();
          }
      });

      folderNames.addSelectionListener(new SelectionAdapter() {
          public void widgetDefaultSelected(SelectionEvent e) {
              okPressed();
          }
      });

      applyDialogFont(dialogArea);

      return dialogArea;
  }

  /**
   * Return an image for a resource descriptor.
   * 
   * @param desc resource descriptor to return image for
   * @return an image for a resource descriptor.
   */
  private Image getImage(ResourceDescriptor desc) {
      IResource r = (IResource) desc.resources.get(0);
      return labelProvider.getImage(r);
  }
  
  private Image getParentImage(IResource resource) {
      IResource parent = resource.getParent();
      return labelProvider.getImage(parent);
  }

  private String getParentLabel(IResource resource) {
      IResource parent = resource.getParent();
      String text;
      if (parent.getType() == IResource.ROOT) {
          // Get readable name for workspace root ("Workspace"), without duplicating language-specific string here.
          text = labelProvider.getText(parent);
      } else {
          text = parent.getFullPath().makeRelative().toString();
      }
      if(text == null) {
            return ""; //$NON-NLS-1$
        }
      return text;
  }

  /**
   * Creates a ResourceDescriptor for each IResource,
   * sorts them and removes the duplicated ones.
   * 
   * @param resources resources to create resource descriptors for
   */
  private void initDescriptors(final IResource resources[]) {
      BusyIndicator.showWhile(null, new Runnable() {
          public void run() {
              descriptors = new ResourceDescriptor[resources.length];
              for (int i = 0; i < resources.length; i++) {
                  IResource r = resources[i];
                  ResourceDescriptor d = new ResourceDescriptor();
                  //TDB: Should use the label provider and compare performance.
                  d.label = r.getName();
                  d.resources.add(r);
                  descriptors[i] = d;
              }
              Arrays.sort(descriptors);
              descriptorsSize = descriptors.length;

              //Merge the resource descriptor with the same label and type.
              int index = 0;
              if (descriptorsSize < 2) {
                    return;
                }
              ResourceDescriptor current = descriptors[index];
              IResource currentResource = (IResource) current.resources.get(0);
              for (int i = 1; i < descriptorsSize; i++) {
                  ResourceDescriptor next = descriptors[i];
                  IResource nextResource = (IResource) next.resources.get(0);
                  if (nextResource.getType() == currentResource.getType()
                          && next.label.equals(current.label)) {
                      current.resources.add(nextResource);
                      // If we are merging resources with the same name, into a single descriptor,
                      // then we must mark the descriptor unsorted so that we will sort the folder
                      // names.  
                      // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=76496
                      current.resourcesSorted = false;
                  } else {
                      if (current.resources.size() > 1) {
                          current.resourcesSorted = false;
                      }
                      descriptors[index + 1] = descriptors[i];
                      index++;
                      current = descriptors[index];
                      currentResource = (IResource) current.resources.get(0);
                  }
              }
              descriptorsSize = index + 1;
          }
      });
  }

  /**
   * The user has selected a resource and the dialog is closing.
   * Set the selected resource as the dialog result.
   */
  protected void okPressed() {
      TableItem items[] = folderNames.getSelection();
      if (items.length == 1) {
          ArrayList result = new ArrayList();
          result.add(items[0].getData());
          setResult(result);
      }
      super.okPressed();
  }

  /**
   * Use this method to further filter resources.  As resources are gathered,
   * if a resource matches the current pattern string, this method will be called.
   * If this method answers false, the resource will not be included in the list
   * of matches and the resource's children will NOT be considered for matching.
   */
  protected boolean select(IResource resource) {
      return true;
  }

  /**
   * A new resource has been selected. Change the contents
   * of the folder names list.
   * 
   * @desc resource descriptor of the selected resource
   */
  private void updateFolders(final ResourceDescriptor desc) {
      BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
          public void run() {
              if (!desc.resourcesSorted) {
                  // sort the folder names
                  Collections.sort(desc.resources, new Comparator() {
                      public int compare(Object o1, Object o2) {
                          String s1 = getParentLabel((IResource) o1);
                          String s2 = getParentLabel((IResource) o2);
                          return collator.compare(s1, s2);
                      }
                  });
                  desc.resourcesSorted = true;
              }
              folderNames.removeAll();
              for (int i = 0; i < desc.resources.size(); i++) {
                  TableItem newItem = new TableItem(folderNames, SWT.NONE);
                  IResource r = (IResource) desc.resources.get(i);
                  newItem.setText(getParentLabel(r));
                  newItem.setImage(getParentImage(r));
                  newItem.setData(r);
              }
              folderNames.setSelection(0);
          }
      });
  }

  /**
   * Update the specified item with the new info from the resource 
   * descriptor.
   * Create a new table item if there is no item. 
   * 
   * @param index index of the resource descriptor
   * @param itemPos position of the existing item to update
   * @param itemCount number of items in the resources table widget
   */
  private void updateItem(int index, int itemPos, int itemCount) {
      ResourceDescriptor desc = descriptors[index];
      TableItem item;
      if (itemPos < itemCount) {
          item = resourceNames.getItem(itemPos);
          if (item.getData() != desc) {
              item.setText(desc.label);
              item.setData(desc);
              item.setImage(getImage(desc));
              if (itemPos == 0) {
                  resourceNames.setSelection(0);
                  updateFolders(desc);
              }
          }
      } else {
          item = new TableItem(resourceNames, SWT.NONE);
          item.setText(desc.label);
          item.setData(desc);
          item.setImage(getImage(desc));
          if (itemPos == 0) {
              resourceNames.setSelection(0);
              updateFolders(desc);
          }
      }
      updateOKState(true);
  }
  
  protected Control createContents(Composite parent) {
    Control ctrl= super.createContents(parent);
    
    for(int i= 0; i < descriptorsSize; i++ ) {
      updateItem(i, i, 0);
    }
    
    return ctrl;
  }
  
  /**
   * Update the enabled state of the OK button.  To be called when
   * the resource list is updated.
   * @param state the new enabled state of the button
   */
  protected void updateOKState(boolean state) {
    Button okButton = getButton(IDialogConstants.OK_ID);
    if(okButton != null && !okButton.isDisposed() && state != okEnabled) {
        okButton.setEnabled(state);
        okEnabled = state;
    }
  }
  
    
    /* (non-Javadoc)
   * @see org.eclipse.jface.window.Dialog#getDialogBoundsSettings()
   * 
   * @since 3.2
   */
    protected IDialogSettings getDialogBoundsSettings() {
      IDialogSettings settings = IDEWorkbenchPlugin.getDefault().getDialogSettings();
      IDialogSettings section = settings.getSection(DIALOG_SETTINGS_SECTION);
      if (section == null) {
          section = settings.addNewSection(DIALOG_SETTINGS_SECTION);
      } 
      return section;
    }
}
