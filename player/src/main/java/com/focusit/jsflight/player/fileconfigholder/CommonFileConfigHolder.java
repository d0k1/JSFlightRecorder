package com.focusit.jsflight.player.fileconfigholder;

import com.focusit.jsflight.player.configurations.CommonConfiguration;
import com.focusit.jsflight.player.constants.BrowserType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class CommonFileConfigHolder extends UIFileConfigHolder
{
    private static final long serialVersionUID = 1L;

    private final static CommonFileConfigHolder instance = new CommonFileConfigHolder();
    private CommonConfiguration configuration;

    private CommonFileConfigHolder()
    {
    }

    public static CommonFileConfigHolder getInstance()
    {
        return instance;
    }

    @Override
    public void load(String file) throws Exception
    {
        ObjectInputStream stream = getInputStream(file);
        configuration.loadDefaultMaxElementScript();

        configuration.setProxyHost((String)stream.readObject());
        configuration.setProxyPort((Integer) stream.readObject());
        configuration.setPathToBrowserExecutable((String)stream.readObject());
        configuration.setAsyncRequestsCompletedTimeoutInSeconds((Integer) stream.readObject());
        configuration.setMakeShots(stream.readBoolean());
        configuration.setBrowserType((BrowserType) stream.readObject());
        configuration.setUseRandomChars(stream.readBoolean());
        configuration.setScreenshotsDirectory((String)stream.readObject());
        configuration.setWebDriverTag((String)stream.readObject());
        configuration.setAsyncRequestsCompletedTimeoutInSeconds((Integer)stream.readObject());
        configuration.setFormOrDialogXpath((String)stream.readObject());
    }

    public void setConfiguration(CommonConfiguration configuration)
    {
        this.configuration = configuration;
    }

    @Override
    public void store(String file) throws Exception
    {
        ObjectOutputStream stream = getOutputStream(file);
        stream.writeObject(configuration.getProxyHost());
        stream.writeObject(configuration.getProxyPort());
        stream.writeObject(configuration.getPathToBrowserExecutable());
        stream.writeObject(configuration.getAsyncRequestsCompletedTimeoutInSeconds());
        stream.writeBoolean(configuration.getMakeShots());
        stream.writeObject(configuration.getBrowserType());
        stream.writeBoolean(configuration.isUseRandomChars());
        stream.writeObject(configuration.getScreenshotsDirectory());
        stream.writeObject(configuration.getWebDriverTag());
        stream.writeObject(configuration.getAsyncRequestsCompletedTimeoutInSeconds());
        stream.writeObject(configuration.getFormOrDialogXpath());
    }

    @Override
    protected String getControllerDataFilename()
    {
        return "options";
    }
}