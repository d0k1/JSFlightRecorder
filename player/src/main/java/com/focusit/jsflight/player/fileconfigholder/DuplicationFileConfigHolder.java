package com.focusit.jsflight.player.fileconfigholder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.focusit.jsflight.player.config.WebConfiguration;

public class DuplicationFileConfigHolder extends UIFileConfigHolder
{
    private static final long serialVersionUID = 5900843806336550183L;

    private static final DuplicationFileConfigHolder INSTANCE = new DuplicationFileConfigHolder();
    private WebConfiguration configuration;

    public static DuplicationFileConfigHolder getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void load(String file) throws Exception
    {
        ObjectInputStream inputStream = getInputStream(file);
        configuration.setDuplicationScriptFilename((String)inputStream.readObject());
        configuration.setDuplicationScript((String)inputStream.readObject());
    }

    @Override
    public void store(String file) throws Exception
    {
        ObjectOutputStream outputStream = getOutputStream(file);
        outputStream.writeObject(configuration.getDuplicationScriptFilename());
        outputStream.writeObject(configuration.getDuplicationScript());
    }

    @Override
    protected String getControllerDataFilename()
    {
        return "duplicateHandler";
    }

    public WebConfiguration getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration(WebConfiguration configuration)
    {
        this.configuration = configuration;
    }
}
