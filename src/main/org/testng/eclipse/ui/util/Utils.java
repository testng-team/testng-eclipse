package org.testng.eclipse.ui.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.internal.annotations.JDK14TagFactory;

public class Utils {
  public static final String TEST_ANNOTATION = "@" + JDK14TagFactory.TEST;
  public static final String CONFIGURATION_ANNOTATION = "@" + JDK14TagFactory.CONFIGURATION; 
  public static final String FACTORY_ANNOTATION = "@" + JDK14TagFactory.FACTORY;
  public static final String EXPECTED_EXCEPTIONS_ANNOTATION = "@" + JDK14TagFactory.EXPECTED_EXCEPTIONS; 
  
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
    result.text.addModifyListener(textListener);

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

  /**
   * TODO:  Needs to use the constants defined in TestNG.
   */
  public static boolean isTestNGTag(String tagName) {
    return TEST_ANNOTATION.equals(tagName)
      || CONFIGURATION_ANNOTATION.equals(tagName)
      || EXPECTED_EXCEPTIONS_ANNOTATION.equals(tagName)
      || FACTORY_ANNOTATION.equals(tagName);
  }

  public static String[] split(final String string, final String sep) {
    return org.testng.internal.Utils.split(string, sep);
  }
  
  /**
   *
   * @param s
   * @return
   */
  public static List stringToList(String s) {
    String[] a = s.split("[ ]+");
    List     result = new ArrayList();
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
  public static List stringToNullList(final String s) {
    List result = stringToList(s);
    
    return result.isEmpty() ? null : result;
  }
  
  public static void ppp(String s) {
    System.out.println("[Utils] " + s);
  }

  public static String listToString(List l) {
    StringBuffer result = new StringBuffer();

    if(null != l) {
      for(Iterator it = l.iterator(); it.hasNext();) {
        result.append(it.next().toString()).append(" ");
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
