package com.dotcms.plugin.dotzapier.zapier.workflow;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.api.web.HttpServletResponseThreadLocal;
import com.dotcms.mock.request.FakeHttpRequest;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.BaseResponse;
import com.dotcms.plugin.dotzapier.util.ResourceUtil;
import com.dotcms.plugin.dotzapier.zapier.app.ZapierApp;
import com.dotcms.plugin.dotzapier.zapier.app.ZapierAppAPI;
import com.dotcms.plugin.dotzapier.zapier.content.ContentAPI;
import com.dotcms.rendering.engine.ScriptEngine;
import com.dotcms.rendering.engine.ScriptEngineFactory;
import com.dotcms.util.CollectionsUtils;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClassParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowActionFailureException;
import com.dotmarketing.portlets.workflows.model.WorkflowActionletParameter;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.json.JSONObject;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import io.vavr.control.Try;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This actionlet basically sends the json contentlet event to whatevet hooks (or hooks) subscribed by zapier.
 * Also the user can write custom script (velocity) code as a precondition to add extra properties as part of the contentlet to zapier
 */
public class ZapierTriggerActionlet extends WorkFlowActionlet {
    private final static String ENGINE = "Velocity";
    private static final ZapierAppAPI zapierAppAPI = new ZapierAppAPI();

    private static final ContentAPI contentAPI   = new ContentAPI();
    private static final long serialVersionUID = 1L;
    private static List<WorkflowActionletParameter> parameterList = createParamList();
    private static final ResourceUtil resourceUtil = new ResourceUtil();
    
    /**
     * Input parameters to the Workflow action
     * @return List
    */
    @Override
    public List<WorkflowActionletParameter> getParameters() {

        return parameterList;
    }

    private static List<WorkflowActionletParameter> createParamList () {

        final ImmutableList.Builder<WorkflowActionletParameter> paramList = new ImmutableList.Builder<>();

        paramList.add(new WorkflowActionletParameter
                ("webHookUrl", "Zap Webhook URL", null, false));

        paramList.add(new WorkflowActionletParameter
                ("script", "Post Script Code", null, false));

        return paramList.build();
    }

    /**
     * Name of the Workflow action
    */
    @Override
    public String getName() {
        return "Send to Zapier";
    }

    /**
     * Description associated with the Workflow action
    */
    @Override
    public String getHowTo() {
        return "Send notification to Zapier for integrating with third party apps";
    }

    private HttpServletRequest  mockRequest (final User currentUser) {

        final Host host = Try.of(()-> APILocator.getHostAPI()
                .findDefaultHost(currentUser, false)).getOrElse(APILocator.systemHost());
        return new MockAttributeRequest(
                new MockSessionRequest(
                        new FakeHttpRequest(host.getHostname(), StringPool.FORWARD_SLASH).request()
                ).request()
        ).request();
    }

    private HttpServletResponse mockResponse () {

        return new BaseResponse().response();
    }
    public void executeScript(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        try {

            final User currentUser          = processor.getUser();
            final HttpServletRequest request =
                    null == HttpServletRequestThreadLocal.INSTANCE.getRequest()?
                            this.mockRequest(currentUser): HttpServletRequestThreadLocal.INSTANCE.getRequest();
            final HttpServletResponse response =
                    null == HttpServletResponseThreadLocal.INSTANCE.getResponse()?
                            this.mockResponse(): HttpServletResponseThreadLocal.INSTANCE.getResponse();
            final WorkflowActionClassParameter scriptParameter = params.get("script");
            final String script       = scriptParameter.getValue();
            if (UtilMethods.isSet(script)) {
                final ScriptEngine engine = ScriptEngineFactory.getInstance().getEngine(ENGINE);
                final Reader reader = new StringReader(script);
                engine.eval(request, response, reader,
                        CollectionsUtils.map("workflow", processor,
                                "user", processor.getUser(),
                                "contentlet", processor.getContentlet(),
                                "content", processor.getContentlet()));
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new WorkflowActionFailureException(e.getMessage(), e);
        }
    }

    private Optional<ZapierApp> findZappierApp () {

        if (null != HttpServletRequestThreadLocal.INSTANCE.getRequest()) {

            try {
                final Host currentSite = WebAPILocator.getHostWebAPI()
                        .getCurrentHost(HttpServletRequestThreadLocal.INSTANCE.getRequest());
                return this.zapierAppAPI.config(currentSite);
            } catch (Exception e) {

                Logger.debug(this, e.getMessage());
            }
        }

        return this.zapierAppAPI.config();
    }

    /**
     * This method gets invoked when an action is performed on a dotCMS content
     * @param processor {@link WorkflowProcessor}
     * @param params Workflow action parameters
     * @throws WorkflowActionFailureException
    */
    @Override
    public void executeAction(WorkflowProcessor processor, Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

        Logger.info(this, "Zapier Workflow action invoked");

        final Optional<ZapierApp> zapierAppOpt = this.findZappierApp();

        if (zapierAppOpt.isPresent()) {

            final Contentlet contentlet = processor.getContentlet();

            // Do not execute the workflow action if no content is found
            // This would only occur when the destroy workflow is executed before Zapier workflow
            if (contentlet == null) {
                Logger.error(this, "No contentlet found.");
                return;
            }

            // if allowed content types are set, so we check if this content type is allowed for zapier
            final Set<String> allowedContentTypes   = zapierAppOpt.get().getAllowedContentTypes();
            final boolean areAllowedAllContentTypes = zapierAppAPI.isAllowedAllContentTypes (allowedContentTypes);
            final String currentContentTypeVar      = contentlet.getContentType().variable();
            if (!areAllowedAllContentTypes && // if not all types are allowed and the type is not allowed
                    !allowedContentTypes.contains(currentContentTypeVar)) {

                Logger.info(this, "Content type not allowed: " + currentContentTypeVar);
                return;
            }

            this.executeScript(processor, params);

            final Map<String, String> zapierWebHookTriggerURLS = zapierAppOpt.get().getZapsRegisterMap();

            Logger.info(this, "Firing zapier actionlet");
            Logger.info(this, "Available Zapier Actions " + zapierAppOpt.get().getZapsRegisterMap());

            final JSONObject dotCMSObject = this.prepareContentletObject(contentlet);
            final WorkflowActionClassParameter webHookUrlParameter = params.get("webHookUrl");
            final String webHookUrl = webHookUrlParameter.getValue();
            if (UtilMethods.isSet(webHookUrl)) {

                this.pushToZapier(resourceUtil, webHookUrl, dotCMSObject);
            } else {

                for (final Map.Entry<String, String> webHookEntry : zapierWebHookTriggerURLS.entrySet()) {

                    final String webHookKey = webHookEntry.getKey();
                    // if exists any hook that contains the current content type variable
                    if (UtilMethods.isSet(webHookKey) && webHookKey.contains(currentContentTypeVar)) {

                        final String zapierActionUrl = webHookEntry.getValue();
                        this.pushToZapier(resourceUtil, zapierActionUrl, dotCMSObject);
                    }
                }
            }
        } else {

            Logger.error(this, "No Zapier configuration found");
        }
    }



    private boolean isContentTypeAllowed(final Set<String> allowedContentTypes, final String currentContentTypeVar) {

        if (UtilMethods.isSet(allowedContentTypes)) {

            return allowedContentTypes.contains(currentContentTypeVar);
        }

        return true; // if allowedContentTypes is null or empty, all content types are allowed
    }

    private void pushToZapier (final ResourceUtil resourceUtil, final String zapierActionUrl, final JSONObject dotCMSObject) {

        try {
            // Obtain the stored Subscribe URL
            Logger.info(this.getClass().getName(), "zapierActionUrl= " + zapierActionUrl);
            // Publish to Zapier
            if (resourceUtil.publishToZapier(zapierActionUrl, dotCMSObject)) {

                Logger.info(this.getClass().getName(), "The zapierActionUrl= " + zapierActionUrl + " was sent ok");
            } else {
                Logger.info(this.getClass().getName(), "The zapierActionUrl= " + zapierActionUrl + " NO OK");
            }
        } catch (Exception ex) {
            Logger.error(this, "Unable to obtain Zapier action url");
            Logger.error(this, ex.getMessage());
        }
    }

    /**
     * Generates the contentlet object which needs to be sent out to Zapier.
     * It would contain all the output fields in a Zap trigger
     * @param contentlet dotCMS Contentlet object
     * @return JSONObject Contains only the fields needed to be processed by Zapier
    */
    private final JSONObject prepareContentletObject(final Contentlet contentlet) {
        JSONObject dotCMSObject = new JSONObject();

        try {
            final String identifier = contentlet.getIdentifier();
            final String host = contentlet.getHost();
            final String contentType = contentlet.getContentType().variable();
            final String title = contentlet.getTitle();
            final String modUserName = contentlet.getModUser();
            final String modDate = contentlet.getModDate().toString();
            final String owner = contentlet.getOwner();
            final boolean isArchived = contentlet.isArchived();
            final boolean isWorking = contentlet.isWorking();
            final boolean isLocked = contentlet.isLocked();
            final boolean isLive = contentlet.isLive();
            
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "id"), identifier);
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "identifier"), identifier);
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "hostName"), host);
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "contentType"), contentType);
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "title"), title);
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "modUserName"), modUserName);
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "modDate"), modDate);
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "owner"), owner);
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "archived"), isArchived);
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "working"), isWorking);
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "locked"), isLocked);
            dotCMSObject.put(contentAPI.createContentKey(contentlet, "live"), isLive);

            for (final Map.Entry entry: contentlet.getMap().entrySet()) {

                dotCMSObject.put(contentAPI.createContentKey(contentlet, entry.getKey().toString()), entry.getValue());
            }

            Logger.info(this, "dotCMSObject = " + dotCMSObject);
        }
        catch(Exception ex) {
            Logger.error(this, "Unable to prepare the contentlet object to be sent to Zapier");
            Logger.error(this, ex.getMessage());
        }

        return dotCMSObject;
    }
}
