/**
 * Contains the utility methods for dotZapier resource
*/

package com.dotcms.plugin.dotzapier.util;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.time.LocalDateTime;  
import java.io.FileWriter;
import java.io.File;
import java.io.FileInputStream;

import com.dotmarketing.util.Logger;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.util.json.JSONArray;

public class ResourceUtil {

    private static final String zapierComment = "Performed by Zapier";
    private static final String webhookUrls = "webhookUrls.json";

    /**
     * Obtains the content type variable name
     * If it is unable to resolve the content type variable name, then it will return an empty string
     * @param hostName The dotCMS instance url
     * @param dotCMSAPIKey API Key to access the Content API
     * @param ContentTypeName Name of the content type
     * @return JSONObject Contains the dotCMS content type variable name and fields associated with it
     * @throws IOException
     * @throws JSONException
    */
    public final JSONObject getContentTypeObject(final String hostName, final String dotCMSAPIKey, final String ContentTypeName) throws IOException, JSONException {
        final JSONObject contentTypeObject = new JSONObject();

        final String url = hostName + "/api/v1/contenttype" + "?orderby=modDate&direction=DESC&per_page=100&" + "filter=" + URLEncoder.encode(ContentTypeName, "UTF-8").replace("+", "%20");

        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet request = new HttpGet(url);
            request.addHeader("authorization", dotCMSAPIKey);
            request.addHeader("content-type", "application/json");
            request.addHeader("accept","application/json");
            HttpResponse response = httpClient.execute(request);
        
            final String jsonString = new String(IOUtils.toByteArray(response.getEntity().getContent()));
		    final JSONObject responseBody = new JSONObject(jsonString);

            if(responseBody.has("entity")) {
                final JSONArray entities = responseBody.getJSONArray("entity");
                if(entities.length() > 0) {
                    final JSONObject entity = entities.getJSONObject(0);
                    final JSONObject contentTypeFields = this.getContentTypeFields(entity);
                    final String contentTypeVariableName = entity.optString("variable", "");

                    contentTypeObject.put("variableName", contentTypeVariableName);
                    contentTypeObject.put("fields", contentTypeFields);
                }
            }
        }
        catch (Exception ex) {
            Logger.error(this, "Unable to process Content Type variable name request");
            Logger.error(this, ex.getMessage());
        }

        return contentTypeObject;
    }

    /**
     * Obtains all the fields associated with the dotCMS content type
     * @param entity Single dotCMS content type object
     * @return JSONObject Contains the fields associated with the dotCMS content type
     * @throws JSONException
     */
    private final JSONObject getContentTypeFields(JSONObject entity) throws JSONException {
        final JSONObject contentTypeFields = new JSONObject();

        try {
            // If the entity contains the layout, then only iterate over it
            if(entity.has("layout")) {
                final JSONArray layouts = entity.getJSONArray("layout");

                // Iterate over the Layouts
                for(int i=0; i<layouts.length(); i++) {
                    final JSONObject layout = layouts.getJSONObject(i);
                    if(layout.has("columns")) {
                        final JSONArray columns = layout.getJSONArray("columns");

                        // Iterate over the Columns
                        for(int j=0; j<columns.length(); j++) {
                            final JSONObject column = columns.getJSONObject(j);
                            if(column.has("fields")) {
                                final JSONArray fields = column.getJSONArray("fields");

                                // Iterate over the fields
                                for(int k=0; k<fields.length(); k++) {
                                    final JSONObject field = fields.getJSONObject(k);
                                    final String variableName = field.optString("variable", "");

                                    JSONObject contentTypeField = new JSONObject();

                                    // Obtain only the required fields from the content type object
                                    final String[] keys = {
                                        "dataType",
                                        "readOnly",
                                        "required",
                                        "searchable"
                                    };
                            
                                    for(String key : keys) {
                                        contentTypeField.put(key, field.get(key));
                                    }
                                    contentTypeField.put("variable", variableName);

                                    contentTypeFields.put(variableName, contentTypeField);
                                }
                            }

                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            Logger.error(this, "Unable to obtain content type fields");
            Logger.error(this, ex.getMessage());
        }

        return contentTypeFields;
    }
    
    /**
     * Obtains the content identifier of the content
     * @param hostName The dotCMS instance url
     * @param dotCMSAPIKey API Key to access the Content API
     * @param title The title of the content
     * @return String Content Identifier
    */
    public final String searchContentIdentifier(final String hostName, final String dotCMSAPIKey, final String title) {
        String contentIdentifier = "";
        try {
		    final JSONObject responseBody = this.obtainContentFromDotCMS(hostName, dotCMSAPIKey, title);

            if(responseBody.has("entity")) {
                final JSONObject entity = responseBody.getJSONObject("entity");

                if(entity.has("jsonObjectView")) {
                    final JSONObject jsonObjectView = entity.getJSONObject("jsonObjectView");
                    if(jsonObjectView.has("contentlets")) {
                        final JSONArray contentlets = jsonObjectView.getJSONArray("contentlets");
                        if(contentlets.length() > 0) {
                            for(int i=0; i< contentlets.length(); i++) {
                                final JSONObject contentlet = contentlets.getJSONObject(i);
                                
                                // Look for a extact match
                                if(contentlet.optString("title", "").equalsIgnoreCase(title)) {
                                    contentIdentifier = contentlet.optString("identifier", "");
                                    break;
                                }
                            }

                            // Since no exact matches were found, lets do a partial match
                            if(contentIdentifier.length() == 0) {
                                final JSONObject contentlet = contentlets.getJSONObject(0);
                                contentIdentifier = contentlet.optString("identifier", "");
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            contentIdentifier = "";
            Logger.error(this, "Unable to process content identifier request");
            Logger.error(this, ex.getMessage());
        }

        return contentIdentifier;
    }

    /**
     * Invokes dotCMS search API to obtain the most recent content.
     * If no content is present on the dotCMS it will return an empty list
     * @param hostName The dotCMS instance url
     * @param dotCMSAPIKey API Key to access the Content API
     * @param title The title of the content
     * @return JSONObject List of dotCMS objects
     * @throws JSONException
     */
    public final JSONObject obtainContentFromDotCMS(final String hostName, final String dotCMSAPIKey, final String title) throws JSONException {
        final String url = hostName + "/api/content/_search";
        JSONObject responseBody = new JSONObject("{}");

        try {
            String query = "";

            if(title.length() > 0) {
                query = query + "title:" + title + "*" + " ";
            }

            if(query.length() == 0 ){
                query = "*";
            }

            query = query.trim();

            JSONObject body = new JSONObject();
            body.put("query", query);
            body.put("sort", "modDate desc");
            body.put("limit", 10);
            body.put("offset", 0);

            HttpClient httpClient = HttpClients.createDefault();
            HttpPost request = new HttpPost(url);
            StringEntity params = new StringEntity(body.toString());
            request.addHeader("authorization", dotCMSAPIKey);
            request.addHeader("content-type", "application/json");
            request.addHeader("accept","application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);
        
            final String jsonString = new String(IOUtils.toByteArray(response.getEntity().getContent()));
		    responseBody = new JSONObject(jsonString);

        }
        catch (Exception ex) {
            Logger.error(this, "Unable to process search request");
            Logger.error(this, ex.getMessage());
        }

        return responseBody;
    }
    
    /**
     * Invokes the dotCMS crud API with save action
     * @param hostName The dotCMS instance url
     * @param dotCMSAPIKey API Key to access the Content API
     * @param dotCMSContent Partial payload needed to invoke the dotCMS crud API 
     * @param contentTypeObject Contains the dotCMS content type variable name and fields associated with it
     * @return String Empty string on success, else an errror message
     * @throws IOException
     * @throws JSONException
     */
    public final String saveOperation(final String hostName, final String dotCMSAPIKey, final JSONObject dotCMSContent, final JSONObject contentTypeObject) throws IOException, JSONException {

        String apiResponse = "Unable to process save request";

        try {
            // Create the url title from the title of the of content
            final String defaultTitle = "Default Title";
            String urlTitle = dotCMSContent.optString("title", defaultTitle).replaceAll(" ", "-").replaceAll("\\s+", "");

            // Get current date time
            final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");  
            final LocalDateTime now = LocalDateTime.now(); 
            final String publishDate = dtf.format(now);

            // If it uses a default title then append current date time to it to make url-title unique 
            if(urlTitle.equals(defaultTitle.replaceAll(" ", "-").replaceAll("\\s+", ""))) {
                urlTitle = urlTitle + publishDate;
                urlTitle = urlTitle.replaceAll(" ", "-").replaceAll("\\s+", "");
            }

            final String contentTypeVariableName = contentTypeObject.optString("variableName", "");
            final JSONObject contentTypeFields = contentTypeObject.optJSONObject("fields");
            if(contentTypeFields == null) {
                throw new Exception("No content type fields found");
            }

            JSONObject contentletObject = new JSONObject();
            contentletObject.put("contentType", contentTypeVariableName);

            Iterator<?> contentTypeFieldKeys = contentTypeFields.keys();
            while(contentTypeFieldKeys.hasNext()) {
                final String fieldVariableName = (String) contentTypeFieldKeys.next();
                final JSONObject fieldObject = contentTypeFields.getJSONObject(fieldVariableName);

                final String fieldDataType = fieldObject.optString("dataType", "");
                final boolean isReadOnlyField = fieldObject.optBoolean("readOnly", false);

                if(dotCMSContent.has(fieldVariableName)) {
                    if(!isReadOnlyField) {
                        if(fieldDataType.equalsIgnoreCase("DATE")) {
                            ContentParser contentParser = new ContentParser();

                            final String dateText = dotCMSContent.getString(fieldVariableName);
                            final String fieldDate = contentParser.parseDate(dateText);
                            
                            // Place in the JSON only if it is a valid date
                            if(fieldDate.length() > 0) {
                                contentletObject.put(fieldVariableName, fieldDate);
                            }
                        }
                        else {
                            contentletObject.put(fieldVariableName, dotCMSContent.getString(fieldVariableName));
                        }
                    }
                }
            }

            JSONObject body = new JSONObject();
            body.put("actionName", "save");
            body.put("contentlet", contentletObject);
            body.put("comments", ResourceUtil.zapierComment);

            apiResponse = this.apiOperation(hostName, dotCMSAPIKey, body);
        }
        catch (Exception ex) {
            apiResponse = "Unable to process save request";
            Logger.error(this, "Unable to process save request");
            Logger.error(this, ex.getMessage());
        }

        return apiResponse;
    }

    /**
     * Invokes the dotCMS crud API with edit action
     * @param hostName The dotCMS instance url
     * @param dotCMSAPIKey API Key to access the Content API
     * @param contentIdentifier Unique id for the content on dotCMS
     * @param dotCMSContent Partial payload needed to invoke the dotCMS crud API
     * @param contentTypeObject Contains the dotCMS content type variable name and fields associated with it
     * @return String Empty string on success, else an errror message
     * @throws IOException
     * @throws JSONException
     */
    public final String editOperation(final String hostName, final String dotCMSAPIKey, final String contentIdentifier, final JSONObject dotCMSContent, final JSONObject contentTypeObject) throws IOException, JSONException {
        String apiResponse = "Unable to process edit request";

        try {
            final JSONObject contentTypeFields = contentTypeObject.optJSONObject("fields");
            if(contentTypeFields == null) {
                throw new Exception("No content type fields found");
            }

            JSONObject contentletObject = new JSONObject();
            contentletObject.put("identifier", contentIdentifier);

            Iterator<?> contentTypeFieldKeys = contentTypeFields.keys();
            while(contentTypeFieldKeys.hasNext()) {
                final String fieldVariableName = (String) contentTypeFieldKeys.next();
                final JSONObject fieldObject = contentTypeFields.getJSONObject(fieldVariableName);

                final String fieldDataType = fieldObject.optString("dataType", "");
                final boolean isReadOnlyField = fieldObject.optBoolean("readOnly", false);

                if(dotCMSContent.has(fieldVariableName)) {
                    if(!isReadOnlyField) {
                        if(fieldDataType.equalsIgnoreCase("DATE")) {
                            ContentParser contentParser = new ContentParser();

                            final String dateText = dotCMSContent.getString(fieldVariableName);
                            final String fieldDate = contentParser.parseDate(dateText);
                            
                            // Place in the JSON only if it is a valid date
                            if(fieldDate.length() > 0) {
                                contentletObject.put(fieldVariableName, fieldDate);
                            }
                        }
                        else {
                            contentletObject.put(fieldVariableName, dotCMSContent.getString(fieldVariableName));
                        }
                    }
                }
            }
        
            JSONObject body = new JSONObject();
            body.put("actionName", "save");
            body.put("contentlet", contentletObject);
            body.put("comments", ResourceUtil.zapierComment);

            apiResponse = this.apiOperation(hostName, dotCMSAPIKey, body);
        }
        catch (Exception ex) {
            apiResponse = "Unable to process edit request";
            Logger.error(this, "Unable to process edit request");
            Logger.error(this, ex.getMessage());
        }

        return apiResponse;
    }

    /**
     * Invokes the dotCMS crud API with either publish, unpublish, archive or unarchive action
     * @param hostName The dotCMS instance url
     * @param dotCMSAPIKey API Key to access the Content API
     * @param contentIdentifier Unique id for the content on dotCMS
     * @param triggerName Name of the trigger for which the workflow is to be triggered
     * @return String Empty string on success, else an errror message
     * @throws IOException
     * @throws JSONException
     */
    public final String workflowOperation(final String hostName, final String dotCMSAPIKey, final String contentIdentifier, final String triggerName) throws IOException, JSONException {
        String apiResponse = "Unable to process workflow request";

        try {
            final String url = hostName + "/api/v1/workflow/actions/fire" + "?identifier=" + contentIdentifier; 

            JSONObject body = new JSONObject();
            body.put("actionName", triggerName);
            body.put("comments", ResourceUtil.zapierComment);

            HttpClient httpClient = HttpClients.createDefault();

            HttpPut request = new HttpPut(url);
            StringEntity params = new StringEntity(body.toString());
            request.addHeader("authorization", dotCMSAPIKey);
            request.addHeader("content-type", "application/json");
            request.addHeader("accept", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);

            if(response.getStatusLine().getStatusCode() == 200) {
                apiResponse = "";
            }
            else {
                final String jsonString = new String(IOUtils.toByteArray(response.getEntity().getContent()));
                final JSONObject responseBody = new JSONObject(jsonString);

                if(responseBody.has("message")) {
                    apiResponse = responseBody.optString("message", "");
                }
            }
        }
        catch (Exception ex) {
            apiResponse = "Unable to process workflow request";
            Logger.error(this, "Unable to process workflow request");
            Logger.error(this, ex.getMessage());
        }

        return apiResponse;
    }

    /**
     * Invokes the dotCMS crud API
     * @param hostName The dotCMS instance url
     * @param dotCMSAPIKey API Key to access the Content API
     * @param body Payload to an API request
     * @return String Empty string on success, else an errror message
     * @throws IOException
     * @throws JSONException
     */
    private final String apiOperation(final String hostName, final String dotCMSAPIKey, final JSONObject body) throws IOException, JSONException {
        String apiResponse = "Unable to process dotCMS API";

        Logger.info(this, "CRUD API Payload" + body.toString());

        try {
            final String url = hostName + "/api/v1/workflow/actions/fire";

            HttpClient httpClient = HttpClients.createDefault();

            HttpPut request = new HttpPut(url);
            StringEntity params = new StringEntity(body.toString());
            request.addHeader("authorization", dotCMSAPIKey);
            request.addHeader("content-type", "application/json");
            request.addHeader("accept", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);

            if(response.getStatusLine().getStatusCode() == 200) {
                apiResponse = "";
            }
            else {
                final String jsonString = new String(IOUtils.toByteArray(response.getEntity().getContent()));
                final JSONObject responseBody = new JSONObject(jsonString);
    
                if(responseBody.has("errors")) {
                    final JSONArray errors = responseBody.getJSONArray("errors");
                    if(errors.length() > 0) {
                        final JSONObject error = errors.getJSONObject(0);
                        apiResponse = error.optString("message", "");
                    }
                }
            }
        }
        catch (Exception ex) {
            apiResponse = "Unable to process dotCMS API";
            Logger.error(this, "Unable to send invoke dotCMS API");
            Logger.error(this, ex.getMessage());
        }

        return apiResponse;        
    }

    /**
     * Invokes the REST API url shared by Zapier at the time of publishing the Zap
     * @param url Zapier REST API url
     * @param body Payload to be sent to Zapier
     * @return boolean Indicating the Rest action was successful or not
     */
    public boolean publishToZapier(final String url, final JSONObject body) {
        boolean apiResponse = false;

        try {
            HttpClient httpClient = HttpClients.createDefault();

            HttpPut request = new HttpPut(url);
            StringEntity params = new StringEntity(body.toString());
            request.addHeader("content-type", "application/json");
            request.addHeader("accept", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);

            if(response.getStatusLine().getStatusCode() == 200) {
                apiResponse = true;
            }
        }
        catch (Exception ex) {
            apiResponse = false;
            Logger.error(this, "Unable to invoke Zapier publish action");
            Logger.error(this, ex.getMessage());
        }

        return apiResponse;
    }

    /**
     * Generates the dotCMS object which needs to be sent out to Zapier. 
     * It contains a subset of all the keys available on the dotCMS content object
     * @param json Raw contentlet object 
     * @return JSONObject dotCMS content api object to be sent to Zapier
     * @throws JSONException
     */
    public final JSONObject prepareZapierObject(final JSONObject json) throws JSONException {
        JSONObject temp = new JSONObject();
        
        final String[] keys = {
            "identifier",
            "hostName",
            "url",
            "contentType",
            "title",
            "modUserName",
            "owner",
            "archived",
            "working",
            "locked",
            "live",
            "modDate"
        };

        for(String key : keys) {
            temp.put(key, json.get(key));
        }
        temp.put("id", json.optString("identifier", ""));
        
        return temp;
    }

    /**
     * Generates the absolute contentlet url with the instance url
     * @param hostName The dotCMS instance url
     * @param contentletUrl Relative url associated with the contentlet
     * @return String containing Absolute contentlet url
    */
    public final String prepareContentletUrl(final String hostName, final String contentletUrl) {
        final String url = hostName + "/dotAdmin/#/c" + contentletUrl.replace(".", "/");
        return url;
    }

    /**
     * Writes data to a JSON file
     * @param jsonObject Data to be writted to file
     */
    public final void writeJSON(JSONObject jsonObject) {
        try {
            BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
            File filePath = bundleContext.getDataFile(ResourceUtil.webhookUrls);

            FileWriter file = new FileWriter(filePath);
            file.write(jsonObject.toString());
            file.close();
            Logger.info(this, "Data written to JSON");
         } catch (Exception e) {
            e.printStackTrace();
            Logger.error(this, "Write error");
            Logger.error(this, e.getMessage());
         }
    }

    /**
     * Reads a JSON file
     * @return JSONObject JSON data read from the file
     */
    public final JSONObject readJSON() {
        JSONObject result = new JSONObject();
        try {
            BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
            File filePath = bundleContext.getDataFile(ResourceUtil.webhookUrls);

            File file = new File(filePath.toPath().toAbsolutePath().toString());
            
            // Read the JSON data from file if it exists
            if(file.exists()) {
                FileInputStream fileInputStream = new FileInputStream(filePath);
                byte[] data = new byte[(int) file.length()];
                fileInputStream.read(data);
                fileInputStream.close();
    
                String jsonString = new String(data);
                result = new JSONObject(jsonString);
            }
            else {
                Logger.info(this, "No Zapier action file exists");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Logger.error(this, "Read error");
            Logger.error(this, e.getMessage());
        }
        return result;
    }
}
