/**
 * dotZapier Plugin REST Endpoint
 * Defines the public url associated with the plugin
*/

package com.dotcms.plugin.dotzapier.zapier.rest;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.plugin.dotzapier.util.ContentParser;
import com.dotcms.plugin.dotzapier.util.ResourceUtil;
import com.dotcms.plugin.dotzapier.zapier.app.ZapierApp;
import com.dotcms.plugin.dotzapier.zapier.app.ZapierAppAPI;
import com.dotcms.plugin.dotzapier.zapier.content.ContentAPI;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This endpoint is in charge of un/subscribed zaps, also provide some helper methods for it in addition to encapsulate the perform create action
 * for the dotEvent
 */
@Path("/v1/dotzapier")
public class DotZapierResource  {

	private static final long serialVersionUID = 1L;
    private final ZapierAppAPI zapierAppAPI = new ZapierAppAPI();
    private final ContentAPI   contentAPI   = new ContentAPI();
    private final ContentParser contentParser = new ContentParser();

    /**
     * This is a public endpoint to verify if the dotZapier plugin is reachable or not
     *
     * @return It will return a message
     * {"server: "online"}
     */
   /* @GET
    @Path("/hello")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response hello(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
            throws URISyntaxException, DotStateException, DotDataException, DotSecurityException {

        final ResponseEntityView responseEntityView = new ResponseEntityView(ImmutableMap.of("hello", "all"));
        return Response.ok(responseEntityView).build();
    }*/

    /**
     * This is a public endpoint to verify if the dotZapier plugin is reachable or not
     * 
     * @return It will return a message
     * {"server: "online"}
    */
	@GET
    @Path("/health")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getHealth(@Context final HttpServletRequest request, @Context final HttpServletResponse response) 
		throws URISyntaxException, DotStateException, DotDataException, DotSecurityException {

		final ResponseEntityView responseEntityView = new ResponseEntityView(ImmutableMap.of("server", "online"));
        return Response.ok(responseEntityView).build();
    }

    /**
     * This endpoint verifies if the dotZapier plugin is reachable or not
     * 
     * This endpoint requires authentication. 
     * 
     * @return It will return a message
     * {"user": "authenticated"}
    */
    @GET
    @Path("/auth-health")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getAuthHealth(@Context final HttpServletRequest request, @Context final HttpServletResponse response) 
		throws URISyntaxException, DotStateException, DotDataException, DotSecurityException {
		// Only allow authenticated users
        new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();

		final ResponseEntityView responseEntityView = new ResponseEntityView(ImmutableMap.of("user", "authenticated"));

        return Response.ok(responseEntityView).build();
    }

    private JSONArray getDotCMSData (final HttpServletRequest request) {

        final JSONArray dotCMSData = new JSONArray();

        try {

            final Host currentSite = WebAPILocator.getHostWebAPI().getCurrentHost(request);
            final Optional<ZapierApp>  zapierApp = this.zapierAppAPI.config(currentSite);

            final Set<String> types = zapierApp.isPresent()?
                    zapierApp.get().getAllowedContentTypes(): Collections.emptySet();

            final List<Contentlet> contentlets = zapierApp.isPresent() &&
                    !zapierAppAPI.isAllowedAllContentTypes(zapierApp.get().getAllowedContentTypes())?
                    this.contentAPI.contents(zapierApp.get().getAllowedContentTypes()): this.contentAPI.contents();

            for (final Contentlet contentlet : contentlets) {
                // Obtain the specific keys from the object
                final JSONObject contentJsonObject = new JSONObject();
                final String contentTypeVar = contentlet.getContentType().variable();
                final String title = contentlet.getTitle();


                contentJsonObject
                        .put(this.contentAPI.createContentKey(contentlet, "id"), contentlet.getIdentifier())
                        .put(this.contentAPI.createContentKey(contentlet, "identifier"), contentlet.getIdentifier())
                        .put(this.contentAPI.createContentKey(contentlet, "hostName"), contentlet.getHost())
                        .put(this.contentAPI.createContentKey(contentlet, "contentType"), contentlet.getContentType().variable())
                        .put(this.contentAPI.createContentKey(contentlet, "title"), contentlet.getTitle())
                        .put(this.contentAPI.createContentKey(contentlet, "modUserName"), contentlet.getModUser())
                        .put(this.contentAPI.createContentKey(contentlet, "owner"), contentlet.getOwner())
                        .put(this.contentAPI.createContentKey(contentlet, "archived"), contentlet.isArchived())
                        .put(this.contentAPI.createContentKey(contentlet, "working"), contentlet.isWorking())
                        .put(this.contentAPI.createContentKey(contentlet, "locked"), contentlet.isLocked())
                        .put(this.contentAPI.createContentKey(contentlet, "live"), contentlet.isLive())
                        .put(this.contentAPI.createContentKey(contentlet, "modDate"), contentlet.getModDate());

                for (final Field field: contentlet.getContentType().fields()) {

                    final Object value = contentlet.get(field.variable());

                    contentJsonObject.put(this.contentAPI.createContentKey(contentlet, field.variable()), null == value? "Example Value":value.toString());
                }


                dotCMSData.put(contentJsonObject);
            }
        } catch(Exception ex) {
            Logger.error(this, "Unable to process getZapierList request");
            Logger.error(this, ex.getMessage());
        }

        return dotCMSData;
    }

    @GET
    @Path("/perform-list-raw")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getZapierContentSampleListRaw(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
            throws URISyntaxException, DotStateException, DotDataException, DotSecurityException, JSONException {

        new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();

        Logger.info(this, "Perform List Zapier Raw API invoked");
        final ResourceUtil resourceUtil = new ResourceUtil();
        final String hostName = this.getHostName(request);
        final JSONArray dotCMSData = this.getDotCMSData(request);

        // Build the API response
        return Response.status(200).entity(dotCMSData).build();
    }

    /**
     * This endpoint provides the list of most recent content generated on dotCMS
     * It is consumed by the Zapier perform list operation. It is invoked for each Zap 
     * at the time of creation via the Zapier UI
     * 
     * This endpoint requires authentication. 
     * 
     * @return It will return list of dotCMS content
     * [
     *  {
     *      "id": "2a4fb69a-1f79-4bef-be47-20dec8669c78",
     *      "identifier": "2a4fb69a-1f79-4bef-be47-20dec8669c78",
     *      "hostName": "demo.dotcms.com",
     *      "url": "https://demo.dotcms.com/content.ec5c6e2f-4266-4ff8-adfc-22f76ba453b7",
     *      "contentType": "My Blog",
     *      "title": "French Polynesia Everything You Need to Know",
     *      "modUserName": "Admin User",
     *      "owner": "dotcms.org.1",
     *      "archived": false,
     *      "working": true,
     *      "locked": false,
     *      "live": true,
     *      "modDate": "2022-06-07 18:23:10.844",
     *   }
     * ]
    */
    @GET
    @Path("/perform-list")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getZapierContentSampleList(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
		throws URISyntaxException, DotStateException, DotDataException, DotSecurityException, JSONException {
		// Only allow authenticated users
        new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();
        
        Logger.info(this, "Perform List Zapier API invoked");
        final ResourceUtil resourceUtil = new ResourceUtil();
        final String hostName = this.getHostName(request);
        final JSONArray dotCMSData = this.getDotCMSData(request);
        // Build the API response
        final JSONObject userResponse = new JSONObject();
        userResponse.put("data", dotCMSData.toString());
        return Response.status(200).entity(userResponse).build();
    }

    /**
     * This endpoint provides the list of the types to execute
     * It is consumed by the Zapier perform action list operation. It is invoked for each Zap
     * at the time of creation via the Zapier UI
     *
     * This endpoint requires authentication.
     *
     * @return It will return list of dotCMS content
     *
     * {
     *     "type-var1":"type-var1",
     *     "type-var2":"type-var2"
     * }
     */
    @GET
    @Path("/perform-type-list")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getZapierTypeList(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
            throws URISyntaxException, DotStateException, DotDataException, DotSecurityException, JSONException {
        // Only allow authenticated users
        new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();

        Logger.info(this, "Perform Type List Zapier API invoked");
        final JSONObject userResponse = new JSONObject();

        this.contentAPI.types().entrySet().forEach(entry ->
                Try.run(()->userResponse.put(entry.getKey(), entry.getValue())));

        // Build the API response
        return Response.status(200).entity(userResponse).build();
    }

    /**
     * This endpoint provides the list of the allowed types to execute
     * It is consumed by the Zapier perform action list operation. It is invoked for each Zap
     * at the time of creation via the Zapier UI
     *
     * This endpoint requires authentication.
     *
     * @return It will return list of dotCMS content
     *
     * {
     *     "type-var1":"type-var1",
     *     "type-var2":"type-var2"
     * }
     */
    @GET
    @Path("/perform-allowed-type-list")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getZapierAlloweedTypeList(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
            throws Exception {
        // Only allow authenticated users
        new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();

        final Host currentSite = WebAPILocator.getHostWebAPI().getCurrentHost(request);
        final Optional<ZapierApp> zapierAppOpt = this.zapierAppAPI.config(currentSite);

        if (zapierAppOpt.isPresent()) {

            final Set<String> allowedContentTypes = zapierAppOpt.get().getAllowedContentTypes();
            if (!allowedContentTypes.contains("*")) { // if do not want all content types

                final JSONObject userResponse = new JSONObject();

                this.contentAPI.types(allowedContentTypes).entrySet().forEach(entry ->
                        Try.run(()->userResponse.put(entry.getKey(), entry.getValue())));

                // Build the API response
                return Response.status(200).entity(userResponse).build();
            }
        }

        Logger.info(this, "Perform Type List Zapier API invoked");
        final JSONObject userResponse = new JSONObject();

        this.contentAPI.types().entrySet().forEach(entry ->
                Try.run(()->userResponse.put(entry.getKey(), entry.getValue())));

        // Build the API response
        return Response.status(200).entity(userResponse).build();
    }

    /**
     * This endpoint provides the list of the formats allowed to send
     * It is consumed by the Zapier perform action list operation. It is invoked for each Zap
     * at the time of creation via the Zapier UI
     *
     * This endpoint requires authentication.
     *
     * @return It will return list of dotCMS content
     * {
     *     "format1":"format1",
     *     "format2":"format2"
     * }
     */
    @GET
    @Path("/perform-format-list")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getZapierFormatList(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
            throws DotStateException {
        // Only allow authenticated users
        new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();

        Logger.info(this, "Perform Format List Zapier API invoked");
        final JSONObject userResponse = new JSONObject();

        this.contentAPI.contentInputFormatMap().entrySet().forEach(entry ->
                Try.run(()->userResponse.put(entry.getKey(), entry.getValue())));

        // Build the API response
        return Response.status(200).entity(userResponse).build();
    }

    /**
     * This endpoint provides the list of the actions to execute
     * It is consumed by the Zapier perform action list operation. It is invoked for each Zap
     * at the time of creation via the Zapier UI
     *
     * This endpoint requires authentication.
     *
     * @return It will return list of dotCMS content
     *
     * {
     *     "save":"save",
     *     "edit":"edit",
     *     ...
     * }
     */
    @GET
    @Path("/perform-action-list")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getZapierActionList(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
            throws URISyntaxException, DotStateException, DotDataException, DotSecurityException, JSONException {
        // Only allow authenticated users
        new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();

        Logger.info(this, "Perform Action List Zapier API invoked");
        final JSONObject userResponse = new JSONObject();

        this.contentAPI.actionNames().entrySet().forEach(entry ->
                Try.run(()->userResponse.put(entry.getKey(), entry.getValue())));

        // Build the API response
        return Response.status(200).entity(userResponse).build();
    }

    /**
     * This endpoint deletes the rest url provided by Zapier for the specific trigger
     * It is consumed by the Zapier unsubscribe operation. It is invoked when a Zap 
     * is turned off via the Zapier UI
     * 
     * This endpoint requires authentication.
     * 
     * @param triggerName It is a query parameter that indicates the name of the trigger
     * ?triggerName=publish
     * 
     * @return It will return a message 
     * { "message": "Zapier hook removed" }
    */
    @DELETE
    @Path("/unsubscribe")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON) // todo: we have to refactor this to reflect the new approach
    public final Response deleteUnSubscribe(@Context final HttpServletRequest request, @Context final HttpServletResponse response) 
		throws URISyntaxException, DotStateException, DotDataException, DotSecurityException {
		// Only allow authenticated users
        new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();

        final String triggerUrl = request.getParameter("triggerUrl");

        Logger.info(this, "Unsubscribe Zapier API invoked, triggerUrl: " + triggerUrl);
        // Update the Zapier Trigger Data
        this.zapierAppAPI.unregisterZap(triggerUrl);

        // Build the API response
		final ResponseEntityView responseEntityView = new ResponseEntityView(ImmutableMap.of("message", "Zapier hook removed"));
        return Response.ok(responseEntityView).build();
    }

    /**
     * This endpoint stores the rest url provided by Zapier for the specific trigger
     * It is consumed by the Zapier subscribe operation. It is invoked when a Zap 
     * is turned on via the Zapier UI
     * 
     * This endpoint requires authentication.
     * 
     * @param SubscribeBody It is the request body that is sent by Zapier. Contains the trigger name
     *                      and the rest url associated with it.
     * {
     *    "triggerName": "publish",
     *    "url": "htpps://example.zapier.com/f54eb652f79a31b10c7c3ac766268c0415/237e31a74ec2d1d89d02976e54adca"  
     * } 
     * 
     * @return It will return a message 
     * { "message": "Zapier hook added" }
    */
    @POST
    @Path("/subscribe")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON) // todo: this is done
    public final Response postSubscribe(@Context final HttpServletRequest request, @Context final HttpServletResponse response) 
		throws URISyntaxException, DotStateException, DotDataException, DotSecurityException, IOException, JSONException {

		// Only allow authenticated users
        new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();
        
        Logger.info(this, "Subscribe Zapier API invoked");

		final String jsonString = new String(IOUtils.toByteArray(request.getInputStream()));
		final JSONObject requestBody = new JSONObject(jsonString);
        final String actionName = requestBody.optString("triggerName", "");
        final String triggerURL = requestBody.optString("url", "");
        final String type       = requestBody.optString("type", "");


        Logger.info(this, "URL " + triggerURL);
        Logger.info(this, "actionName " + actionName);
        Logger.info(this, "type " + type);
        
        // Save the Zapier Trigger Data
        this.zapierAppAPI.registerZap(actionName, triggerURL, type);

        // Build the API response
        final ResponseEntityView responseEntityView = new ResponseEntityView(ImmutableMap.of("message", "Zapier hook added"));
        return Response.ok(responseEntityView).build();
    }

    /**
     * This endpoint receives the content from the Zapier Action application
     * It is consumed by the Zapier create operation. It is invoked when a Zapier  
     * Action app is triggered. For Eg => When a slack message is sent to a Channel
     * 
     * This endpoint requires authentication. 
     * 
     * @param ActionBody It is the request body that is sent by Zapier. Contains 
     *                   the content type and the text used to generate the content
     * {
     *    "contentType": "My Blog",
     *     "text": "#save #title="First Content Title" #url=first-content-title #author="John Doe" #publishDate="Sept 28 2022" Hello World"
     * }
     * 
     * @return It will return a message 
     * { "message": "save action process successfully" }
     * 
    */
    @POST
    @Path("/action") // todo: if the site is not sent, so use the default or system one
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response postCreateAction(@Context final HttpServletRequest request, @Context final HttpServletResponse response)
            throws Exception {
		
        // Only allow authenticated users
        final User user = new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();
        
        Logger.info(this, "Action Zapier API invoked");

		final String jsonString = new String(IOUtils.toByteArray(request.getInputStream()));
		final JSONObject requestBody = new JSONObject(jsonString);
        
        final String contentType = requestBody.optString("contentType", "");
        Logger.info(this, "Name of the Content Type " + contentType);

        final String actionName = requestBody.optString("actionName", "save");
        Logger.info(this, "Name of the Action Name " + actionName);

        final String inputFormat = requestBody.optString("inputFormat", "json");
        Logger.info(this, "Name of the Input Format " + inputFormat);

        final String contentText = requestBody.optString("text", "");
        Logger.info(this, "Text of the content " + contentText);

        // text is a required property on the request payload
        if(contentText.length() == 0) {
            Logger.error(this, "Invalid argument received");

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message", "Invalid argument received");
            return Response.status(400).entity(errorResponse).build();
        }

        final Map<String, Object> dotCMSContent = contentParser.parseContent(inputFormat, contentText);

        Logger.info(this, "dotCMS content " + dotCMSContent.toString());

        if(contentType.length() == 0) {
            Logger.error(this, "contentType is a required field");

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message", "contentType is a required field");
            return Response.status(400).entity(errorResponse).build();
        }

        this.contentAPI.fire(dotCMSContent, contentType, actionName, user);
        return this.buildApiResponse("", actionName);
    }

    private final Response buildApiResponse(final String apiResponse, final String actionName) throws JSONException {
        JSONObject userResponse = new JSONObject();

        // Build the API response
        if(apiResponse.length() == 0) {
            final String successMessage = actionName + " process successfully executed";
            Logger.info(this, successMessage);

            userResponse.put("message", successMessage);
            return Response.status(200).entity(userResponse).build();
        }
        else {
            final String errorMessage = actionName + " process failed";
            Logger.error(this, errorMessage);
            Logger.error(this, apiResponse);

            userResponse.put("error", errorMessage);
            userResponse.put("message", apiResponse);
            return Response.status(400).entity(userResponse).build();
        }
    }

    private String getHostName(final HttpServletRequest request) {
        final String requestUrl = request.getRequestURL().toString();
        final String requestUri = request.getRequestURI();
        final String hostName = requestUrl.replace(requestUri, "");

        return hostName;
    }
}
