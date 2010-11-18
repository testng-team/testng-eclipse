package org.testng.eclipse.convert;

public class JUnit3Test2 extends MyTestCase {

  public void test1() {
    assertEquals(true, true);
    assertMyStuff(null);
    assertEquals("This is", "123","123");
    System.out.println("Worked");
  }

}
