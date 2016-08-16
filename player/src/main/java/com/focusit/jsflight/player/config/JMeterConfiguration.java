package com.focusit.jsflight.player.config;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.focusit.jmeter.JMeterRecorder;

/**
 * Created by dkirpichenkov on 06.05.16.
 */
public class JMeterConfiguration
{
    private static final Logger LOG = LoggerFactory.getLogger(JMeterConfiguration.class);

    private String stepProcessorScript = "";
    private String scenarioProcessorScript = "";

    public String getStepProcessorScript()
    {
        return stepProcessorScript;
    }

    public void setStepProcessorScript(@Nullable JMeterRecorder recorder, String stepProcessorScript)
    {
        this.stepProcessorScript = stepProcessorScript;
        if (recorder != null)
        {
            syncScripts(recorder);
        }
    }

    public String getScenarioProcessorScript()
    {
        return scenarioProcessorScript;
    }

    public void setScenarioProcessorScript(@Nullable JMeterRecorder recorder, String scenarioProcessorScript)
    {
        this.scenarioProcessorScript = scenarioProcessorScript;
        if (recorder != null)
        {
            syncScripts(recorder);
        }
    }

    public void syncScripts(JMeterRecorder recorder)
    {
        if (getStepProcessorScript() != null)
        {
            recorder.getScriptProcessor().setRecordingScript(getStepProcessorScript());
        }

        if (getScenarioProcessorScript() != null)
        {
            recorder.getScriptProcessor().setProcessScript(getScenarioProcessorScript());
        }
    }

    private void loadDefaultStepProcessorScript()
    {
        try
        {
            InputStream script = this.getClass().getClassLoader()
                    .getResourceAsStream("example-scripts/jmeter/step/example02.groovy");
            if (script != null)
            {
                stepProcessorScript = IOUtils.toString(script, "UTF-8");
            }
        }
        catch (IOException e)
        {
            LOG.error(e.toString(), e);
        }
    }

    private void loadDefaultScenarioProcessorScript()
    {
        try
        {
            InputStream script = this.getClass().getClassLoader()
                    .getResourceAsStream("example-scripts/jmeter/scenario/example03.groovy");
            if (script != null)
            {
                scenarioProcessorScript = IOUtils.toString(script, "UTF-8");
            }
        }
        catch (IOException e)
        {
            LOG.error(e.toString(), e);
        }
    }

    public void loadDefaults()
    {
        loadDefaultStepProcessorScript();
        loadDefaultScenarioProcessorScript();
    }
}
