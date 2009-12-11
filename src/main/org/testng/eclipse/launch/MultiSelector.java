package org.testng.eclipse.launch;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.collections.Maps;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.launch.TestngTestSelector.ButtonHandler;
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

  private Map<String, List<String>> m_valueMap = Maps.newHashMap();
  private ILaunchConfiguration m_configuration;

  protected MultiSelector(TestNGMainTab callback, Composite comp, LaunchType type,
      String labelKey, String titleId) {
    super();
    init(callback, createButtonHandler(titleId), comp, type, labelKey);
    //setTextEditable(false);
  }

  public Map<String, List<String>> getValueMap() {
    return m_valueMap;
  }

  protected ButtonHandler createButtonHandler(final String titleId) {
    return new TestngTestSelector.ButtonHandler() {
      public void handleButton() {
        IJavaProject selectedProject = getCallback().getSelectedProject();
        if (selectedProject == null) {
          MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
              "No project", "Please select a project");
        } else {
          handleMultiSearchButtonSelected(titleId);
        }
      }
    };
  }

  @Override
  public abstract void initializeFrom(ILaunchConfiguration configuration);

  protected abstract Collection<String> getValues(ILaunchConfiguration configuration);

  protected abstract Map<String, List<String>> onSelect(String[] selectedValues);

  /**
  * Invoked when the Search button for a multiselect dialog is pressed.
  * @param titleId the id for the title of the dialog.
  */
  public void handleMultiSearchButtonSelected(String titleId) {
    Collection<String> values = getValues(m_configuration);

    if (values.size() > 0) {
      String[] uniqueValues = values.toArray(new String[values.size()]);
      Arrays.sort(uniqueValues);
      final CheckBoxTable cbt = new CheckBoxTable(getCallback().getShell(), uniqueValues, titleId);
      String content = getText();
      if(!Utils.isEmpty(content)) {
        List<String> s = Utils.stringToList(content);
        String[] existingValues = s.toArray(new String[s.size()]);
        cbt.checkElements(existingValues);
      }
      if(SelectionStatusDialog.CANCEL != cbt.open()) {
        String[] selectedValues = cbt.getSelectedElements();
        m_valueMap = onSelect(selectedValues);
        setText(Utils.listToString(Arrays.asList(selectedValues)));
  
      }
  
      getCallback().updateDialog();
    }
  }

}
