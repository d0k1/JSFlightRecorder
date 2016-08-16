package com.focusit.jsflight.player.config;

/**
 * Created by dkirpichenkov on 06.05.16.
 */
public class ScriptEventConfiguration
{

    private String filename = "";

    private String script = "";

    public String getFilename()
    {
        return filename;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public String getScript()
    {
        return script;
    }

    public void setScript(String script)
    {
        this.script = script;
    }

    public void loadDefaults()
    {

    }
}
