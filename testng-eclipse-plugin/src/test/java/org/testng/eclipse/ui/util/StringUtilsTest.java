package org.testng.eclipse.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.eclipse.util.StringUtils;

public class StringUtilsTest {
  List<String> expectedList;

  @BeforeTest
  public void setup(){
    expectedList = new ArrayList<String>();

    expectedList.add("c:/With Space/test.xml");
    expectedList.add("c:/WithoutSpace/test.xml");
    expectedList.add("c:/test/test A/test B/test.xml");
  }

  @Test
  public void listToString(){
    String resultStringList = StringUtils.listToString(expectedList);

    Assert.assertEquals(resultStringList, 
        "c:/With Space/test.xml,c:/WithoutSpace/test.xml,c:/test/test A/test B/test.xml");
  }

  @Test
  public void stringToList(){    
    String resultStringList = StringUtils.listToString(expectedList);
    List<String> resultList = StringUtils.stringToList(resultStringList);

    Assert.assertEquals(resultList.size(), 3);
    Assert.assertEquals(resultList.get(0), expectedList.get(0));
    Assert.assertEquals(resultList.get(1), expectedList.get(1));
    Assert.assertEquals(resultList.get(2), expectedList.get(2));
  }

  @Test
  public void stringToNullListShouldReturnNullList(){
    Assert.assertEquals(StringUtils.stringToNullList(null), null);
    Assert.assertEquals(StringUtils.stringToNullList(",,,,"), null);
  }

  @Test
  public void stringToNullListShouldNotReturnNullList(){
    List<String> listResult = 
        StringUtils.stringToNullList(",,,,c:/With Space/test.xml,,c:/WithoutSpace/test.xml,,c:/test/test A/test B/test.xml,,");

    Assert.assertNotEquals(listResult, null);
    Assert.assertEquals(listResult.size(), 3);
    
    Assert.assertEquals(listResult.get(0), expectedList.get(0));
    Assert.assertEquals(listResult.get(1), expectedList.get(1));
    Assert.assertEquals(listResult.get(2), expectedList.get(2));     
  }
}
