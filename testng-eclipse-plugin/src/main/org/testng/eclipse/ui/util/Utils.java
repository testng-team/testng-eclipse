package org.testng.eclipse.ui.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

  private static Composite createParent(Composite parent, boolean group) {
    Composite result;
    if (group) {
      result = new Group(parent, SWT.SHADOW_ETCHED_OUT);
    } else {
      result = new Composite(parent, SWT.NONE);
    }

    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    result.setLayout(layout);
    result.setLayoutData(SWTUtil.createGridData());

    return result;
  }

  /**
   * Create a line of widgets, made of:
   * - A label
   * - A text field
   * - A browse button
   * If checkKey is not null, the Control will be surrounded by a Group and enabled
   * by a check box.
   */
  public static Widgets createTextBrowseControl(Composite suppliedParent,
      String checkKey, String labelKey, 
      SelectionListener buttonListener,
      final SelectionListener checkListener,
      ModifyListener textListener, boolean enabled)
  {
    final Widgets result = new Widgets();
    Composite parent = createParent(suppliedParent, checkKey != null);

    if (checkKey != null) {
      //
      // Radio
      //
      result.radio = new Button(parent, SWT.CHECK);
      result.radio.setText(ResourceUtil.getString(checkKey));
      GridData gd = new GridData();
      gd.horizontalSpan = 3;
      result.radio.setLayoutData(gd);
      result.radio.setSelection(true);
      result.radio.addSelectionListener(new SelectionListener() {
        public void widgetDefaultSelected(SelectionEvent e) {
          if (checkListener != null) {
            checkListener.widgetDefaultSelected(e);
          }
        }

        public void widgetSelected(SelectionEvent e) {
          if (checkListener != null) {
            checkListener.widgetSelected(e);
          }
          enableControls(result, ((Button) e.getSource()).getSelection());
        }

      });
    }

    //
    // Label
    //
    Label label = new Label(parent, SWT.NULL);
    label.setText(ResourceUtil.getString(labelKey));

    //
    // Text widget
    //
    result.text = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
    result.text.setLayoutData(gd);
    if (buttonListener == null) gd.grabExcessHorizontalSpace = true;
    if (textListener != null) result.text.addModifyListener(textListener);

    //
    // Browse button
    //
    if (buttonListener != null) {
      result.button = new Button(parent, SWT.PUSH);
      result.button.setText(ResourceUtil.getString("TestNGMainTab.label.browse")); //$NON-NLS-1$
      result.button.addSelectionListener(buttonListener);
    }

    enableControls(result, enabled);
    return result;
  }

  /**
   * Create a line of widgets, made of:
   * - A label
   * - A text field
   */
  public static Widgets createStringEditorControl(Composite suppliedParent,
      String labelKey, ModifyListener textListener, boolean enabled){
    
    final Widgets result = new Widgets();
    Composite parent = createParent(suppliedParent, true);
    //
    // Label
    //
    Label label = new Label(parent, SWT.NULL);
    label.setText(ResourceUtil.getString(labelKey));

    //
    // Text widget
    //
    result.text = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
    result.text.setLayoutData(gd);
    if (textListener != null) result.text.addModifyListener(textListener);
    
    enableControls(result, enabled);
    return result;
  }

  private static void enableControls(Widgets result, boolean enabled) {
    if (result.text != null) result.text.setEnabled(enabled);
    if (result.button != null) result.button.setEnabled(enabled);
  }

  /**
   * Create a line of widgets, made of:
   * - A toggle
   * - A label
   * - A text field
   * - A browse button
   */
  public static Widgets createWidgetTriple(Composite parent,
      String labelKey,
      SelectionListener radioListener, SelectionListener buttonListener,
      ModifyListener textListener)
  {
    Widgets result = new Widgets();
    
    //
    // Radio
    //
    result.radio = new Button(parent, SWT.RADIO);
    result.radio.setText(ResourceUtil.getString(labelKey));
    result.radio.addSelectionListener(radioListener);

    //
    // Text widget
    //
    result.text = new Text(parent, SWT.SINGLE | SWT.BORDER);
    GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
    result.text.setLayoutData(gd);
    if (textListener != null) result.text.addModifyListener(textListener);

    //
    // Search button
    //
    result.button = new Button(parent, SWT.PUSH);
    result.button.setText(ResourceUtil.getString("TestNGMainTab.label.browse")); //$NON-NLS-1$
    result.button.addSelectionListener(buttonListener);
    
    // Make the button bold by default so that it gets its maximu size right
    // away
//    TestNGPlugin.bold(result.button, true);

    return result;
  }

  public static ITreeContentProvider getResourceContentProvider(final String extension) {
    return new BaseWorkbenchContentProvider() {
      @Override
      public Object[] getChildren(Object element) {
        Object[] children = super.getChildren(element);
        List<Object> elements = new ArrayList<>();
        for (Object obj : children) {
          if (obj instanceof IProject) {
            if (((IProject) obj).isOpen()) {
              elements.add(obj);
            }
          }
          else if (obj instanceof IFile) {
            if (extension.equalsIgnoreCase(((IFile) obj).getFileExtension())) {
              elements.add(obj);
            }
          }
          else {
            elements.add(obj);
          }
        }
        return elements.toArray(new Object[elements.size()]);
      }
    };
  }

  public static String selectTemplateFile(Shell shell) {
    ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(shell, 
        new WorkbenchLabelProvider(), Utils.getResourceContentProvider("xml"));
    dialog.setTitle("Select TestNG suite template file");
    dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
    String result = null;
    dialog.open();
    Object[] results = dialog.getResult();    
    if ((results != null) && (results.length > 0) && (results[0] instanceof IFile)) {
      IFile file = (IFile) results[0];
      IProject prj = file.getProject();
      IPath relativePath = file.getProjectRelativePath();
      String wsRelativePath = prj.getName() + "/" + relativePath;
      result = "${workspace_loc:" + wsRelativePath + "}";
    }
    return result;
  }

  public static String stripDoubleQuotes(String v) {
    String result = v;
    int    ind1 = v.indexOf("\"");
    int    ind2 = v.lastIndexOf("\"");
    if((ind1 != -1) && (ind2 != -1) && (ind1 < ind2)) {
      result = v.substring(ind1 + 1, ind2);
    }

    return result;
  }

  public static String[] split(final String string, final String sep) {
    return org.testng.internal.Utils.split(string, sep);
  }

  public static void ppp(String s) {
    System.out.println("[Utils] " + s);
  }

  public static String absolutePath(final String rootPath, final String sourcePath) {
    File sourceFile = null;
    if(null != sourcePath && !"".equals(sourcePath.trim())) {
      sourceFile= new File(sourcePath);
    }

    if(null != sourceFile && sourceFile.isAbsolute()) {
      return sourceFile.getAbsolutePath();
    }
    else {
      String projectPrefixPath= null; 
      int lastSegment= rootPath.lastIndexOf('/');
      return new File(rootPath, sourcePath).getAbsolutePath();
    }
  }

  /**
   * Unifies the list of files absolute paths into a single path string using the 
   * specified separator.
   * 
   * @param files List<File>
   * @param sep
   * @return
   */
  public static String toSinglePath(final List files, final String sep) {
    if(null == files || files.isEmpty()) {
      return "";
    }

    final StringBuffer buf= new StringBuffer(((File) files.get(0)).getAbsolutePath());
    for(int i= 1; i < files.size(); i++) {
      buf.append(sep).append(((File) files.get(i)).getAbsolutePath());
    }

    return buf.toString();
  }

  public static class WidgetPair {
    public Text   text;
    public Button button;
  }

  public static class Widgets extends WidgetPair {
    public Button radio;
  }

  /**
   * Collection<List<String>>
   */
  public static List<String> uniqueMergeList(Collection<List<String>> collection) {
    Set<String> uniques = new HashSet<>();
    for(List<String> l : collection) {
      uniques.addAll(l);
    }

    return new ArrayList<>(uniques);
  }

  /**
   * Create a file with the given content. Show up a confirmation dialog if the file already
   * exists.
   *
   * @return true if the file was written successfully.
   */
  public static boolean createFileWithDialog(Shell shell, IFile file,
      InputStream stream) throws CoreException {
    boolean success = false;
    NullProgressMonitor monitor = new NullProgressMonitor();
    try {
      if (file.exists()) {
        boolean overwrite = MessageDialog.openConfirm(shell, ResourceUtil
            .getString("NewTestNGClassWizard.alreadyExists.title"), //$NON-NLS-1$
            ResourceUtil.getFormattedString(
                "NewTestNGClassWizard.alreadyExists.message", file
                    .getFullPath().toString())); //$NON-NLS-1$
        if (overwrite) {
          file.setContents(stream, true, true, monitor);
          success = true;
        }
      } else {
        createResourceRecursively(file, monitor);
        file.setContents(stream, IFile.FORCE | IFile.KEEP_HISTORY, monitor);
        success = true;
        // result.create(contentStream, true, monitor);
      }
      stream.close();
    } catch (IOException e) {
    }

    return success;
  }

  protected static void createResourceRecursively(IResource resource,
      IProgressMonitor monitor) throws CoreException {
    if (resource == null || resource.exists())
      return;
    if (!resource.getParent().exists())
      createResourceRecursively(resource.getParent(), monitor);
    switch (resource.getType()) {
    case IResource.FILE:
      ((IFile) resource).create(new ByteArrayInputStream(new byte[0]), true,
          monitor);
      break;
    case IResource.FOLDER:
      ((IFolder) resource).create(IResource.NONE, true, monitor);
      break;
    case IResource.PROJECT:
      ((IProject) resource).create(monitor);
      ((IProject) resource).open(monitor);
      break;
    }
  }


}
