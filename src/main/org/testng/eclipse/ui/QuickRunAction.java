package org.testng.eclipse.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.Action;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.LaunchUtil;
import org.testng.eclipse.util.ResourceUtil;


/**
 * A quick launcher from the TestNG viewer.
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class QuickRunAction extends Action {
  private IJavaProject m_javaProject;
  private ILaunch m_previousRun;
  private RunInfo m_runInfo;
  private String m_runMode;
  
  public QuickRunAction(IJavaProject javaProject, ILaunch prevLaunch, RunInfo runInfo, String mode) {
    m_javaProject= javaProject;
    m_previousRun= prevLaunch;
    m_runInfo= runInfo;
    m_runMode= mode;
    
    initUI();
  }

  private void initUI() {
    if(ILaunchManager.RUN_MODE.equals(m_runMode)) {
      setText(ResourceUtil.getString("QuickRunAction.run.action.label")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("QuickRunAction.run.action.tooltip")); //$NON-NLS-1$
      setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("dlcl16/relaunch.gif")); //$NON-NLS-1$
      setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
      setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
    }
    else {
      setText(ResourceUtil.getString("QuickRunAction.debug.action.label")); //$NON-NLS-1$
      setToolTipText(ResourceUtil.getString("QuickRunAction.debug.action.tooltip")); //$NON-NLS-1$
      setDisabledImageDescriptor(TestNGPlugin.getImageDescriptor("dlcl16/debug.gif")); //$NON-NLS-1$
      setHoverImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/debug.gif")); //$NON-NLS-1$
      setImageDescriptor(TestNGPlugin.getImageDescriptor("elcl16/debug.gif")); //$NON-NLS-1$
    }
  }
  
  public void run() {
    IMethod imethod= null;  
    try {
      imethod= (IMethod) JDTUtil.findElement(m_javaProject, m_runInfo); 
    }
    catch(JavaModelException jmex) {
      TestNGPlugin.log(new Status(IStatus.ERROR, TestNGPlugin.PLUGIN_ID, 3333, 
          "Cannot find method " + m_runInfo.getMethodDisplay() + " in class " + m_runInfo.getClassName(), //$NON-NLS-1$ $NON-NLS-2$
          jmex));
    }

    if(null == imethod) return;

    LaunchUtil.launchMethodConfiguration(m_javaProject, 
        imethod, 
        ConfigurationHelper.getComplianceLevel(m_javaProject, m_previousRun.getLaunchConfiguration()), 
        m_runMode);
  }
  
/*  public void run() {
    IMethod imethod= null; 
    Map parameters= null; 
    try {
      imethod= (IMethod) JDTUtil.findElement(m_javaProject, m_runInfo); 
      parameters= ParameterSolver.solveParameters(imethod);
    }
    catch(JavaModelException jmex) {
      TestNGPlugin.log(new Status(IStatus.ERROR, TestNGPlugin.PLUGIN_ID, 3333, 
          "Cannot find method " + m_runInfo.getMethodDisplay() + " in class " + m_runInfo.getClassName(), //$NON-NLS-1$ $NON-NLS-2$
          jmex));
    }

    if(null == imethod) return;

    solveDependencies(imethod);

    final String confName= imethod.getDeclaringType().getElementName() + "." + imethod.getElementName();
    ILaunchConfigurationWorkingCopy workingCopy= 
      ConfigurationHelper.createBasicConfiguration(getLaunchManager(), m_javaProject.getProject(), confName);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST, m_className);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.METHOD_TEST_LIST, m_methodName);
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TYPE, TestNGLaunchConfigurationConstants.METHOD);
    if(null != parameters) {
      workingCopy.setAttribute(TestNGLaunchConfigurationConstants.PARAMS, parameters);
    }

    String complianceLevel= ConfigurationHelper.getComplianceLevel(m_javaProject, m_previousRun.getLaunchConfiguration());
    workingCopy.setAttribute(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR,
                             complianceLevel);

    if(null != workingCopy) {
      try {
        launchConfiguration(workingCopy.doSave(), m_runMode);
      }
      catch(CoreException ce) {
        TestNGPlugin.log(ce);
      }
    }
  }*/
  
/*  private void solveDependencies(IMethod imethod) {
    Map methods = new HashMap();
    methods.put(imethod.getElementName(), imethod);
    
    JDTUtil.solveDependencies(imethod, methods);
    
    m_methodName= new ArrayList(methods.size());
    for(Iterator it= methods.values().iterator(); it.hasNext(); ) {
      IMethod m= (IMethod) it.next();
      m_methodName.add(m.getElementName());
    }    
  }*/
  
/*  protected ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }*/
  
/*  protected void launchConfiguration(ILaunchConfiguration config, String mode) {
    if(null != config) {
      DebugUITools.launch(config, mode);
    }
  }*/
}
