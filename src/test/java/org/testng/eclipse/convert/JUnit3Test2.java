package org.testng.eclipse.convert;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;

public class JUnit3Test2 extends MyTestCase {
  @Test()
  public void test1() {
    AssertJUnit.assertEquals(true, true);
    assertMyStuff(null);
    AssertJUnit.assertEquals("This is", "123","123");
    System.out.println("Worked");
  }

}
