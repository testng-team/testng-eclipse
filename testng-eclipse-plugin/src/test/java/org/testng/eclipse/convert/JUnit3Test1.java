package org.testng.eclipse.convert;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JUnit3Test1 extends TestCase {
  private QueueTracker _queueTracker;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    assertEquals("ErrorRate is incorrect", 0.0, 1.0);
    assertNull(null);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void _test3Underscore() {
    fail("Should be a disabled test");
  }

  private void test3Private() {
    fail("Should be a disabled test");
  }

  public void testExecute() {
    assertEquals("CurrentEnqueueCountTotal is incorrect", 1,
        _queueTracker.getCurrentEnqueueCountTotal());
    assertEquals("CurrentQueueSize is incorrect", 1,
        _queueTracker.getCurrentQueueSize());
    assertTrue("run() should have been called", _queueTracker.isRun());
    assertEquals("CurrentQueueSize is incorrect after run()", 0,
        _queueTracker.getCurrentQueueSize());

    String DATE_FORMAT = "dd.MM.yyyy HH:mm:ss SSS";
    String DATE_STRING = "12.12.2112 12:12:12 122";
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    Date date;
    try {
      date = dateFormat.parse(DATE_STRING);
      assertEquals("12.12.2112 00:00:00 000", dateFormat.format(null));
      assertEquals(dateFormat.parse(DATE_STRING), date);
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static Test suite() {
    return new TestSuite();
  }

  public void test1() {
		Assert.assertEquals(true, true);
		System.out.println("Worked");
		assertNull(null);
		assertNotNull(new Object());
	}

	public void test2() {
	  fail("Not implemented");
	}

}
