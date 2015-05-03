package org.testng.eclipse.convert;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JUnit4Test1 {

  @Test(expected = RuntimeException.class, timeout = 1000)
  public void f() {
    // Should preserve the static import
    assertEquals(0, 1);
    // Should convert this one
    Assert.assertArrayEquals(new int[0], new int[0]);
  }

  @Ignore
  public void ignoredTest() {
  }

  @Before
  public void before() {
  }

  @After
  public void after() {
  }

  @BeforeClass
  public void bc() {
  }

  @AfterClass
  public void ac() {
  }
}
