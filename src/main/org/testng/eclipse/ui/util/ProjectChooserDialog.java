package org.testng.eclipse.ui.util;

import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.ResourceUtil;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;


/**
 * Class usage XXX
 * 
 * @version $Revision$
 */
public class ProjectChooserDialog {
	public static IJavaProject getSelectedProject(Shell parentShell) {
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(
		      parentShell, 
		      new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT)
		);
		dialog.setTitle(ResourceUtil.getString("TestNGMainTab.projectdialog.title")); //$NON-NLS-1$
		dialog.setMessage(ResourceUtil.getString("TestNGMainTab.projectdialog.message")); //$NON-NLS-1$
		dialog.setElements(JDTUtil.getJavaProjects());
		
		if(Window.OK == dialog.open()) {			
			return (IJavaProject) dialog.getFirstResult();
		}			
		
		return null;
	}
}
