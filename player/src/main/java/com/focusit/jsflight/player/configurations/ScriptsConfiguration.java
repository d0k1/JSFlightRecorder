package com.focusit.jsflight.player.configurations;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.focusit.jsflight.jmeter.JMeterRecorder;
import com.focusit.jsflight.player.configurations.interfaces.DefaultFile;
import com.focusit.jsflight.player.configurations.interfaces.IDefaults;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.Arrays;

public class ScriptsConfiguration implements IDefaults
{
    @JsonIgnore
    private static final Logger LOG = LoggerFactory.getLogger(ScriptsConfiguration.class.getSimpleName());

    @DefaultFile("jmeter/stepProcessor.groovy")
    private String stepProcessorScript;

    @DefaultFile("jmeter/scenarioProcessor.groovy")
    private String scenarioProcessorScript;

    @DefaultFile("selenium/elementLookup.groovy")
    private String elementLookupScript;

    @DefaultFile("selenium/duplicationHandler.groovy")
    private String duplicationHandlerScript;

    @DefaultFile("selenium/isBrowserHaveError.groovy")
    private String isBrowserHaveErrorScript;

    @DefaultFile("selenium/isSelectElement.groovy")
    private String isSelectElementScript;

    @DefaultFile("selenium/isUiShown.groovy")
    private String isUiShownScript;

    @DefaultFile("selenium/scriptEventHandler.groovy")
    private String scriptEventHandlerScript;

    @DefaultFile("selenium/shouldSkipKeyboard.groovy")
    private String shouldSkipKeyboardScript;

    @DefaultFile("selenium/sendSignalToProcess.groovy")
    private String sendSignalToProcessScript;

    @DefaultFile("selenium/getWebDriverPid.groovy")
    private String getWebDriverPidScript;

    @DefaultFile("js/isAsyncRequestsCompleted.js")
    private String isAsyncRequestsCompletedScript;

    private static Method getGetterForField(Field field) throws NoSuchMethodException
    {
        return getMethodWithPrefixForField(field, "get");
    }

    private static Method getSetterForField(Field field) throws NoSuchMethodException
    {
        return getMethodWithPrefixForField(field, "set", String.class);
    }

    private static String getScriptPathForField(Field field)
    {
        DefaultFile defaultFile = field.getAnnotation(DefaultFile.class);
        String path = Paths.get(defaultFile.rootDirectory(), defaultFile.value()).toString();
        LOG.info("Trying to read default script from {}", path);
        return path;
    }

    private static Method getMethodWithPrefixForField(Field field, String prefix, Class<?>... parameterTypes)
            throws NoSuchMethodException
    {
        return ScriptsConfiguration.class.getMethod(getMethodNameWithPrefixForField(field, prefix), parameterTypes);
    }

    private static String getMethodNameWithPrefixForField(Field field, String prefix)
    {
        return prefix + StringUtils.capitalize(field.getName());
    }

    public String getElementLookupScript()
    {
        return elementLookupScript;
    }

    public void setElementLookupScript(String elementLookupScript)
    {
        this.elementLookupScript = elementLookupScript;
    }

    public String getDuplicationHandlerScript()
    {
        return duplicationHandlerScript;
    }

    public void setDuplicationHandlerScript(String duplicationHandlerScript)
    {
        this.duplicationHandlerScript = duplicationHandlerScript;
    }

    public String getIsBrowserHaveErrorScript()
    {
        return isBrowserHaveErrorScript;
    }

    public void setIsBrowserHaveErrorScript(String isBrowserHaveErrorScript)
    {
        this.isBrowserHaveErrorScript = isBrowserHaveErrorScript;
    }

    public String getIsSelectElementScript()
    {
        return isSelectElementScript;
    }

    public void setIsSelectElementScript(String isSelectElementScript)
    {
        this.isSelectElementScript = isSelectElementScript;
    }

    public String getIsUiShownScript()
    {
        return isUiShownScript;
    }

    public void setIsUiShownScript(String isUiShownScript)
    {
        this.isUiShownScript = isUiShownScript;
    }

    public String getStepProcessorScript()
    {
        return stepProcessorScript;
    }

    public void setStepProcessorScript(String stepProcessorScript)
    {
        this.stepProcessorScript = stepProcessorScript;
    }

    public String getScenarioProcessorScript()
    {
        return scenarioProcessorScript;
    }

    public void setScenarioProcessorScript(String scenarioProcessorScript)
    {
        this.scenarioProcessorScript = scenarioProcessorScript;
    }

    public String getScriptEventHandlerScript()
    {
        return scriptEventHandlerScript;
    }

    public void setScriptEventHandlerScript(String scriptEventHandlerScript)
    {
        this.scriptEventHandlerScript = scriptEventHandlerScript;
    }

    public String getShouldSkipKeyboardScript()
    {
        return shouldSkipKeyboardScript;
    }

    public void setShouldSkipKeyboardScript(String shouldSkipKeyboardScript)
    {
        this.shouldSkipKeyboardScript = shouldSkipKeyboardScript;
    }

    public String getSendSignalToProcessScript()
    {
        return sendSignalToProcessScript;
    }

    public void setSendSignalToProcessScript(String sendSignalToProcessScript)
    {
        this.sendSignalToProcessScript = sendSignalToProcessScript;
    }

    public String getGetWebDriverPidScript()
    {
        return getWebDriverPidScript;
    }

    public void setGetWebDriverPidScript(String getWebDriverPidScript)
    {
        this.getWebDriverPidScript = getWebDriverPidScript;
    }

    public String getIsAsyncRequestsCompletedScript()
    {
        return isAsyncRequestsCompletedScript;
    }

    public void setIsAsyncRequestsCompletedScript(String isAsyncRequestsCompletedScript)
    {
        this.isAsyncRequestsCompletedScript = isAsyncRequestsCompletedScript;
    }

    @Override
    public void loadDefaults()
    {
        // Please, sorry me for this hell, but otherwise it would look very ugly
        // Here we initialize all uninitialized script fields with default script from resources
        Arrays.stream(getClass().getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()) && !isFieldInitialized(field))
                .forEach(field -> {
                    if (!initializeField(field))
                    {
                        throw new RuntimeException(String.format("Can't load resource for %s", field.getName()));
                    }
                });
    }

    private boolean initializeField(Field field)
    {
        try
        {
            getSetterForField(field).invoke(this, readResourceAsString(getScriptPathForField(field)));
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private boolean isFieldInitialized(Field field)
    {
        try
        {
            return getGetterForField(field).invoke(this) != null;
        }
        catch (Exception e)
        {
            return true;
        }
    }

    private String readResourceAsString(String resourceName) throws IOException
    {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName))
        {
            return IOUtils.toString(stream, "UTF-8");
        }
    }

    public void syncScripts(JMeterRecorder recorder)
    {
        recorder.getScriptProcessor().setScenarioProcessorScript(getScenarioProcessorScript());
        recorder.getScriptProcessor().setStepProcessorScript(getStepProcessorScript());
    }

}
