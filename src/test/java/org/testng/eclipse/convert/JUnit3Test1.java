package org.testng.eclipse.convert;

import junit.framework.Assert;
import junit.framework.TestCase;

public class JUnit3Test1 extends TestCase {

	public void test1() {
		Assert.assertEquals(true, true);
		System.out.println("Worked");
	}

	public void test2() {
	  fail("Not implemented");
	}
}
