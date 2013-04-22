/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.testng.eclipse.pde.runtime;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.testng.CommandLineArgs;
import org.testng.remote.RemoteArgs;
import org.testng.remote.RemoteTestNG;

import com.beust.jcommander.JCommander;

/**
 * Runs JUnit tests contained inside a plugin.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class RemotePluginTestRunner extends RemoteTestNG {

	private String fTestPluginName;
	private ClassLoader fLoaderClassLoader;
	private boolean keepRunning = false;

	class BundleClassLoader extends ClassLoader {
		private Bundle bundle;

		public BundleClassLoader(Bundle target) {
			this.bundle = target;
		}

		protected Class findClass(String name) throws ClassNotFoundException {
			return bundle.loadClass(name);
		}

		protected URL findResource(String name) {
			return bundle.getResource(name);
		}

		protected Enumeration findResources(String name) throws IOException {
			return bundle.getResources(name);
		}
	}

	/** 
	 * The main entry point. Supported arguments in addition
	 * to the ones supported by RemoteTestRunner:
	 * <pre>
	 * -testpluginname: the name of the plugin containing the tests.
	 * </pre>
	 * @see RemoteTestRunner
	 */

	public static void main(String[] args) {
		RemotePluginTestRunner testRunner = new RemotePluginTestRunner();
		testRunner.init(args);
		testRunner.run();
	}

	@Override
	public void run() {
		Thread.currentThread().setContextClassLoader(getTestClassLoader());
		try {
			Field f = RemoteTestNG.class.getDeclaredField("m_dontExit");
			f.setAccessible(true);
			f.set(this, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		super.run();
		if (keepRunning) {
			synchronized(this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					//ignore
				}
			}
		}
	}

	/**
	 * Returns the Plugin class loader of the plugin containing the test.
	 * @see RemoteTestRunner#getTestClassLoader()
	 */
	protected ClassLoader getTestClassLoader() {
		final String pluginId = fTestPluginName;
		return getClassLoader(pluginId);
	}

	public ClassLoader getClassLoader(final String pluginId) {
		Bundle bundle = Platform.getBundle(pluginId);
		if (bundle == null)
			throw new IllegalArgumentException("No Classloader found for plug-in " + pluginId); //$NON-NLS-1$
		return new BundleClassLoader(bundle);
	}

	public void init(String[] args) {
	  List<String> argList = new LinkedList<String>(Arrays.asList(args));
	  // Removes arguments it recognises
	  readPluginArgs(argList);
	  
      CommandLineArgs cla = new CommandLineArgs();
      RemoteArgs ra = new RemoteArgs();
      new JCommander(Arrays.asList(cla, ra), argList.toArray(new String[argList.size()]));
      
      configure(cla);
      setHost(cla.host);
      try {
    	  //m_serPort = ra.serPort; //static
    	  //m_port = cla.port;
    	  Field f = RemoteTestNG.class.getDeclaredField("m_serPort");
    	  f.setAccessible(true);
    	  f.set(null, ra.serPort);
    	  
    	  f = RemoteTestNG.class.getDeclaredField("m_port");
    	  f.setAccessible(true);
    	  f.set(this, cla.port);
    	  
      } catch (Exception e) {
    	  throw new RuntimeException(e);
      }
	}

	public void readPluginArgs(Collection<String> args) {
		for (Iterator<String> iter = args.iterator(); iter.hasNext();) {
			String arg = iter.next();
			if (isFlag(arg, "-testpluginname")) { //$NON-NLS-1$
				iter.remove();
				fTestPluginName = iter.next();
				iter.remove();
			}

			if (isFlag(arg, "-loaderpluginname")) { //$NON-NLS-1$
				iter.remove();
				fLoaderClassLoader = getClassLoader(iter.next());
				iter.remove();
			}
			
			if (isFlag(arg, "-application") || isFlag(arg, "-product")) {
				iter.remove();
				iter.next();
				iter.remove();
			}
			
			if (isFlag(arg, "-keeprunning")) {
				keepRunning = true;
				iter.remove();
			}
		}

		if (fTestPluginName == null)
			throw new IllegalArgumentException("Parameter -testpluginnname not specified."); //$NON-NLS-1$

		if (fLoaderClassLoader == null)
			fLoaderClassLoader = getClass().getClassLoader();
	}

	protected Class loadTestLoaderClass(String className) throws ClassNotFoundException {
		return fLoaderClassLoader.loadClass(className);
	}

	private boolean isFlag(String arg, final String wantedFlag) {
		String lowerCase = arg.toLowerCase(Locale.ENGLISH);
		return lowerCase.equals(wantedFlag);
	}
}
