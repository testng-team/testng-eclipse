package org.testng.eclipse.launch;


import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.collections.Maps;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants.LaunchType;
import org.testng.eclipse.launch.components.Filters;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.ui.util.TypeParser;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.TestSearchEngine;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupSelector extends MultiSelector {

  private Map<String, List<String>> m_groupMap;

  GroupSelector(TestNGMainTab callback, Composite comp) {
    super(callback, comp, LaunchType.GROUP, "TestNGMainTab.label.group");
//    setTextEditable(false); // allow hand entry of group names
  }

  @Override
  protected Set<String> getValues() {
    Map<String, List<String>> result = Maps.newHashMap();

    try {
      IJavaProject[] dependencies = new IJavaProject[0];
      IJavaProject selectedProject = getCallback().getSelectedProject();
      if (selectedProject == null) {
        MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
            "No project", "Please select a project");
        return Collections.emptySet();
      }

      try {
        String[] dependencyPrjNames = selectedProject.getRequiredProjectNames();
        if(null != dependencyPrjNames) {
          dependencies = new IJavaProject[dependencyPrjNames.length];
          for(int i = 0; i < dependencyPrjNames.length; i++) {
            dependencies[i] = JDTUtil.getJavaProject(dependencyPrjNames[i]);
          }
        }
      }
      catch(JavaModelException jmex) {
        ; // ignore for the moment
      }

      Object[] projects = new Object[1 + dependencies.length];
      projects[0] = selectedProject;
      System.arraycopy(dependencies, 0, projects, 1, dependencies.length);
      Object[] types = TestSearchEngine.findTests(getCallback().getLaunchConfigurationDialog(),
          projects, Filters.SINGLE_TEST);

      for(int i = 0; i < types.length; i++) {
        Object t = types[i];
        if(t instanceof IType) {
          IType type = (IType) t;
          ITestContent content = TypeParser.parseType(type);
          Collection<String> groupNames = content.getGroups();
          if(!groupNames.isEmpty()) {
            for (String groupName : groupNames) {
              List<String> rtypes = result.get(groupName);
              if(null == rtypes) {
                rtypes = new ArrayList<String>();
                result.put(groupName, rtypes);
              }

              rtypes.add(type.getFullyQualifiedName());
            }
          }
        }
      }
    }
    catch(InvocationTargetException e) {
      TestNGPlugin.log(e);
    }
    catch(InterruptedException e) {
      TestNGPlugin.log(e);
    }

    m_groupMap = result;
    return result.keySet();
  }

  @Override
  public Map<String, List<String>> onSelect(String[] selectedValues) {
    Map<String, List<String>> result = Maps.newHashMap();
    for(int i = 0; i < selectedValues.length; i++) {
      result.put(selectedValues[i], m_groupMap.get(selectedValues[i]));
    }

    return result;
  }
}
