/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.testng.eclipse.pde.runtime;

import org.eclipse.core.runtime.IPlatformRunnable;

public class CoreTestApplication implements IPlatformRunnable {

	/**
	 * Runs a set of tests as defined by the given command line args.
	 * This is the platform application entry point.
	 * @see IPlatformRunnable
	 */
	public Object run(Object arguments) throws Exception {
		RemotePluginTestRunner.main((String[])arguments);
		return null;
	}

}
