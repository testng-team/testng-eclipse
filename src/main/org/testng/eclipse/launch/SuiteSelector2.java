package org.testng.eclipse.launch;

import org.eclipse.core.internal.resources.File;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.testng.eclipse.collections.Lists;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.TestSearchEngine;
import org.testng.v6.Maps;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Allow the user to select one or many suite files in this launch configuration.
 * 
 * @author cbeust
 */
public class SuiteSelector2 extends MultiSelector {

  SuiteSelector2(TestNGMainTab callback, Composite comp) {
    super(callback, comp, LaunchType.GROUP, "TestNGMainTab.label.suiteTest");
//    setTextEditable(false); // allow hand entry of group names
  }

  @Override
  protected Collection<String> getValues(ILaunchConfiguration configuration) {
    TestNGMainTab mainTab = getCallback();
    List<String> result = Lists.newArrayList();
    try {
      File[] types = TestSearchEngine.findSuites(mainTab.getLaunchConfigurationDialog(),
          new Object[] {mainTab.getSelectedProject()});
      for(File f : types) {
        result.add(f.getProjectRelativePath().toString());
      }
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    return result;
//    return ConfigurationHelper.getSuites(configuration);
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    List<String> names = ConfigurationHelper.getSuites(configuration);
    setText(Utils.listToString(names));
  }

  @Override
  protected Map<String, List<String>> onSelect(String[] selectedValues) {
    Map<String, List<String>> result = Maps.newHashMap();
    for (String s : selectedValues) {
      result.put(s, Collections.<String>emptyList());
    }
    return result;
  }

//  @Override
//  protected ButtonHandler createButtonHandler() {
//    return new TestngTestSelector.ButtonHandler() {
//      public void handleButton() {
//        getCallback().handleSearchButtonSelected(LaunchType.SUITE);
//      }
//    };
//  }
}