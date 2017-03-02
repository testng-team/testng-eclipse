package org.testng.eclipse.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Joiner;

public class StringUtils {

  public static List<String> stringToList(String s) {
    List<String> result = new ArrayList<>();
    
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
    if (l != null && !l.isEmpty()) {
      return Joiner.on(',').join(l).trim();
    }
    return "";
  }
}
