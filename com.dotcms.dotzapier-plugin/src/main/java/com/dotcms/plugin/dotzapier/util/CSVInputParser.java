package com.dotcms.plugin.dotzapier.util;

import com.liferay.util.StringPool;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Very basic comma separated csv parser
 * expect a line such as
 * fieldVariableName1=Value1,fieldVariableName2=Value2,fieldVariableName2=Value2,...
 */
public class CSVInputParser implements InputParser {

    @Override
    public Map<String, Object> parse(final String content) throws Exception {

        final Map<String, Object> contentMap = new HashMap<>();
        final String [] sentences = content.split(StringPool.COMMA);

        for (final String sentence : sentences) {

            final String sentenceTrimmed = sentence.trim();
            final String[] splitValues   = sentenceTrimmed.split("=");

            // Trim the values obtained
            for (int i = 0; i < splitValues.length; i++) {
                splitValues[i] = splitValues[i].trim();
            }

            if(splitValues.length == 2) {

                final String key = splitValues[0];
                final String longText = splitValues[1];
                final String value = this.extractStringBetweenQuotes(longText);
                contentMap.put(key, value.replace("\"", "").trim());
            }
        }

        return contentMap;
    }

    /**
     * Extracts the string from the given text
     * @param text Text received from Zapier
     * @return String[] List of strings
     */
    private String extractStringBetweenQuotes(final String text) {
        final StringBuilder result = new StringBuilder();

        final String regex = "(\\\"[a-zA-Z0-9 :;,.!?_@#$%^&+\\-=*<>\\{\\}()\\[\\]]+\\\")";

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(text);

        if(matcher.find()) {

            for (int i = 1; i <= matcher.groupCount(); i++) {
                result.append(matcher.group(i)).append(StringPool.SPACE);
            }
        } else {
            result.append(text);
        }

        return result.toString();
    }

}
