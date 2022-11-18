/**
 * Contains the utility methods to parse the text coming from Zapier
*/

package com.dotcms.plugin.dotzapier.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;
import com.dotmarketing.util.json.JSONObject;
import com.dotmarketing.util.json.JSONException;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ContentParser { // todo: this one has to be changed to another format

    /**
     * Parses the content received from Zapier, it should be a json with the contentlet properties to add
     * @param content Text received from Zapier
     * @return Map Parsed data
     * @throws JsonProcessingException
     */
    public final Map<String, Object> parseJson(final String content) throws JsonProcessingException {

        return DotObjectMapperProvider.getInstance().getDefaultObjectMapper().readValue(content, Map.class);
    }
    /**
     * Parses the content received from Zapier
     * @param content Text received from Zapier
     * @return JSONObject Parsed data
     * @throws JSONException
     */
    public final JSONObject parseSlack(final String content) throws JSONException {
        JSONObject result = new JSONObject();

        final String[] cmdList = {
            "save",
            "edit",
            "publish",
            "unpublish",
            "archive",
            "unarchive",
            "delete",
            "destroy"
        };

        // Split String on basis of "#" and iterate over it
        final String[] sentences = content.split("#");
        for (String sentence : sentences) {
            sentence = sentence.trim();
            final String[] splitValues = sentence.split("=");

            // Trim the values obtained
            for (int i = 0; i < splitValues.length; i++) {
                splitValues[i] = splitValues[i].trim();
            }

            if(splitValues.length == 0) {
                continue;
            }
            else if(splitValues.length == 1) {
                String value = splitValues[0];
                Boolean isCmdFound = false;

                // Check if the command is present in the string or not
                // if it is not present then do not add any body as well
                for(String cmd : cmdList) {
                    if(value.contains(cmd)) {
                        result.put("actionName", cmd);
                        value = value.replace(cmd, "");
                        value = value.trim();
                        isCmdFound = true;
                        break;
                    }
                }

                if(isCmdFound && value.length() > 0) {
                    result.put("body", value);
                }
            }
            else if(splitValues.length == 2) {
                final String key = splitValues[0];
                final String longText = splitValues[1];
                String[] values = this.extractStringBetweenQuotes(longText);

                if(values.length == 2) {
                    result.put(key, values[0].replace("\"", "").trim());
                    String body = values[1].replace("\"", "").trim();
                    if(body.length() > 0) {
                        result.put("body", body);
                    }
                }
                else if(values.length == 1) {
                    result.put(key, values[0].replace("\"", "").trim());
                    String body = longText.replace(values[0].trim(), "").replace("\"", "").trim();
                    if(body.length() > 0) {
                        result.put("body", body);
                    }
                }
                else if(values.length == 0) {
                    String[] items = longText.split(" ");
                    if(items.length > 0) {
                        result.put(key, items[0].replace("\"", "").trim());
                        String body = longText.replace(items[0].replace("\"", "").trim(), "").replace("\"", "").trim();
                        if(body.length() > 0) {
                            result.put("body", body);
                        }
                    }
                }
            }
        }

        // Replace the id field with identifier for all operations
        if(result.has("id")) {
            result.put("identifier", result.optString("id", ""));
            result.remove("id");
        }
        
        return result;
    }

    /**
     * Extracts the string from the given text
     * @param text Text received from Zapier
     * @return String[] List of strings
     */
    private final String[] extractStringBetweenQuotes(final String text) {
        ArrayList<String> result = new ArrayList<String>();

        final String regex = "(\\\"[a-zA-Z0-9 :;,.!?_@#$%^&+\\-=*<>\\{\\}()\\[\\]]+\\\")";

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(text);

        if(matcher.find()) {

            for (int i = 1; i <= matcher.groupCount(); i++) {
                result.add(matcher.group(i));
            }
        }

        return result.toArray(new String[0]);
    }

    /**
     * Generates all possible supported date format strings 
     * @return String[] List of all supported date format strings
     */
    private final String[] generateAllDateFormats() {
        final String[] dateFormats = {
            "MM dd yy", 
            "dd MM yy", 
            "dd yy MM", 
            "MM yy dd", 
            "yy MM dd", 
            "yy dd MM",
            "MM dd yyyy", 
            "dd MM yyyy", 
            "dd yyyy MM", 
            "MM yyyy dd", 
            "yyyy MM dd", 
            "yyyy dd MM",
            "MMM dd yy",
            "dd MMM yy",
            "dd yy MMM",
            "MMM yy dd",
            "yy MMM dd",
            "yy dd MMM",
            "MMM dd yyyy",
            "dd MMM yyyy",
            "dd yyyy MMM",
            "MMM yyyy dd",
            "yyyy MMM dd",
            "yyyy dd MMM"
        };
        final char[] delimitters = { ' ', '-', '/', ':', '\\' };

        ArrayList<String> supportedDateFormats = new ArrayList<String>();

        for (char delimitter : delimitters) {
            for (String dateFormat : dateFormats) {
                String fmt = dateFormat.replace(' ', delimitter);
                
                // All possible time formats
                supportedDateFormats.add(fmt + " HH:mm:ss");
                supportedDateFormats.add(fmt + " HH:mm");
                supportedDateFormats.add(fmt + " HH");
                supportedDateFormats.add(fmt);
            }
        }

        return supportedDateFormats.toArray(new String[0]);
    }

    /**
     * Obtains the date in the desired format
     * @param dateFormat Format of the date
     * @param dateString Date text parsed from the Zapier content 
     * @return String Date String in the desired format else it will return an empty string
     */
    private final String obtainDateInDesiredFormat(String dateFormat, String dateString) {
        try {
            final String desiredFormat = "yyyy-MM-dd HH:mm:ss";

            SimpleDateFormat currentFormat = new SimpleDateFormat(dateFormat);
            currentFormat.setLenient(false);
    
            Date date = currentFormat.parse(dateString);
            String desiredDate = new SimpleDateFormat(desiredFormat).format(date);
            return desiredDate;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Parses the date string to the appropriate format
     * @param dateText Possible string formatted in different date formats
     * @return String Date string in yyyy-MM-dd HH:mm:ss format else it will return an empty string
     */
    public final String parseDate(final String dateText) {
        final String[] supportedDateFormats = this.generateAllDateFormats();

        for (String dateFormat : supportedDateFormats) {
            String formattedDate = this.obtainDateInDesiredFormat(dateFormat, dateText);
            if(formattedDate.length() > 0) {
                return formattedDate;
            }
        }
        return ""; 
    }
}
