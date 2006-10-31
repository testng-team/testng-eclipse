package org.testng.eclipse.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.Action;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.TestNGLaunchConfigurationConstants;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.param.ParameterSolver;


/**
 * A quick launcher from the TestNG viewer.
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class QuickRunAction extends Action {
  private IJavaProject m_javaProject;
  private ILaunch m_previousRun;
  private List/*<String>*/ m_className= new ArrayList();
  private List/*<String>*/ m_methodName= new ArrayList();
  private RunInfo m_runInfo;
  private String m_runMode;
  
  public QuickRunAction(IJavaProject javaProject, ILaunch prevLaunch, RunInfo runInfo, String mode) {
    m_javaProject= javaProject;
    m_previousRun= prevLaunch;
    m_runInfo= runInfo;
    m_className.add(m_runInfo.getClassName());
    m_methodName.add(m_runInfo.getMethodName());
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
  
  /**
   * TODO:
   * - solve dependencies
   */
  public void run() {
    IMethod imethod= null; 
    Map parameters= null; 
    try {
      imethod= (IMethod) JDTUtil.findElement(m_javaProject, m_runInfo); 
      parameters= ParameterSolver.solveParameters(imethod);
    }
    catch(JavaModelException jmex) {
      // FIXME: log that there was a problem finding the IMethod
    }

    if(null != imethod) {
      solveDependencies(imethod);
    }

    ILaunchConfigurationWorkingCopy workingCopy= 
      ConfigurationHelper.createBasicConfiguration(getLaunchManager(), m_javaProject.getProject(), "TestNG context suite"); //$NON-NLS-1$
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
  }
  
  private void solveDependencies(IMethod imethod) {
    Map methods = new HashMap();
    methods.put(imethod.getElementName(), imethod);
    
    JDTUtil.solveDependencies(imethod, methods);
    
    m_methodName= new ArrayList(methods.size());
    for(Iterator it= methods.values().iterator(); it.hasNext(); ) {
      IMethod m= (IMethod) it.next();
      m_methodName.add(m.getElementName());
    }    
  }
  
  protected ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }
  
  protected void launchConfiguration(ILaunchConfiguration config, String mode) {
    if(null != config) {
      DebugUITools.launch(config, mode);
    }
  }
}
