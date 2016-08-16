package com.focusit.jsflight.player.config;

/**
 * Player configuration holder
 * Created by dkirpichenkov on 06.05.16.
 */
public class Configuration
{
    private CommonConfiguration commonConfiguration = new CommonConfiguration();
    private JMeterConfiguration jMeterConfiguration = new JMeterConfiguration();
    private ScriptEventConfiguration scriptEventConfiguration = new ScriptEventConfiguration();
    private WebConfiguration webConfiguration = new WebConfiguration();

    public CommonConfiguration getCommonConfiguration()
    {
        return commonConfiguration;
    }

    public void setCommonConfiguration(CommonConfiguration commonConfiguration)
    {
        this.commonConfiguration = commonConfiguration;
    }

    public JMeterConfiguration getjMeterConfiguration()
    {
        return jMeterConfiguration;
    }

    public void setjMeterConfiguration(JMeterConfiguration jMeterConfiguration)
    {
        this.jMeterConfiguration = jMeterConfiguration;
    }

    public ScriptEventConfiguration getScriptEventConfiguration()
    {
        return scriptEventConfiguration;
    }

    public void setScriptEventConfiguration(ScriptEventConfiguration scriptEventConfiguration)
    {
        this.scriptEventConfiguration = scriptEventConfiguration;
    }

    public WebConfiguration getWebConfiguration()
    {
        return webConfiguration;
    }

    public void setWebConfiguration(WebConfiguration webConfiguration)
    {
        this.webConfiguration = webConfiguration;
    }
}
