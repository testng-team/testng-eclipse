package org.testng.eclipse;

import junit.framework.Assert;
import junit.framework.TestCase;

public class JTest extends TestCase {

	public void test1() {
		Assert.assertEquals(true, true);
		System.out.println("Worked");
	}

	public void test2() {
	  fail("Not implemented");
	}
}
