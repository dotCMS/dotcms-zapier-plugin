package com.dotcms.plugin.dotzapier.zapier.app;

import java.util.Map;
import java.util.Set;

/**
 * This encapsulates the zappier app config for a host
 * @author jsanca
 */
public class ZapierApp {

    private final String name;
    private final Set<String> allowedApps;
    private final int maxAllowedApps;
    private final Map<String, String> zapsRegisterMap;

    public ZapierApp(String name, Set<String> allowedApps, int maxAllowedApps, Map<String, String> zapsRegisterMap) {
        this.name = name;
        this.allowedApps = allowedApps;
        this.maxAllowedApps = maxAllowedApps;
        this.zapsRegisterMap = zapsRegisterMap;
    }

    public Set<String> getAllowedApps() {
        return allowedApps;
    }

    public int getMaxAllowedApps() {
        return maxAllowedApps;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getZapsRegisterMap() {
        return zapsRegisterMap;
    }
}
