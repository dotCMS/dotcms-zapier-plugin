package com.dotcms.plugin.dotzapier.util;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Json parser based on jackson mapper
 */
public class JsonInputParser implements InputParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());
    }
    @Override
    public Map<String, Object> parse(final String content) throws Exception {

        return objectMapper.readValue(content, Map.class);
    }
}
