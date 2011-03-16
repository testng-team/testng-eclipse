package org.testng.eclipse.convert;

import junit.framework.Assert;

public class JUnit3Test2 extends MyTestCase {
  public JUnit3Test2(String name) {
    super(name);
  }

  public void test1() {
    Assert.fail("should fail");
    assert_notification_that(1, 2);
    Assert.assertEquals(true, true);
    assertMyStuff(null);
    Assert.assertEquals("This is", "123","123");
  }

}
