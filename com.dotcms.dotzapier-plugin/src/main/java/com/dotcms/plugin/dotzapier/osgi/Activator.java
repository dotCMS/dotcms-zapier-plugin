package com.dotcms.plugin.dotzapier.osgi;

import com.dotcms.plugin.dotzapier.util.AppUtil;
import com.dotcms.plugin.dotzapier.zapier.app.ZapierAppAPI;
import com.dotcms.plugin.dotzapier.zapier.content.ContentAPI;
import com.dotcms.plugin.dotzapier.zapier.rest.DotZapierResource;
import com.dotcms.plugin.dotzapier.zapier.viewtools.ZapierToolInfo;
import com.dotcms.plugin.dotzapier.zapier.workflow.ZapierTriggerActionlet;
import com.dotcms.rest.config.RestServiceUtil;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.util.Logger;
import org.osgi.framework.BundleContext;

/**
 * Entry point to the OSGi application
 * Registers the rest endpoint resources
 */
public class Activator extends GenericBundleActivator {

    /**
     * Starts the OSGi application
     * @param context The execution context within the Framework. 
	 * 				  The context is used to grant access to other methods 
	 * 				  so that this bundle can interact with the Framework.
    */
	public void start(final BundleContext context) throws Exception {
		Logger.info(Activator.class.getName(), "Starting dotZapier Plugin");

		//Init Services
		this.initializeServices(context);

		new ContentAPI();

		//Register Resource
		RestServiceUtil.addResource(DotZapierResource.class);

		//Display the plugin as an App
		Logger.info(Activator.class.getName(), "Generating dotZapier APP");
		new AppUtil().copyAppYml();

		//Register Actionlet
		this.registerActionlet(context, new ZapierTriggerActionlet());

		registerViewToolService(context, new ZapierToolInfo());
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

		unregisterViewToolServices();
	}
}
