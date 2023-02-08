package com.dotcms.plugin.dotzapier.zapier.app;

import com.dotcms.plugin.dotzapier.util.Constants;
import com.dotcms.security.apps.AppSecrets;
import com.dotcms.security.apps.Secret;
import com.dotcms.security.apps.Type;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.liferay.util.StringUtil;
import io.vavr.Tuple;
import io.vavr.control.Try;
import org.apache.commons.lang3.RandomUtils;

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
    public void registerZap(final String triggerName, final String url, final String type) {

        registerZap(APILocator.systemHost(), type, triggerName, url);
    }

    /**
     * Register a zap into the app config for the system host
     */
    public void registerZap(final Host site, final String type, final String triggerName, final String url) {

        try {

            final User systemUser   = APILocator.systemUser();
            final String webHookKey = this.generateWebHookKey(site, type, systemUser, url);
            final Secret secret     = newSecret(url.toCharArray(), Type.STRING, false);
            Logger.debug(this.getClass().getName(), ()-> "Saving the secret: " + triggerName + " - " + url);
            APILocator.getAppsAPI().saveSecret(APP_KEY, Tuple.of(webHookKey, secret), site, systemUser);
        } catch (Exception e) {

            Logger.error(this.getClass().getName(), e.getMessage(), e);
        }
    }

    /**
     * Returns the web
     * @param site
     * @param type
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public Set<String> getWebHookUrlsPerType (final Host site, final String type, final User user) throws DotDataException, DotSecurityException {

        final Optional<AppSecrets> appSecrets = APILocator.getAppsAPI().getSecrets(APP_KEY, true, site, user);
        if (appSecrets.isPresent()) {

            return appSecrets.get().getSecrets().entrySet().stream().filter(entry -> entry.getKey().contains(type))
                    .map(entry -> entry.getValue().getString()).collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }

    private String generateWebHookKey (final Host site, final String type, final User user, final String url) throws DotDataException, DotSecurityException {

        String webHookKey = type + StringPool.PERIOD + "webhookurl";
        final Optional<AppSecrets> appSecrets = APILocator.getAppsAPI().getSecrets(APP_KEY, true, site, user);
        if (appSecrets.isPresent()) {

            if (appSecrets.get().getSecrets().containsKey(webHookKey)) {

                final String uuid = Try.of(()-> StringUtil.extractDigits(url).substring(0, 4))
                        .getOrElse(String.valueOf(RandomUtils.nextInt(0, Integer.MAX_VALUE)));

                webHookKey = StringPool.PERIOD + uuid;
            }
        }

        Logger.info(this, "webHookKey: " + webHookKey);
        return webHookKey;
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
        final String allowedTypesValue = Try.of(()->secrets.get(ALLOWED_TYPES).getString()).getOrElse(StringPool.BLANK);

        Logger.debug(this.getClass().getName(), ()-> "allowedTypesValue: " + allowedTypesValue);
        final Set<String> allowedApps = UtilMethods.isSet(allowedTypesValue)?
                Stream.of(allowedTypesValue.split(StringPool.COMMA)).map(String::trim).collect(Collectors.toSet()) :
                Collections.emptySet();
        final Map<String, String> zapsRegisterMap = new HashMap<>();
        for (final Map.Entry<String, Secret> zapRegisterEntry : secrets.entrySet()) {

            if (!zapRegisterEntry.getKey().equals(ALLOWED_TYPES)) {

                Logger.debug(this.getClass().getName(), ()-> zapRegisterEntry.getKey() + ": " + zapRegisterEntry.getValue().toString());
                zapsRegisterMap.put(zapRegisterEntry.getKey(), zapRegisterEntry.getValue().getString());
            }
        }

        final ZapierApp config = new ZapierApp(allowedApps, zapsRegisterMap);

        return Optional.ofNullable(config);
    }

    /***
     * Returns true if all content types are allowed
     * @param allowedContentTypes
     * @return
     */
    public boolean isAllowedAllContentTypes(final Set<String> allowedContentTypes) {
        return !UtilMethods.isSet(allowedContentTypes) || allowedContentTypes.contains("*");
    }
}
