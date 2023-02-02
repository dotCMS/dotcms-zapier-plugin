package com.dotcms.plugin.dotzapier.zapier.app;

import com.dotcms.plugin.dotzapier.util.Constants;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;
import io.vavr.Tuple;
import io.vavr.control.Try;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dotcms.security.apps.Secret.newSecret;

/**
 * This API retrieves the Zapier App configuration.
 * @author jsanca
 */
public class ZapierAppAPI {

    public final static String APP_KEY = Constants.DOT_ZAPIER_APP_KEY;
    public final static String APP_NAME = "zapName";
    public final static String ALLOWED_TYPES = "allowedContentTypes";

    /**
     * Unregister a zap into the app config for the system host
     */
    public void unregisterZap(final String triggerUrl) {

        unregisterZap(APILocator.systemHost(), triggerUrl);
    }

    /**
     * Unregister a zap into the app config for the system host
     */
    public void unregisterZap(final Host site, final String triggerName) {

        try {

            Logger.debug(this.getClass().getName(), ()-> "Deleting the secret: " + triggerName);
            APILocator.getAppsAPI().deleteSecret(APP_KEY, new HashSet<>(Arrays.asList(triggerName)), site, APILocator.systemUser());
        } catch (Exception e) {

            Logger.error(this.getClass().getName(), e.getMessage(), e);
        }
    }

    /**
     * Gets the {@link ZapierApp} for system host
     */
    public void registerZap(final String triggerName, final String url) {

        registerZap(APILocator.systemHost(), triggerName, url);
    }

    /**
     * Register a zap into the app config for the system host
     */
    public void registerZap(final Host site, final String triggerName, final String url) {

        try {

            final Secret secret = newSecret(triggerName.toCharArray(), Type.STRING, false);
            Logger.debug(this.getClass().getName(), ()-> "Saving the secret: " + triggerName + " - " + url);
            APILocator.getAppsAPI().saveSecret(APP_KEY, Tuple.of(url, secret), site, APILocator.systemUser());
        } catch (Exception e) {

            Logger.error(this.getClass().getName(), e.getMessage(), e);
        }
    }


    /**
     * Gets the {@link ZapierApp} for system host
     * @return ZapierApp
     */
    public Optional<ZapierApp> config() {

        return config(APILocator.systemHost());
    }

    /**
     * Gets the secrets from the App - this will check the current host then the SYSTEM_HOST for a
     * valid configuration. This lookup is low overhead and cached by dotCMS.
     *
     * @param host
     * @return Optional {@link ZapierApp}
     */
    public Optional<ZapierApp> config(final Host host) {

        final Optional<AppSecrets> appSecrets = Try.of(
                        () -> APILocator.getAppsAPI().getSecrets(APP_KEY, true, host, APILocator.systemUser()))
                .getOrElse(Optional.empty());

        if (!appSecrets.isPresent()) {

            Logger.debug(this.getClass().getName(), ()-> "App secrets is empty for host: " + host.getHostname());
            return Optional.empty();
        }

        final Map<String, Secret> secrets = appSecrets.get().getSecrets();
        final String appName   = Try.of(()->secrets.get(APP_NAME).getString()).getOrElse(StringPool.BLANK);
        final String allowedTypesValue = Try.of(()->secrets.get(ALLOWED_TYPES).getString()).getOrElse(StringPool.BLANK);

        Logger.debug(this.getClass().getName(), ()-> "appName: " + appName);
        Logger.debug(this.getClass().getName(), ()-> "allowedTypesValue: " + allowedTypesValue);
        final Set<String> allowedApps = UtilMethods.isSet(allowedTypesValue)?
                Stream.of(allowedTypesValue.split(StringPool.COMMA)).map(String::trim).collect(Collectors.toSet()) :
                Collections.emptySet();
        final Map<String, String> zapsRegisterMap = new HashMap<>();
        for (final Map.Entry<String, Secret> zapRegisterEntry : secrets.entrySet()) {

            if (!zapRegisterEntry.getKey().equals(APP_NAME) || !zapRegisterEntry.getKey().equals(ALLOWED_TYPES)) {

                Logger.debug(this.getClass().getName(), ()-> zapRegisterEntry.getKey() + ": " + zapRegisterEntry.getValue().toString());
                zapsRegisterMap.put(zapRegisterEntry.getKey(), zapRegisterEntry.getValue().getString());
            }
        }

        final ZapierApp config = new ZapierApp(
                appName, allowedApps, zapsRegisterMap);

        return Optional.ofNullable(config);
    }
}
