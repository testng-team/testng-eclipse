package org.testng.eclipse.pde.runtime;

import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.PlatformUI;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */
public class UITestApplication extends NonUIThreadTestApplication {

	private static final String DEFAULT_APP_3_0 = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.junit.runtime.NonUIThreadTestApplication#getDefaultApplicationId()
	 */
	protected String getDefaultApplicationId() {
		// In 3.0, the default is the "org.eclipse.ui.ide.worbench" application.
		return DEFAULT_APP_3_0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.junit.runtime.NonUIThreadTestApplication#runApp(java.lang.Object, org.eclipse.equinox.app.IApplicationContext, java.lang.String[])
	 */
	protected Object runApp(Object app, IApplicationContext context, String[] args) throws Exception {
		// create UI test harness
		fTestHarness = new PlatformUITestHarness(PlatformUI.getTestableObject(), false);

		// continue application launch
		return super.runApp(app, context, args);
	}
}
