package org.testng.eclipse.ui;

import org.testng.eclipse.collections.Maps;

import java.util.Collection;
import java.util.Map;

public class Node<T> {
  private Map<T, Node<T>> m_children = Maps.newHashMap();
  private T m_name;

  public Node(T name) {
    m_name = name;
  }

  public T getName() {
    return m_name;
  }

  public Node<T> addChild(Node<T> child) {
    m_children.put(child.getName(), child);
    return child;
  }

  public Node<T> addChild(T child) {
    Node result = m_children.get(child);
    if (result == null) {
      result = new Node(child);
      addChild(result);
    }
    return result;
  }

  public Node<T> getChild(T name) {
    return m_children.get(name);
  }

  public Collection<Node<T>> getChildren() {
    return m_children.values();
  }

  @Override
  public String toString() {
    return "[Node:" + m_name + "]";
  }
}
