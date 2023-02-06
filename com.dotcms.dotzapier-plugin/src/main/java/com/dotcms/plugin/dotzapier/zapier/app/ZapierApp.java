package com.dotcms.plugin.dotzapier.zapier.app;

import java.util.Map;
import java.util.Set;

/**
 * This encapsulates the zappier app config for a host
 * @author jsanca
 */
public class ZapierApp {

    private final Set<String> allowedContentTypes;
    private final Map<String, String> zapsRegisterMap;

    public ZapierApp(final Set<String> allowedContentTypes,  final Map<String, String> zapsRegisterMap) {
        this.allowedContentTypes = allowedContentTypes;
        this.zapsRegisterMap = zapsRegisterMap;
    }

    public Set<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }


    public Map<String, String> getZapsRegisterMap() {
        return zapsRegisterMap;
    }
}
