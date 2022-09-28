/**
 * Entry point to the OSGi application
 * Registers the rest endpoint resources
*/

package com.dotcms.plugin.dotzapier;

import org.osgi.framework.BundleContext;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.Logger;

public class Activator extends GenericBundleActivator {

    /**
     * Starts the OSGi application
     * @param context The execution context within the Framework. 
	 * 				  The context is used to grant access to other methods 
	 * 				  so that this bundle can interact with the Framework.
    */
	public void start(BundleContext context) throws Exception {
		Logger.info(Activator.class.getName(), "Starting dotZapier Plugin");

		//Register Resource
		RestServiceUtil.addResource(DotZapierResource.class);
	}

	/**
     * Stops the OSGi application
     * @param context The execution context within the Framework. 
	 * 				  The context is used to grant access to other methods 
	 * 				  so that this bundle can interact with the Framework.
    */
	public void stop(BundleContext context) throws Exception {
		Logger.info(Activator.class.getName(), "Stopping dotZapier Plugin");

		//UnRegister Resource
		RestServiceUtil.removeResource(DotZapierResource.class);
	}
}