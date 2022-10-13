package com.dotcms.plugin.dotzapier.zapier.workflow;

import com.dotcms.plugin.dotzapier.util.ResourceUtil;

import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.util.Logger;

import java.util.List;
import java.util.Map;

public class ZapierTriggerActionlet extends WorkFlowActionlet {
    private static final long serialVersionUID = 1L;
    
    /**
     * Input parameters to the Workflow action
     * @return List
    */
    @Override
    public List<WorkflowActionletParameter> getParameters() {
        return null;
    }

    /**
     * Name of the Workflow action
    */
    @Override
    public String getName() {
        return "dotZapier Zap";
    }

    /**
     * Description associated with the Workflow action
    */
    @Override
    public String getHowTo() {
        return null;
    }

    /**
     * This method gets invoked when an action is performed on a dotCMS content
     * @param contentlet dotCMS Contentlet object
     * @param params Workflow action parameters
     * @throws WorkflowActionFailureException
    */
    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        final WorkflowAction workflowAction = processor.getAction();
        final String actionName = workflowAction.getName(); // get Action Name

        final Contentlet contentlet = processor.getContentlet();
        final JSONObject dotCMSObject = this.prepareContentletObject(contentlet);

        ResourceUtil resourceUtil = new ResourceUtil();
        final JSONObject zapierTriggerURLS = resourceUtil.readJSON();
        if(zapierTriggerURLS.has(actionName)) {
            try {
                Logger.info(this, "Zapier Action found " + actionName);

                // Obtain the stored Subscribe URL
                final String zapierActionUrl = zapierTriggerURLS.getString(actionName);

                // Publish to Zapier
                resourceUtil.publishToZapier(zapierActionUrl, dotCMSObject);
            } catch (JSONException ex) {
                Logger.error(this, "Unable to obtain Zapier action url");
                Logger.error(this, ex.getMessage());
            }
        }
        else {
            Logger.error(this, "No Zapier action found");
        }
    }

    /**
     * Obtains the url of the content processed in dotCMS
     * @param contentlet dotCMS Contentlet object
     * @return String URL of the content processed in dotCMS
    */
    private final String generateUrl(Contentlet contentlet) {
        return "";
    }

    /**
     * Generates the contentlet object which needs to be sent out to Zapier.
     * It would contain all the output fields in a Zap trigger
     * @param contentlet dotCMS Contentlet object
     * @return JSONObject Contains only the fields needed to be processed by Zapier
    */
    private final JSONObject prepareContentletObject(Contentlet contentlet) {
        JSONObject dotCMSObject = new JSONObject();

        try {
            final String url = generateUrl(contentlet);

            final String identifier = contentlet.getIdentifier();
            final String hostName = contentlet.getHost();
            final String contentType = contentlet.getContentType().name();
            final String title = contentlet.getTitle();
            final String modUserName = contentlet.getModUser();
            final String modDate = contentlet.getModDate().toString();
            final String owner = contentlet.getOwner();
            final boolean isArchived = contentlet.isArchived();
            final boolean isWorking = contentlet.isWorking();
            final boolean isLocked = contentlet.isLocked();
            final boolean isLive = contentlet.isLive();
            
            dotCMSObject.put("id", identifier);
            dotCMSObject.put("identifier", hostName);
            dotCMSObject.put("url", url);
            dotCMSObject.put("contentType", contentType);
            dotCMSObject.put("title", title);
            dotCMSObject.put("modUserName", modUserName);
            dotCMSObject.put("modDate", modDate);
            dotCMSObject.put("owner", owner);
            dotCMSObject.put("archived", isArchived);
            dotCMSObject.put("working", isWorking);
            dotCMSObject.put("locked", isLocked);
            dotCMSObject.put("live", isLive);
        }
        catch(Exception ex) {
            Logger.error(this, "Unable to prepare the contentlet object to be sent to Zapier");
            Logger.error(this, ex.getMessage());
        }

        return dotCMSObject;
    }
}
