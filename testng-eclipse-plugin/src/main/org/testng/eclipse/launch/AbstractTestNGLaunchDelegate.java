package org.testng.eclipse.launch;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
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
import org.testng.eclipse.ui.util.TypeParser;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.LaunchUtil;
import org.testng.eclipse.util.SuiteFileValidator;

/**
 * Base class for Run/Debug contextual actions. Handles TestNG tests, but no
 * suites yet. 
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public abstract class AbstractTestNGLaunchDelegate implements IEditorActionDelegate {
  private IAction m_action;
  private IEditorPart m_editorPart;
  
  private IProject m_project;
  private ICompilationUnit m_compilationUnit;
  private Map<String, Object> m_launchAttributes= new HashMap<>();
  private String m_configName;
  
  private String m_actionText= "";
  private boolean m_enabled= false;
  
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

    m_enabled= false;
    m_launchAttributes.clear();

    IEditorInput editorInput= m_editorPart.getEditorInput();
  
    if(editorInput instanceof IFileEditorInput) {
      IFile file= ((IFileEditorInput) editorInput).getFile();
      m_project= file.getProject();
      m_actionText= getCommandPrefix();
      
      if("java".equals(file.getFileExtension())) {
        m_actionText+= " test";
        m_compilationUnit= JDTUtil.getJavaElement(file);

        IType[] types= getTypes(m_compilationUnit);
        IType mainType= getMainType(m_compilationUnit, types);
        
        if(null == types || null == mainType || !hasSource(mainType)) {
          return;
        }

        ITestContent testContent = TypeParser.parseType(mainType);

        if(testContent.isTestNGClass()) {
          m_enabled= true;
          m_configName= mainType.getElementName();
          
          m_launchAttributes= LaunchUtil.createClassLaunchConfigurationMap(mainType, types, testContent.getAnnotationType());
        }        
      }
//      else if("xml".equals(file.getFileExtension())) {
//        m_actionText+= " suite";
//      
//        if(isSuiteDefinition(file)) {
//          m_launchAttributes.put(TestNGLaunchConfigurationConstants.TYPE, SUITE_TYPE);
//          m_launchAttributes.put(TestNGLaunchConfigurationConstants.SUITE_TEST_LIST,
//            Utils.stringToList(file.getProjectRelativePath().toOSString()));
//          m_configName= file.getProjectRelativePath().toString().replace('/', '.');
//          m_isSuite= true;
//          isTestNGenabled= true;
//        }
//      }
    
      m_action.setEnabled(m_enabled);
      m_action.setText(m_actionText);
    }
  }
  
  /**
   * @param compilationUnit
   * @param types
   * @return
   */
  private IType getMainType(ICompilationUnit compilationUnit, IType[] types) {
    IType mainType= null;
    try {
      mainType= compilationUnit.findPrimaryType();
    }
    catch(AssertionFailedException failure) {
      ; //ignore
    }
    if(null == mainType && null != types && types.length > 0) {
      mainType= types[0];
    }
    
    return mainType;
  }

  public void run(IAction action) {
    if(!m_enabled) return;
    
    LaunchUtil.launchMapConfiguration(m_project, m_configName, m_launchAttributes, m_compilationUnit, getLaunchMode());
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
  
/*  protected IType[] getTypes(IFile file) {
    try {
      ICompilationUnit compilationUnit= JDTUtil.getJavaElement(file);
      return compilationUnit.getTypes();
    }
    catch(JavaModelException jme) {
      TestNGPlugin.log(jme);
    }
    
    return null;
  }*/
  
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
      return SuiteFileValidator.isSuiteDefinition(file);
    }
    catch(CoreException ce) {
      TestNGPlugin.log(ce);
    }
    
    return false;
  }  
}
