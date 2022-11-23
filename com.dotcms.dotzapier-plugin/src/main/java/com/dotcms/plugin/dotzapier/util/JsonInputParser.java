package com.dotcms.plugin.dotzapier.util;

import com.dotcms.rest.api.v1.DotObjectMapperProvider;

import java.util.Map;

public class JsonInputParser implements InputParser {

    @Override
    public Map<String, Object> parse(final String content) throws Exception {

        return DotObjectMapperProvider.getInstance().getDefaultObjectMapper().readValue(content, Map.class);
    }
}
