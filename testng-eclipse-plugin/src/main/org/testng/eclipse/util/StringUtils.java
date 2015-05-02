package org.testng.eclipse.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

public class StringUtils {

  public static List<String> stringToList(String s) {
    List<String> result = Lists.newArrayList();
    
    if(null != s){
      String[] a = s.split("[,]+");
      for(int i = 0; i < a.length; i++) {
        if(a[i].trim().length() > 0) {
          result.add(a[i]);
        }
      }
    }
    return result;
  }

  /**
   * Splits a comma separated string into token and returns <tt>null</tt> if the string
   * is empty.
   */
  public static List<String> stringToNullList(final String s) {
    List<String> result = stringToList(s);

    return result.isEmpty() ? null : result;
  }

  public static boolean isEmptyString(String content) {
    return null == content || content.trim().length() == 0;
  }

  public static String listToString(Collection<String> l) {
    StringBuffer result = new StringBuffer();

    if(null != l) {
      Iterator<String> iter = l.iterator();
      while(iter.hasNext()) {
        String s = iter.next();
        result.append(s);
        // only append if it's not the last entry
        if(iter.hasNext()) {
          result.append(",");
        }
      }
    }

    return result.toString().trim();
  }
}
