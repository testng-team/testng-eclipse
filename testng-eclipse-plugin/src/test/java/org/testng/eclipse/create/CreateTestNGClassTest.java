package org.testng.eclipse.create;

import java.util.List;

/**
 * This class is used to test that "Create a TestNG class" with all the methods
 * generates a TestNG class that compiles.
 * 
 * @author Cedric Beust <cedric@beust.com>
 */
public class CreateTestNGClassTest {
  public void foo(Integer n) {
  }
  
  public void foo() {
  }

  public void bar(List<String> l) {
  }
  
  public void bar(String[] v) {
  }

  public void f() {
  }

  public void f(String... var) {
  }
}
