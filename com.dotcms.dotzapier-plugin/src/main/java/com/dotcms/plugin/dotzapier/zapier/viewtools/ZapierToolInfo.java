package com.dotcms.plugin.dotzapier.zapier.viewtools;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

public class ZapierToolInfo extends ServletToolInfo {

    @Override
    public String getKey () {
        return "zapier";
    }

    @Override
    public String getScope () {
        return ViewContext.APPLICATION;
    }

    @Override
    public String getClassname () {
        return ZapierViewTool.class.getName();
    }

    @Override
    public Object getInstance (final Object initData ) {

        ZapierViewTool viewTool = new ZapierViewTool();
        viewTool.init(initData);

        setScope(ViewContext.APPLICATION);

        return viewTool;
    }
}
