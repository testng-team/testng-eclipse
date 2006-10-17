package org.testng.eclipse.launch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.testng.eclipse.TestNGPlugin;
import org.testng.eclipse.launch.components.ITestContent;
import org.testng.eclipse.ui.util.ConfigurationHelper;
import org.testng.eclipse.ui.util.TypeParser;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.SuiteFileValidator;
import org.testng.eclipse.util.param.ParameterSolver;

/**
 * Base class for Run/Debug contextual actions. Handles TestNG tests, but no
 * suites yet. 
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public abstract class AbstractTestNGLaunchDelegate implements IEditorActionDelegate {
  private static final Integer CLASS_TYPE= new Integer(TestNGLaunchConfigurationConstants.CLASS);
  private static final Integer SUITE_TYPE= new Integer(TestNGLaunchConfigurationConstants.SUITE);
  
  
  private IAction m_action;
  private IEditorPart m_editorPart;
  
  private IProject m_project;
  private ICompilationUnit m_compilationUnit;
  private Map m_launchAttributes= new HashMap();
  private String m_actionText= "";
  
  protected abstract String getLaunchMode();
  
  protected abstract String getCommandPrefix();
  
  protected abstract String getTestShortcut();
  
  protected abstract String getSuiteShortcut();
  
  public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    m_action= action; 
    m_editorPart= targetEditor;
    update();
  }

  public void selectionChanged(IAction action, ISelection selection) {
  }

  private void update() {
    if(null == m_action || null == m_editorPart) {
      return;
    }

    m_launchAttributes.clear();

    IEditorInput editorInput= m_editorPart.getEditorInput();
  
    if(editorInput instanceof IFileEditorInput) {
      IFile file= ((IFileEditorInput) editorInput).getFile();
      m_project= file.getProject();
      m_actionText= getCommandPrefix();
      boolean isTestNGenabled= false;
      if("java".equals(file.getFileExtension())) {
        m_actionText+= " test";
      
        m_compilationUnit= JDTUtil.getJavaElement(file);
        IType mainType= null; 
        try {
          mainType= m_compilationUnit.findPrimaryType();
        }
        catch(AssertionFailedException failure) {
          TestNGPlugin.log(failure); // TESTNG-70
          return;
        }
        
        IType[] types= getTypes(m_compilationUnit); //getTypes(file);
        IType checkType= null;
        if(null != mainType) {
          checkType= mainType;
        }
        else if(null != types && types.length > 0) {
          checkType= types[0];
        }
        if(null != checkType && hasSource(checkType)) {
          long startT= System.currentTimeMillis();
          ITestContent testContent = TypeParser.parseType(checkType);
          long stopT= System.currentTimeMillis();
          String msg= "Parsing time for main type '" + checkType.getFullyQualifiedName() + "' done in " + (stopT - startT) + " ms";
//          TestNGPlugin.log(new Status(IStatus.INFO, TestNGPlugin.PLUGIN_ID, 2323, msg, null));
      
          if(testContent.isTestNGClass()) {
            if(null != types) {
              List classNames= new ArrayList();
              for(int i= 0; i < types.length; i++) {
                classNames.add(types[i].getFullyQualifiedName());
              }
              
              m_launchAttributes.put(TestNGLaunchConfigurationConstants.TYPE, CLASS_TYPE);
              m_launchAttributes.put(TestNGLaunchConfigurationConstants.CLASS_TEST_LIST,
                                     classNames);
              m_launchAttributes.put(TestNGLaunchConfigurationConstants.TESTNG_COMPLIANCE_LEVEL_ATTR,
                                     testContent.getAnnotationType());

              isTestNGenabled= true;
            }
          }
        }
      }
      else if("xml".equals(file.getFileExtension())) {
        m_actionText+= " suite";
      
        if(isSuiteDefinition(file)) {
          m_launchAttributes.put(TestNGLaunchConfigurationConstants.TYPE, SUITE_TYPE);
          m_launchAttributes.put(TestNGLaunchConfigurationConstants.SUITE_TEST_LIST,
            Utils.stringToList(file.getProjectRelativePath().toOSString()));
          isTestNGenabled= true;
        }
      }
    
      m_action.setEnabled(isTestNGenabled);
      m_action.setText(m_actionText);
    }
  }
  
  public void run(IAction action) {
    try {
      ILaunchConfigurationWorkingCopy workingCopy = ConfigurationHelper.createBasicConfiguration(
          getLaunchManager(), m_project, "TestNG context suite");
      m_launchAttributes.putAll(workingCopy.getAttributes());
      
      if(null != m_compilationUnit) {
        Map params= ParameterSolver.solveParameters(m_compilationUnit);
        if(null != params) {
          m_launchAttributes.put(TestNGLaunchConfigurationConstants.PARAMS, params);
        }
      }
      
      workingCopy.setAttributes(m_launchAttributes);
      if(null != workingCopy) {
        launchConfiguration(workingCopy.doSave(), getLaunchMode());
      }
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }
  }

  protected IType[] getTypes(ICompilationUnit compilationUnit) {
    try {
      return compilationUnit.getTypes();
    }
    catch(JavaModelException jme) {
      TestNGPlugin.log(jme);
    }
    
    return null;
  }
  
  protected IType[] getTypes(IFile file) {
    try {
      ICompilationUnit compilationUnit= JDTUtil.getJavaElement(file);
      return compilationUnit.getTypes();
    }
    catch(JavaModelException jme) {
      TestNGPlugin.log(jme);
    }
    
    return null;
  }
  
  private boolean hasSource(IType type) {
    try {
      return type.getSource() != null;
    }
    catch(JavaModelException jme) {
      TestNGPlugin.log(jme);
    }
    
    return false;
  }
  
  protected boolean isSuiteDefinition(IFile file) {
    try {
      return SuiteFileValidator.isSuiteDefinition(file.getContents());
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }
    
    return false;
  }
  
  protected void launchConfiguration(ILaunchConfiguration config, String mode) {
    if(null != config) {
      DebugUITools.launch(config, mode);
    }
  }
  
  protected ILaunchManager getLaunchManager() {
    return DebugPlugin.getDefault().getLaunchManager();
  }
}
