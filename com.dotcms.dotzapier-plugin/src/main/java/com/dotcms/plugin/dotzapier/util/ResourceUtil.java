package com.dotcms.plugin.dotzapier.util;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
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

/**
 * Contains the utility methods for dotZapier resource
 */
public class ResourceUtil {

    private static final String zapierComment = "Performed by Zapier";

    /**
     * Invokes the REST API url shared by Zapier at the time of publishing the Zap
     * @param url Zapier REST API url
     * @param body Payload to be sent to Zapier
     * @return boolean Indicating the Rest action was successful or not
     */
    public boolean publishToZapier(final String url, final JSONObject body) {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut request = new HttpPut(url);
            final StringEntity params = new StringEntity(body.toString(), ContentType.APPLICATION_FORM_URLENCODED);
            request.addHeader("content-type", "application/json");
            request.addHeader("accept", "application/json");
            request.setEntity(params);
            final HttpResponse response = httpClient.execute(request);
            return response.getStatusLine().getStatusCode() == 200;
        }  catch (Exception ex) {

            Logger.error(this, "Unable to invoke Zapier publish action");
            Logger.error(this, ex.getMessage());
        }

        return false;
    }

    public boolean publishToZapier(final String url, final JSONArray body) {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            final HttpPut request = new HttpPut(url);
            final StringEntity params = new StringEntity(body.toString(), ContentType.APPLICATION_FORM_URLENCODED);
            request.addHeader("content-type", "application/json");
            request.addHeader("accept", "application/json");
            request.setEntity(params);
            final HttpResponse response = httpClient.execute(request);
            return response.getStatusLine().getStatusCode() == 200;
        }  catch (Exception ex) {

            Logger.error(this, "Unable to invoke Zapier publish action");
            Logger.error(this, ex.getMessage());
        }

        return false;
    }

    /**
     * Obtains the contentlet data from content identifier via dotCMS API
     * @param hostName The dotCMS instance url
     * @param contentIdentifier Unique id for the content on dotCMS
     * @return JSONObject Contains the contentlet data
    */
    public JSONObject getContentletData(final String hostName, final String contentIdentifier) {
        JSONObject contentletData = new JSONObject();

        try {
            final String url = hostName + "/api/content/id/" + contentIdentifier;

            HttpClient httpClient = HttpClients.createDefault();

            HttpGet request = new HttpGet(url);
            request.addHeader("content-type", "application/json");
            request.addHeader("accept", "application/json");
            HttpResponse response = httpClient.execute(request);

            if(response.getStatusLine().getStatusCode() == 200) {
                final String jsonString = new String(IOUtils.toByteArray(response.getEntity().getContent()));
		        final JSONObject responseBody = new JSONObject(jsonString);
                
                // Extract the contentlet data from the api response
                if(responseBody.has("contentlets")) {
                    final JSONArray contentlets = responseBody.getJSONArray("contentlets");
                    if(contentlets.length() > 0) {
                        contentletData = contentlets.getJSONObject(0);
                    }
                }
            }
        }
        catch (Exception ex) {
            contentletData = new JSONObject();
            Logger.error(this, "Unable to obtain contentlet data");
            Logger.error(this, ex.getMessage());
        }

        return contentletData;
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
}
