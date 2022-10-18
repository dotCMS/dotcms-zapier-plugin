/**
 * dotZapier Plugin REST Endpoint
 * Defines the public url associated with the plugin
*/

package com.dotcms.plugin.dotzapier.zapier.rest;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.Produces;

import com.dotcms.plugin.dotzapier.util.ContentParser;
import com.dotcms.plugin.dotzapier.util.ResourceUtil;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.util.json.JSONArray;
import com.liferay.portal.model.User;
import jersey.repackaged.com.google.common.collect.ImmutableMap;

@Path("/v1/dotzapier")
public class DotZapierResource  {

	private static final long serialVersionUID = 1L;


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
        final User user = new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();

		final ResponseEntityView responseEntityView = new ResponseEntityView(ImmutableMap.of("user", "authenticated"));

        return Response.ok(responseEntityView).build();
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
    public final Response getZapierList(@Context final HttpServletRequest request, @Context final HttpServletResponse response) 
		throws URISyntaxException, DotStateException, DotDataException, DotSecurityException, JSONException {
		// Only allow authenticated users
        final User user = new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();
        
        Logger.info(this, "Perform List Zapier API invoked");
        ResourceUtil resourceUtil = new ResourceUtil();

        final String hostName = this.getHostName(request);
        final String dotCMSAPIKey = request.getHeader("authorization");
        
        JSONArray dotCMSData = new JSONArray();
        
        // Invoke the /api/content/_search API to obtain the list of dotCMS objects
        try {
            final JSONObject responseBody = resourceUtil.obtainContentFromDotCMS(hostName, dotCMSAPIKey, "");
            
            // Obtains the result from the required key
            if(responseBody.has("entity")) {
                final JSONObject entity = responseBody.getJSONObject("entity");
                if(entity.has("jsonObjectView")) {
                    final JSONObject jsonObjectView = entity.getJSONObject("jsonObjectView");
                    if(jsonObjectView.has("contentlets")) {
                        final JSONArray contentlets = jsonObjectView.getJSONArray("contentlets");
                        for(int i=0; i< contentlets.length(); i++) {
                            // Obtain the specific keys from the object
                            JSONObject temp = resourceUtil.prepareZapierObject( (JSONObject) contentlets.get(i));
                            
                            // Append the domain information to the URL
                            if(temp.has("url")) {
                                final String url = resourceUtil.prepareContentletUrl(hostName, temp.getString("url"));
                                temp.put("url", url);
                            }

                            dotCMSData.put(temp);
                        }
                    }
                }
            }
        }
        catch(Exception ex) {
            Logger.error(this, "Unable to process getZapierList request");
            Logger.error(this, ex.getMessage());
        }
        
        // Build the API response
        JSONObject userResponse = new JSONObject();
        userResponse.put("data", dotCMSData.toString());
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
    @Produces(MediaType.APPLICATION_JSON)
    public final Response deleteUnSubscribe(@Context final HttpServletRequest request, @Context final HttpServletResponse response) 
		throws URISyntaxException, DotStateException, DotDataException, DotSecurityException {
		// Only allow authenticated users
        final User user = new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();

        Logger.info(this, "Unsubscribe Zapier API invoked");

        ResourceUtil resourceUtil = new ResourceUtil();

        final String actionName = request.getParameter("triggerName"); 

        // Update the Zapier Trigger Data
        final JSONObject zapierTriggerURLS = resourceUtil.readJSON();
        zapierTriggerURLS.remove(actionName);
        resourceUtil.writeJSON(zapierTriggerURLS);

        Logger.info(this, "Zapier Details Saved " + zapierTriggerURLS.toString());

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
    @Produces(MediaType.APPLICATION_JSON)
    public final Response postSubscribe(@Context final HttpServletRequest request, @Context final HttpServletResponse response) 
		throws URISyntaxException, DotStateException, DotDataException, DotSecurityException, IOException, JSONException {
		// Only allow authenticated users
        final User user = new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();
        
        Logger.info(this, "Subscribe Zapier API invoked");

        ResourceUtil resourceUtil = new ResourceUtil();

        final String hostName = this.getHostName(request);

		final String jsonString = new String(IOUtils.toByteArray(request.getInputStream()));
		final JSONObject requestBody = new JSONObject(jsonString);
        
        final String actionName = requestBody.optString("triggerName", "");
        final String triggerURL = requestBody.optString("url", "");

        Logger.info(this, "URL " + triggerURL);
        Logger.info(this, "actionName " + actionName);
        
        // Save the Zapier Trigger Data
        final JSONObject zapierTriggerURLS = resourceUtil.readJSON();
        if(!zapierTriggerURLS.has("url")) {
            zapierTriggerURLS.put("url", hostName);
        }

        zapierTriggerURLS.put(actionName, triggerURL);
        resourceUtil.writeJSON(zapierTriggerURLS);

        Logger.info(this, "Zapier Details Saved " + zapierTriggerURLS.toString());
        
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
    @Path("/action")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public final Response postCreateAction(@Context final HttpServletRequest request, @Context final HttpServletResponse response) 
		throws URISyntaxException, DotStateException, DotDataException, DotSecurityException, IOException, JSONException {
		
        // Only allow authenticated users
        final User user = new WebResource.InitBuilder(request, response).rejectWhenNoUser(true).requiredBackendUser(true).init().getUser();
        
        Logger.info(this, "Action Zapier API invoked");

        ResourceUtil resourceUtil = new ResourceUtil();

        final String hostName = this.getHostName(request);
        final String dotCMSAPIKey = request.getHeader("authorization");

		final String jsonString = new String(IOUtils.toByteArray(request.getInputStream()));
		final JSONObject requestBody = new JSONObject(jsonString);
        
        final String contentType = requestBody.optString("contentType", "");
        Logger.info(this, "Name of the Content Type " + contentType);
        
        final String contentText = requestBody.optString("text", "");
        Logger.info(this, "Text of the content " + contentText);
        
        // text is a required property on the request payload 
        if(contentText.length() == 0) {
            Logger.error(this, "Invalid argument received");

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message", "Invalid argument received");
            return Response.status(400).entity(errorResponse).build();
        }

        ContentParser contentParser = new ContentParser();

        final JSONObject dotCMSContent = contentParser.parse(contentText);

        Logger.info(this, "dotCMS content " + dotCMSContent.toString());

        // If the action name is not recognized, reject the API
        if(!dotCMSContent.has("actionName")) {
            Logger.error(this, "No action found");

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("message", "No action found");
            return Response.status(400).entity(errorResponse).build();
        }

        final String actionName = dotCMSContent.optString("actionName");
        if(actionName.equals("save")) { // Save Action
            // For Save action, content type is a required property
            if(contentType.length() == 0) {
                Logger.error(this, "contentType is a required field");
    
                JSONObject errorResponse = new JSONObject();
                errorResponse.put("message", "contentType is a required field");
                return Response.status(400).entity(errorResponse).build();
            }

            final JSONObject contentTypeObject = resourceUtil.getContentTypeObject(hostName, dotCMSAPIKey, contentType);
            
            final String contentTypeVariableName = contentTypeObject.optString("variableName", "");
            Logger.info(this, "Content Type Variable Name " + contentTypeVariableName);

            // If content type cannot be resolved, then reject the request
            if(contentTypeVariableName.length() == 0) {
                Logger.error(this, "Content Type variable not found");

                JSONObject errorResponse = new JSONObject();
                errorResponse.put("message", "Content Type variable not found");
                return Response.status(400).entity(errorResponse).build();
            }

            // Invoke dotCMS crud API
            final String apiResponse = resourceUtil.saveOperation(hostName, dotCMSAPIKey, dotCMSContent, contentTypeObject);
            return this.buildApiResponse(apiResponse, actionName);
        }
        else if( // All other actions
            actionName.equals("edit") ||
            actionName.equals("publish") ||
            actionName.equals("unpublish") ||
            actionName.equals("archive") ||
            actionName.equals("unarchive") ||
            actionName.equals("delete") ||
            actionName.equals("destroy")
        ) {
            String contentIdentifier = dotCMSContent.optString("identifier", "");
            // If the content identifier cannot be resolved, then reject the request
            if(contentIdentifier.length() == 0) {
                final String title = dotCMSContent.optString("title", "");

                if(title.length() == 0) {
                    Logger.error(this, "Either specifies the identifier or title");

                    JSONObject errorResponse = new JSONObject();
                    errorResponse.put("message", "Either specifies the identifier or title");
                    return Response.status(400).entity(errorResponse).build();
                }

                contentIdentifier = resourceUtil.searchContentIdentifier(hostName, dotCMSAPIKey, title);
                
                if(contentIdentifier.length() == 0) {
                    Logger.error(this, "No content matching the search criteria found");

                    JSONObject errorResponse = new JSONObject();
                    errorResponse.put("message", "No content matching the search criteria found");
                    return Response.status(400).entity(errorResponse).build();
                }
            }

            if(actionName.equals("edit")) {
                final JSONObject contentTypeObject = resourceUtil.getContentTypeObject(hostName, dotCMSAPIKey, contentType);
                
                // Invoke dotCMS crud API
                final String apiResponse = resourceUtil.editOperation(hostName, dotCMSAPIKey, contentIdentifier, dotCMSContent, contentTypeObject);
                return this.buildApiResponse(apiResponse, actionName);
            }
            else {
                // Invoke dotCMS crud API
                final String apiResponse = resourceUtil.workflowOperation(hostName, dotCMSAPIKey, contentIdentifier, actionName);
                return this.buildApiResponse(apiResponse, actionName);
            }
        }

        Logger.error(this, "Unable to process the request");

        // Build the API response
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("message", "Unable to process the request");
        return Response.status(400).entity(errorResponse).build();
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
