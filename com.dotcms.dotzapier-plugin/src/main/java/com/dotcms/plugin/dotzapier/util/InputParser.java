package com.dotcms.plugin.dotzapier.util;

import java.util.Map;

/**
 * Defines the Input Parser for a content format
 * @author jsanca
 */
public interface InputParser {

    /**
     * Parse the string to a map depending on the format target
     * @param content String
     * @return Map with the prop name values
     * @throws Exception
     */
    Map<String, Object> parse(final String content) throws Exception;
}
