package com.dotcms.plugin.dotzapier.zapier.workflow;

import com.dotcms.api.vtl.model.DotJSON;
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
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.rendering.velocity.viewtools.content.ContentMap;
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
import com.dotmarketing.util.PageMode;
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
                ("webHookUrl", "<strong>Zap Webhook URL:</strong><br/>Overrides webhook; can usually be left blank.", null, false));

        paramList.add(new WorkflowActionletParameter
                ("script", "<strong>Post-Script Code:</strong><br/>Performs manual Velocity operations on content.<br/>" +
                        "<a href=\"https://auth.dotcms.com/docs/latest/dotzapier-integration-plugin#PostScriptCode\"" + 
                        "target=\"_blank\" style=\"text-decoration:underline; color:#426bf0;\">Variables:</a> <code>$content</code>", 
                        null, false));

        paramList.add(new WorkflowActionletParameter
                ("fieldValueCustomizeScript",
                        "<strong>Field Variable Customization:</strong><br/>" +
                        "Performs a transformation on all fields.<br/>" +
                        "<a href=\"https://auth.dotcms.com/docs/latest/dotzapier-integration-plugin#FieldValueCustomization\"" +
                        "target=\"_blank\" style=\"text-decoration:underline; color:#426bf0;\">Variables:</a>" +
                        "<code>$contentlet</code>, <code>$fieldVarName</code>,<br/>" +
                        "<code>$fieldValue</code>, <code>$contentMap</code><br/>" +
                        "Execute with <code>$dotJSON.put('var', "XX${var}XX")</code>", null, false));
        
        
        
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

            final WorkflowActionClassParameter scriptParameter = params.get("script");
            final String script       = scriptParameter.getValue();
            if (UtilMethods.isSet(script)) {

                final User currentUser          = processor.getUser();
                final HttpServletRequest request =
                        null == HttpServletRequestThreadLocal.INSTANCE.getRequest()?
                                this.mockRequest(currentUser): HttpServletRequestThreadLocal.INSTANCE.getRequest();
                final HttpServletResponse response =
                        null == HttpServletResponseThreadLocal.INSTANCE.getResponse()?
                                this.mockResponse(): HttpServletResponseThreadLocal.INSTANCE.getResponse();

                final Host site = Try.of(()-> WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request)).getOrElse(APILocator.systemHost());
                final PageMode pageMode = Try.of(()->PageMode.get(request)).getOrElse(PageMode.EDIT_MODE);

                final ScriptEngine engine = ScriptEngineFactory.getInstance().getEngine(ENGINE);
                final Reader reader = new StringReader(script);

                engine.eval(request, response, reader,
                        CollectionsUtils.map("workflow", processor,
                                "user", processor.getUser(),
                                "contentlet", processor.getContentlet(),
                                "content", processor.getContentlet(),
                                "contentMap",
                                    new ContentMap(processor.getContentlet(), processor.getUser(), pageMode, site,
                                            VelocityUtil.getInstance().getContext(request, response))
                                ));
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
    public void executeAction(final WorkflowProcessor processor,
                              final Map<String, WorkflowActionClassParameter> params) throws WorkflowActionFailureException {

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
            final WorkflowActionClassParameter webHookUrlParameter = params.get("webHookUrl");
            final String webHookUrl = webHookUrlParameter.getValue();

            Logger.info(this, "Firing zapier actionlet");
            Logger.info(this, "Available Zapier Actions " + zapierAppOpt.get().getZapsRegisterMap());

            final JSONObject dotCMSData = this.prepareContentletObject(contentlet, processor, params);

            if (UtilMethods.isSet(webHookUrl)) {

                this.pushToZapier(resourceUtil, webHookUrl, dotCMSData);
            } else {

                for (final Map.Entry<String, String> webHookEntry : zapierWebHookTriggerURLS.entrySet()) {

                    final String webHookKey = webHookEntry.getKey();
                    // if exists any hook that contains the current content type variable
                    if (UtilMethods.isSet(webHookKey) && webHookKey.contains(currentContentTypeVar)) {

                        final String zapierActionUrl = webHookEntry.getValue();
                        this.pushToZapier(resourceUtil, zapierActionUrl, dotCMSData);
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
     * @param processor Workflow processor
     * @param params Workflow action parameters
     * @return JSONObject Contains only the fields needed to be processed by Zapier
    */
    protected  JSONObject prepareContentletObject(final Contentlet contentlet, final WorkflowProcessor processor,
                                                  final Map<String, WorkflowActionClassParameter> params) {

        final JSONObject dotCMSObject = new JSONObject();

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

                dotCMSObject.put(contentAPI.createContentKey(contentlet, entry.getKey().toString()),
                        this.processContentFieldValue( entry.getKey().toString(), entry.getValue(),
                                contentlet, processor, params));
            }

            Logger.info(this, "dotCMSObject = " + dotCMSObject);
        }
        catch(Exception ex) {
            Logger.error(this, "Unable to prepare the contentlet object to be sent to Zapier");
            Logger.error(this, ex.getMessage());
        }

        return dotCMSObject;
    }

    protected Object processContentFieldValue(final String fieldVarName, final Object value,
                                              final Contentlet contentlet, final WorkflowProcessor processor,
                                              final Map<String, WorkflowActionClassParameter> params) {

        try {

            final WorkflowActionClassParameter scriptParameter = params.get("fieldValueCustomizeScript");
            final String script       = scriptParameter.getValue();
            if (UtilMethods.isSet(script)) {

                final User currentUser          = processor.getUser();
                final HttpServletRequest request =
                        null == HttpServletRequestThreadLocal.INSTANCE.getRequest()?
                                this.mockRequest(currentUser): HttpServletRequestThreadLocal.INSTANCE.getRequest();
                final HttpServletResponse response =
                        null == HttpServletResponseThreadLocal.INSTANCE.getResponse()?
                                this.mockResponse(): HttpServletResponseThreadLocal.INSTANCE.getResponse();

                final Host site = Try.of(()-> WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(request)).getOrElse(APILocator.systemHost());
                final PageMode pageMode = Try.of(()->PageMode.get(request)).getOrElse(PageMode.EDIT_MODE);

                final ScriptEngine engine = ScriptEngineFactory.getInstance().getEngine(ENGINE);
                final Reader reader = new StringReader(script);

                final Map<String, Object> resultMap = (Map<String, Object>) engine.eval(request, response, reader,
                        CollectionsUtils.map("workflow", processor,
                                "contentlet", processor.getContentlet(),
                                "fieldVarName", fieldVarName,
                                "fieldValue", value,
                                "contentMap",
                                new ContentMap(processor.getContentlet(), processor.getUser(), pageMode, site,
                                        VelocityUtil.getInstance().getContext(request, response))
                        ));

                if (null != resultMap &&  resultMap.containsKey("dotJSON")
                        && null != resultMap.get("dotJSON")) {

                    final DotJSON dotJSON = (DotJSON)resultMap.get("dotJSON");
                    if (null != dotJSON.get("fieldValue")) {
                        return dotJSON.get("fieldValue");
                    }
                }
            }
        } catch (Exception e) {

            Logger.error(this, e.getMessage(), e);
            throw new WorkflowActionFailureException(e.getMessage(), e);
        }


        return null != value? value.toString(): "null";
    }
}
