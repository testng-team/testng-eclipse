package org.testng.eclipse.convert;

import junit.framework.Assert;

import org.testng.AssertJUnit;

public class JUnit3Test2 extends MyTestCase {
  public void test1() {
    AssertJUnit.assertEquals(true, true);
    assertMyStuff(null);
    Assert.assertEquals("This is", "123","123");
    System.out.println("Worked");
  }

}
