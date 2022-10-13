/**
 * Entry point to the OSGi application
 * Registers the rest endpoint resources
*/

package com.dotcms.plugin.dotzapier.osgi;

import org.osgi.framework.BundleContext;

import com.dotcms.plugin.dotzapier.util.AppUtil;
import com.dotcms.plugin.dotzapier.zapier.rest.DotZapierResource;
import com.dotcms.plugin.dotzapier.zapier.workflow.ZapierTriggerActionlet;
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

		//Init Services
		this.initializeServices(context);

		//Register Resource
		RestServiceUtil.addResource(DotZapierResource.class);

		//Display the plugin as an App
		Logger.info(Activator.class.getName(), "Generating dotZapier APP");
		new AppUtil().copyAppYml();

		//Register Actionlet
		this.registerActionlet(context, new ZapierTriggerActionlet());
	}

	/**
     * Stops the OSGi application
     * @param context The execution context within the Framework. 
	 * 				  The context is used to grant access to other methods 
	 * 				  so that this bundle can interact with the Framework.
    */
	public void stop(BundleContext context) throws Exception {
		Logger.info(Activator.class.getName(), "Stopping dotZapier Plugin");

		//Remove Actionlet
		this.unregisterServices(context);

		//UnRegister Resource
		RestServiceUtil.removeResource(DotZapierResource.class);

		//Remove the plugin from the App
		Logger.info(Activator.class.getName(), "Removing dotZapier APP");
        new AppUtil().deleteYml();
	}
}