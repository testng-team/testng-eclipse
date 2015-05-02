package org.testng.eclipse.run;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Make sure that failed configurations appear in the failed tab.
 *
 * @author CŽdric Beust <cedric@beust.com>
 *
 */
public class FailedConfigurationTest {

  @BeforeMethod
  public void bm() {
    throw new RuntimeException("Failed configuration");
  }

  @Test
  public void t() {
  }
}
