/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.testng.eclipse.pde.runtime;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.testing.ITestHarness;
import org.eclipse.ui.testing.TestableObject;
import org.testng.Assert;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */ 
public class LegacyUITestApplication implements IPlatformRunnable, ITestHarness {
	
	private static final String DEFAULT_APP_3_0 = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$
	
	private TestableObject fTestableObject;
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IPlatformRunnable#run(java.lang.Object)
	 */
	public Object run(final Object args) throws Exception {
		IPlatformRunnable application = getApplication((String[])args);

		Assert.assertNotNull(application);

		fTestableObject = PlatformUI.getTestableObject();
		fTestableObject.setTestHarness(this);
		return application.run(args);
	}
	

	/*
	 * return the application to run, or null if not even the default application
	 * is found.
	 */
	private IPlatformRunnable getApplication(String[] args) throws CoreException {
		// Find the name of the application as specified by the PDE JUnit launcher.
		// If no application is specified, the 3.0 default workbench application
		// is returned.
		IExtension extension =
			Platform.getExtensionRegistry().getExtension(
				Platform.PI_RUNTIME,
				Platform.PT_APPLICATIONS,
				getApplicationToRun(args));
		
		
		Assert.assertNotNull(extension);
		
		// If the extension does not have the correct grammar, return null.
		// Otherwise, return the application object.
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements.length > 0) {
			IConfigurationElement[] runs = elements[0].getChildren("run"); //$NON-NLS-1$
			if (runs.length > 0) {
				Object runnable = runs[0].createExecutableExtension("class"); //$NON-NLS-1$
				if (runnable instanceof IPlatformRunnable)
					return (IPlatformRunnable) runnable;
			}
		}
		return null;
	}
	
	/*
	 * The -testApplication argument specifies the application to be run.
	 * If the PDE JUnit launcher did not set this argument, then return
	 * the name of the default application.
	 * In 3.0, the default is the "org.eclipse.ui.ide.worbench" application.
	 * 
	 */
	private String getApplicationToRun(String[] args) {
		IProduct product = Platform.getProduct();
		if (product != null) {
			return product.getApplication();
		}
		String application = null;
		Collection<String> argColl = Arrays.asList(args);
		for (Iterator<String> iter = argColl.iterator(); iter.hasNext();) {
			String arg = iter.next();
			if (arg.equals("-testApplication")) { //$NON-NLS-1$
				application = iter.next();
			}
		}
		
		if(application == null) {
			return DEFAULT_APP_3_0;
		}
		return application;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.testing.ITestHarness#runTests()
	 */
	public void runTests() {
		fTestableObject.testingStarting();
		fTestableObject.runTest(new Runnable() {
			public void run() {
				RemotePluginTestRunner.main(stripTestNGPluginArgs(Platform.getCommandLineArgs()));
			}
		});
		fTestableObject.testingFinished();
	}
	
	protected String[] stripTestNGPluginArgs(String[] applicationArgs) {
		List<String> argList = new LinkedList<String>(Arrays.asList(applicationArgs));
		for (Iterator<String> iter = argList.iterator(); iter.hasNext();) {
			String arg = iter.next();
			if (arg.equals("-testApplication")) { //$NON-NLS-1$
				iter.remove();
				iter.next();
				iter.remove();
			}
		}
		return argList.toArray(new String[argList.size()]);
	}
}
