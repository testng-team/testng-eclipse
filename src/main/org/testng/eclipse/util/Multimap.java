package org.testng.eclipse.util;

import java.util.List;
import java.util.Map;

import org.testng.collections.Lists;

public class Multimap {

  public static <K, V> void put(Map<K, List<V>> result, K key, V value) {
    List<V> l = result.get(key);
    if (l == null) {
      l = Lists.newArrayList();
      result.put(key, l);
    }
    l.add(value);
  }

}
