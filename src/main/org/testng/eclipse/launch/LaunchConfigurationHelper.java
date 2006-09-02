package org.testng.eclipse.launch;

import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.util.JDTUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;


public class LaunchConfigurationHelper {
  public static IJavaProject getProject(final ILaunchConfiguration conf) throws CoreException {
    String projectName = ConfigurationHelper.getProjectName(conf);
    return JDTUtil.getJavaProject(projectName);
  }
  
  public static IResource[] findTestResources(final ILaunchConfiguration conf) throws CoreException {
    Set resources = new HashSet();

    resources.addAll(Arrays.asList(findResources(conf, 
        TestNGLaunchConfigurationConstants.DIRECTORY_TEST_LIST)));
    resources.addAll(Arrays.asList(findResources(conf,
        TestNGLaunchConfigurationConstants.CLASS_TEST_LIST)));
    resources.addAll(Arrays.asList(findResources(conf,
        TestNGLaunchConfigurationConstants.SOURCE_TEST_LIST)));
    resources.addAll(Arrays.asList(findResources(conf,
        TestNGLaunchConfigurationConstants.SUITE_TEST_LIST)));
    
    return (IResource[]) resources.toArray(new IResource[resources.size()]);
  }
  
  public static IResource[] findResources(final ILaunchConfiguration conf, 
                                   final String key) throws CoreException {
    List resources = new ArrayList();
    List containerPaths = conf.getAttribute(key, Collections.EMPTY_LIST);
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    
    for(int i = 0; i < containerPaths.size(); i++) {
      String resPath = (String) containerPaths.get(i);
      IResource res = root.findMember(resPath);
      
      if(null != res) {
        resources.add(res);
      }
    }
    
    return (IResource[]) resources.toArray(new IResource[resources.size()]);
  }
}
