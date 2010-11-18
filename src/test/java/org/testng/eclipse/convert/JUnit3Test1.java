package org.testng.eclipse.convert;


import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JUnit3Test1 extends TestCase {

  public static Test suite() {
    return new TestSuite();
  }

  public void test1() {
		Assert.assertEquals(true, true);
		System.out.println("Worked");
	}

	public void test2() {
	  fail("Not implemented");
	}

}
