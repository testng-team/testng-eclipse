package org.testng.eclipse.convert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JUnit4Test1 {

  @Test(expected = RuntimeException.class, timeout = 1000)
  public void f() {
    // Should preserve the static import
    assertEquals(0, 1);
  }

  @Before
  public void before() {
    
  }

  @After
  public void after() {
    
  }

}
