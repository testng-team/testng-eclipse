/*
 * Created on Nov 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.testng.eclipse.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.testng.IReporter;
import org.testng.TestListenerAdapter;

/**
 * @author jocraig
 * 
 * TODO To change the template for this generated type comment go to Window - Preferences - Java -
 * Code Style - Code Templates
 */
public class ListenerContributorUtil
{
    /**
     * 
     * @param validPluginIDs
     * @return
     * @throws Exception 
     */
    public static List findReporterContributors()
    {
        List reporterListenerContributors = new ArrayList();

		// Find all of the plug-ins that create extensions for the
		// profilerContributor extension point.
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("org.testng.eclipse.reporterListenerContributorSchema");
		IExtension extensions[] = extensionPoint.getExtensions();

		reporterListenerContributors = new ArrayList();
		for (int i = 0; i < extensions.length; i++)
		{
		    Object obj = null;
		    Class c = null;
		    IConfigurationElement elements[] = extensions[i].getConfigurationElements();
		    for (int j = 0; j < elements.length; j++)
		    {
		        String className = elements[j].getAttribute("class");

		        // Find all of the ones that implement IRPCEndpointContributor
		        try
		        {
		            c = Platform.getBundle(extensions[i].getNamespaceIdentifier()).loadClass(className);
		            obj = c.newInstance();
		            // Save them so later we can iterate through them
		            if ( obj instanceof IReporter )
		            {
		            	reporterListenerContributors.add(obj);
		            }
		        }
		        catch (Exception e)
		        {
		            e.printStackTrace();
		            // Ignore
		        }
		    }
		}

        return reporterListenerContributors;
    }
    
    /**
     * 
     * @param validPluginIDs
     * @return
     * @throws Exception 
     */
    public static List findTestContributors()
    {
        List testListenerContributors = new ArrayList();

		// Find all of the plug-ins that create extensions for the
		// profilerContributor extension point.
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("org.testng.eclipse.testListenerContributorSchema");
		IExtension extensions[] = extensionPoint.getExtensions();

		testListenerContributors = new ArrayList();
		for (int i = 0; i < extensions.length; i++)
		{
		    Object obj = null;
		    Class c = null;
		    IConfigurationElement elements[] = extensions[i].getConfigurationElements();
		    for (int j = 0; j < elements.length; j++)
		    {
		        String className = elements[j].getAttribute("class");

		        // Find all of the ones that implement IRPCEndpointContributor
		        try
		        {
		            c = Platform.getBundle(extensions[i].getNamespaceIdentifier()).loadClass(className);
		            obj = c.newInstance();
		            // Save them so later we can iterate through them
		            if ( obj instanceof TestListenerAdapter )
		            {
		            	testListenerContributors.add(obj);
		            }
		        }
		        catch (Exception e)
		        {
		            e.printStackTrace();
		            // Ignore
		        }
		    }
		}

        return testListenerContributors;
    }
}
