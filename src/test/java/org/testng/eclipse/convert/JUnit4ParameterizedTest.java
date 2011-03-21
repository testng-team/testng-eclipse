package org.testng.eclipse.convert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

// Should be removed
@RunWith(Parameterized.class)
public class JUnit4ParameterizedTest {
  
  private int number;

  public JUnit4ParameterizedTest(int number) {
     this.number = number;
  }

  @Parameters
  public static Collection<Object[]> data() {
    Object[][] data = new Object[][] { { 1 }, { 2 }, { 3 }, { 4 } };
    return Arrays.asList(data);
  }

  @Test
//  @org.testng.annotations.Test
  public void pushTest() {
    System.out.println("Parameterized Number is : " + number);
  }

  // Should be inserted by the refactoring
//  @Factory
//  public Object[] factory() {
//    return ConversionUtils.wrapDataProvider(getClass(), data());
//  }

}
