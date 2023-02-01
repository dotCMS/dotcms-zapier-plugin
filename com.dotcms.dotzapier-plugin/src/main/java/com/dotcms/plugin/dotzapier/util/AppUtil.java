package com.dotcms.plugin.dotzapier.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.commons.io.IOUtils;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.util.ConfigUtils;
import com.dotmarketing.util.Logger;

/**
 * Contains the utility methods to copy and delete the App yaml file to the App directory.
 * Also refreshs the Apps.
 */
public class AppUtil {
    /**
     * Name of the YAML file which identifies the App
    */
    private final File installedAppYaml = new File(ConfigUtils.getAbsoluteAssetsRootPath() 
                                                    + File.separator + "server"
                                                    + File.separator + "apps" 
                                                    + File.separator + Constants.DOT_ZAPIER_APP_KEY 
                                                    + ".yml"
                                                );

    /**
     * Copies the App yaml to the Apps directory and refreshes the Apps
     */
    public void copyAppYml() throws IOException {
        Logger.info(this.getClass().getName(), "Copying YAML File:" + installedAppYaml);
        try (final InputStream in = this.getClass().getResourceAsStream("/" + Constants.DOT_ZAPIER_APP_KEY + ".yml")) {
            IOUtils.copy(in, Files.newOutputStream(installedAppYaml.toPath()));
        }
        CacheLocator.getAppsCache().clearCache();
    }

    /**
     * Deletes the App yaml to the Apps directory and refreshes the Apps
     */
    public void deleteYml() throws IOException {
        Logger.info(this, "Deleting the YAML File:" + installedAppYaml);

        installedAppYaml.delete();
        CacheLocator.getAppsCache().clearCache();
    }
}
