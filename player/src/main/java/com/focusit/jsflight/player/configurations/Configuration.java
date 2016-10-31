package com.focusit.jsflight.player.configurations;

import com.focusit.jsflight.player.configurations.interfaces.IDefaults;

/**
 * Player configuration holder
 * Created by dkirpichenkov on 06.05.16.
 */
public class Configuration implements IDefaults
{
    private CommonConfiguration commonConfiguration = new CommonConfiguration();
    private ScriptsConfiguration scriptsConfiguration = new ScriptsConfiguration();
    private WebConfiguration webConfiguration = new WebConfiguration();

    public CommonConfiguration getCommonConfiguration()
    {
        return commonConfiguration;
    }

    public WebConfiguration getWebConfiguration()
    {
        return webConfiguration;
    }

    public ScriptsConfiguration getScriptsConfiguration()
    {
        return scriptsConfiguration;
    }

    @Override
    public void loadDefaults() {
        commonConfiguration.loadDefaultMaxElementScript();
        scriptsConfiguration.loadDefaults();
        webConfiguration.loadDefaults();
    }
}
