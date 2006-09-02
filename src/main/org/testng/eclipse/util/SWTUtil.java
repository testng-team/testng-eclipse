package org.testng.eclipse.util;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;


/**
 * Class usage XXX
 * 
 * @version $Revision$
 */
public class SWTUtil {
	private SWTUtil(){}

	public static void setButtonGridData(Button button) {
		GridData gridData= new GridData();
		button.setLayoutData(gridData);
		setButtonDimensionHint(button);
	}
	
	/**
	 * Returns a width hint for a button control.
	 */
	public static int getButtonWidthHint(Button button) {
		button.setFont(JFaceResources.getDialogFont());
		PixelConverter converter= new PixelConverter(button); // FIXME
		int widthHint= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	}
	
	public static void setButtonDimensionHint(Button button) {
		Assert.isNotNull(button);
		Object gd= button.getLayoutData();
		if (gd instanceof GridData) {
			((GridData)gd).widthHint= getButtonWidthHint(button);		 
		}
	}

	public static Display getDisplay() {
		Display display= Display.getCurrent();
		if (display == null) {
			display= Display.getDefault();
		}
		return display;
	}

	/**
	 * Returns the active workbench window
	 * 
	 * @return the active workbench window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow(IWorkbench workBench) {
		if(null == workBench) {
			return null;
		}
		
		return workBench.getActiveWorkbenchWindow();
	}		
	
	public static IWorkbenchPage getActivePage(IWorkbench workBench) {
		IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow(workBench);
		
		if(null == activeWorkbenchWindow) {
			return null;
		}
		
		return activeWorkbenchWindow.getActivePage();
	}
}
