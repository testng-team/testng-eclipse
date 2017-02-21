package org.testng.eclipse.launch;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.testng.collections.Maps;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.launch.components.CheckBoxTable;
import org.testng.eclipse.launch.components.SuiteFileCheckBoxTable;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.util.StringUtils;
import org.testng.eclipse.util.TestSearchEngine;

/**
 * Allow the user to select one or many suite files in this launch configuration.
 * 
 * @author cbeust
 */
public class SuiteSelector2 extends MultiSelector {

  SuiteSelector2(TestNGMainTab callback, Composite comp) {
    super(callback, comp, LaunchType.SUITE, "TestNGMainTab.label.suiteTest",
        "CheckBoxTable.suites.title");
    // make it editable to be able to modify the order easier
    setTextEditable(true);
  }

  @Override
  protected Collection<String> getValues(ILaunchConfiguration configuration) {
    TestNGMainTab mainTab = getCallback();
    List<String> result = new ArrayList<>();
    try {
      IFile[] types = TestSearchEngine.findSuites(mainTab.getLaunchConfigurationDialog(),
          new Object[] {mainTab.getSelectedProject()});
      for(IFile f : types) {
        result.add(f.getProjectRelativePath().toString());
      }
    } catch (InvocationTargetException | InterruptedException e) {
      e.printStackTrace();
    }

    return result;
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    List<String> names = ConfigurationHelper.getSuites(configuration);
    setText(StringUtils.listToString(names));
  }

  @Override
  protected Map<String, List<String>> onSelect(String[] selectedValues) {
    Map<String, List<String>> result = Maps.newHashMap();
    for (String s : selectedValues) {
      result.put(s, Collections.<String>emptyList());
    }
    return result;
  }

  @Override
  protected CheckBoxTable getCheckBoxTable(Shell shell, String[] values, String titleId){
    return new SuiteFileCheckBoxTable(getCallback().getShell(), values, titleId);
  }
}