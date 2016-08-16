package com.focusit.jsflight.player.fileconfigholder;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.focusit.jsflight.player.config.JMeterConfiguration;

public class JMeterFileConfigHolder extends UIFileConfigHolder
{
    private static final long serialVersionUID = 1L;
    private final static JMeterFileConfigHolder instance = new JMeterFileConfigHolder();

    private JMeterConfiguration configuration;

    private JMeterFileConfigHolder()
    {
    }

    public static JMeterFileConfigHolder getInstance()
    {
        return instance;
    }

    @Override
    public void load(String file) throws Exception
    {
        ObjectInputStream stream = getInputStream(file);
        configuration.setStepProcessorScript(null, (String)stream.readObject());
        configuration.setScenarioProcessorScript(null, (String)stream.readObject());
        //syncScripts();
    }

    @Override
    public void store(String file) throws Exception
    {
        ObjectOutputStream stream = getOutputStream(file);
        stream.writeObject(configuration.getStepProcessorScript());
        stream.writeObject(configuration.getScenarioProcessorScript());
    }

    @Override
    protected String getControllerDataFilename()
    {
        return null;
    }

    public void setConfiguration(JMeterConfiguration configuration)
    {
        this.configuration = configuration;
    }
}
