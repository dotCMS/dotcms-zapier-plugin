/**
 * Contains the utility methods to parse the text coming from Zapier
*/

package com.dotcms.plugin.dotzapier.util;

import com.dotcms.plugin.dotzapier.zapier.content.ContentAPI;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the providers for content parsers
 * Json or csv by now
 */
public class ContentParser {

    private final Map<String, InputParser> inputParserMap = this.getInputParserMap();

    private Map<String, InputParser> getInputParserMap() {

        final Map<String, InputParser> map = new HashMap<>();

        map.put(ContentAPI.JSON_FORMAT, new JsonInputParser());
        map.put(ContentAPI.CSV_FORMAT, new CSVInputParser());

        return map;
    }

    /**
     * Parses the content received from Zapier, it should be a json with the contentlet properties to add
     * @param content Text received from Zapier
     * @return Map Parsed data
     * @throws JsonProcessingException
     */
    public final Map<String, Object> parseContent(final String inputFormat, final String content) throws Exception {

        return this.inputParserMap.getOrDefault(inputFormat, inputParserMap.get(ContentAPI.JSON_FORMAT)).parse(content);
    }

}
