package org.testng.eclipse.ui;

import org.testng.collections.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

public class TreeInfo {

  public Map<String, Node<String>> m_suites = Maps.newHashMap();

  public void addTest(String suiteName, String testName) {
    getOrCreateTest(suiteName, testName);
  }

  public Node<String> getOrCreateSuite(String suiteName) {
    Node<String> result = m_suites.get(suiteName);
    if (result == null) {
      result = new Node(suiteName);
      m_suites.put(suiteName, result);
    }
    return result;
  }

  public Map<String, Node<String>> getSuites() {
    return m_suites;
  }

  public Node<String> getOrCreateTest(String suiteName, String testName) {
    Node<String> suite = getOrCreateSuite(suiteName);
    return maybeCreate(suite, testName);
  }

  public Node<String> getOrCreateClass(String suiteName, String testName, String className) {
    Node<String> test = getOrCreateTest(suiteName, testName);
    return maybeCreate(test, className);
  }

  public Node<String> getOrCreateMethod(String suiteName, String testName, String className,
      String methodName) {
    Node<String> cls = getOrCreateClass(suiteName, testName, className);
    return maybeCreate(cls, methodName);
  }

  private Node<String> maybeCreate(Node<String> root, String name) {
    Node<String> n = root.getChild(name);
    return n != null ? n : root.addChild(name);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    for (Entry<String, Node<String>> e : m_suites.entrySet()) {
      Node<String> suite = e.getValue();
      System.out.println("Suite:" + e.getKey());
      displayChildren(e.getKey(), suite.getChildren(), "", result);
      for (Node<String> test : suite.getChildren()) {
        displayChildren(test.getName(), test.getChildren(), "  ", result);
        for (Node<String> cls : test.getChildren()) {
          displayChildren(cls.getName(), cls.getChildren(), "    ", result);
        }
      }
    }
    return result.toString();
  }

  private void displayChildren(String name, Collection<Node<String>> nodes, String indent,
      StringBuilder out) {
    for (Node<String> n : nodes) {
      out.append(indent + "  ").append(n.getName()).append("\n");
    }
  }
}
