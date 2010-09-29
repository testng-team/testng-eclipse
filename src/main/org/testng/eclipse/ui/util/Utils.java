package org.testng.eclipse.ui.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.testng.eclipse.collections.Lists;
import org.testng.eclipse.util.ResourceUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Utils {
  
  /**
   * Create a line of widgets, made of:
   * - A label
   * - A text field
   * - A browse button
   * If checkKey is not null, the Control will be surrounded by a Group and enabled
   * by a check box.
   */
  public static Widgets createTextBrowseControl(Composite parentComposite,
      String checkKey, String labelKey, 
      SelectionListener buttonListener,
      final SelectionListener checkListener,
      ModifyListener textListener, boolean enabled)
  {
    Composite p = new Composite(parentComposite, SWT.NONE);
    {
      parentComposite.setLayout(new GridLayout(1, false));
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      parentComposite.setLayoutData(gd);
//      p.setBackground(new Color(p.getDisplay(), 80, 80, 80));
      p.setLayout(new GridLayout(3, false));
      p.setLayoutData(gd);
    }

    final Widgets result = new Widgets();

    Composite parent = p;
    if (checkKey != null) {

      //
      // Radio
      //
      result.radio = new Button(parent, SWT.CHECK);
      result.radio.setText(ResourceUtil.getString(checkKey));
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

      //
      // Group
      //
      {
        final Group g = new Group(parent, SWT.SHADOW_ETCHED_OUT);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        g.setLayoutData(gd);
        
        GridLayout layout = new GridLayout();
        g.setLayout(layout);
        layout.numColumns = 3;
        parent = g;
      }
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
  
  public static List<String> stringToList(String s) {
    String[] a = s.split("[ ]+");
    List<String> result = Lists.newArrayList();
    for(int i = 0; i < a.length; i++) {
      if(a[i].trim().length() > 0) {
        result.add(a[i]);
      }
    }

    return result;
  }

  /**
   * Splits a space separated string into token and returns <tt>null</tt> if the string
   * is empty.
   */
  public static List<String> stringToNullList(final String s) {
    List<String> result = stringToList(s);
    
    return result.isEmpty() ? null : result;
  }
  
  public static void ppp(String s) {
    System.out.println("[Utils] " + s);
  }

  public static String listToString(Collection<String> l) {
    StringBuffer result = new StringBuffer();

    if(null != l) {
      for (String s : l) {
        result.append(s).append(" ");
      }
    }

    return result.toString().trim();
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

  public static boolean isEmpty(String content) {
    return null == content || content.trim().length() == 0;
  }

  /**
   * Collection<List<String>>
   */
  public static List uniqueMergeList(Collection collection) {
    Set uniques = new HashSet();
    for(Iterator it = collection.iterator(); it.hasNext(); ) {
      uniques.addAll((List) it.next());
    }
    
    return new ArrayList(uniques);
  }
}
