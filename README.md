# dotcms-zapier-plugin

Two plugins are needed to connect Zapier with dotCMS.

The first plugin is an OSGi application that needs to be executed within the dotCMS. It acts as a web server to receive requests from Zapier actions and sends out content for Zapier triggers

OSGi Application is present in [com.dotcms.dotzapier-plugin](com.dotcms.dotzapier-plugin)

The other plugin is a Node application that needs to be deployed on Zapier as the Zapier app. It sends out requests to the dotCMS plugin and acts as a middleware between different third-party applications. Allows us to tap into a large Zapier app directory which can be connected with dotCMS

Zapier Plugin is present in [zapier-plugin](zapier-plugin)
