package org.testng.eclipse.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.collections.Maps;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.launch.components.CheckBoxTable;
import org.testng.eclipse.launch.components.Filters;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.TypeParser;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.TestSearchEngine;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for launch configurations that allow the user to select one or more
 * strings from a dialog.
 *
 * @author cbeust
 */
abstract public class MultiSelector extends TestngTestSelector {

//  private Map<String, List<String>> m_groupMap = Maps.newHashMap();

  private Map<String, List<String>> m_valueMap = Maps.newHashMap();

  protected MultiSelector(TestNGMainTab callback, Composite comp, LaunchType type,
      String labelKey) {
    super();
    init(callback, createButtonHandler(), comp, type, labelKey);
    //setTextEditable(false); // allow hand entry of group names
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
//    List<String> groupNames = ConfigurationHelper.getGroups(configuration);
//    setText(Utils.listToString(groupNames));
//    m_groupMap.clear();
//    List<String> groupClassNames = ConfigurationHelper.getGroupClasses(configuration);
//    groupNames = ConfigurationHelper.getGroups(configuration);
//    if(null != groupNames) {
//      for(int i = 0; i < groupNames.size(); i++) {
//        m_groupMap.put(groupNames.get(i), groupClassNames);
//      }
//    }
  }

  public Map<String, List<String>> getValueMap() {
    return m_valueMap;
  }

  private ButtonHandler createButtonHandler() {
    return new TestngTestSelector.ButtonHandler() {
      public void handleButton() {
        handleGroupSearchButtonSelected();
      }
    };
  }

  protected abstract Set<String> getValues();

  protected abstract Map<String, List<String>> onSelect(String[] selectedValues);

  /**
  * Invoked when the Search button for groups is pressed.
  */
  public void handleGroupSearchButtonSelected() {

    Set<String> groups = getValues();

    if (groups.size() > 0) {
      String[] uniqueGroups = groups.toArray(new String[groups.size()]);
      Arrays.sort(uniqueGroups);
      final CheckBoxTable cbt = new CheckBoxTable(getCallback().getShell(), uniqueGroups);
      String content = getText();
      if(!Utils.isEmpty(content)) {
        List<String> s = Utils.stringToList(content);
        String[] existingGroups = s.toArray(new String[s.size()]);
        cbt.checkElements(existingGroups);
      }
      if(SelectionStatusDialog.CANCEL != cbt.open()) {
        String[] selectedGroups = cbt.getSelectedElements();
        m_valueMap = onSelect(selectedGroups);
        setText(Utils.listToString(Arrays.asList(selectedGroups)));
  
      }
  
      getCallback().updateDialog();
    }
  }

}
