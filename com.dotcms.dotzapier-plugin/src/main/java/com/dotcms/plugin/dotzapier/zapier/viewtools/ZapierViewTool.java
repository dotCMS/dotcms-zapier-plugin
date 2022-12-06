package com.dotcms.plugin.dotzapier.zapier.viewtools;

import com.dotcms.plugin.dotzapier.zapier.content.ContentAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import org.apache.velocity.tools.view.tools.ViewTool;

public class ZapierViewTool implements ViewTool {

    private final ContentAPI contentAPI = new ContentAPI();

    @Override
    public void init(Object initData) {

    }

    public String getEditContentletURL (final Contentlet contentlet) {

        return contentAPI.generateEditContentletURL(contentlet);
    }
}
