package com.focusit.jsflight.player.fileconfigholder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.focusit.jsflight.player.config.WebConfiguration;

public class WebLookupFileConfigHolder extends UIFileConfigHolder
{
    private static final long serialVersionUID = 1L;
    private final static WebLookupFileConfigHolder instance = new WebLookupFileConfigHolder();
    private WebConfiguration configuration;

    private WebLookupFileConfigHolder()
    {
    }

    public static WebLookupFileConfigHolder getInstance()
    {
        return instance;
    }

    @Override
    public void load(String file) throws Exception
    {
        ObjectInputStream stream = getInputStream(file);
        configuration.setLookupScript((String)stream.readObject());
        configuration.setLookupScriptFilename((String)stream.readObject());

        configuration.loadDefaults();
    }

    @Override
    public void store(String file) throws Exception
    {
        ObjectOutputStream stream = getOutputStream(file);
        stream.writeObject(configuration.getLookupScript());
        stream.writeObject(configuration.getLookupScriptFilename());
    }

    @Override
    protected String getControllerDataFilename()
    {
        return "weblookup";
    }

    public void setConfiguration(WebConfiguration configuration)
    {
        this.configuration = configuration;
    }
}
