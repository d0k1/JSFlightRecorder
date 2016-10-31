package com.focusit.jsflight.player.cli.config;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.validators.PositiveInteger;
import com.focusit.jsflight.player.constants.BrowserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesConfig implements IConfig
{
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesConfig.class.getSimpleName());

    private Properties properties = new Properties();

    public PropertiesConfig(String propertiesFilePath)
    {
        try (InputStream input = new FileInputStream(propertiesFilePath))
        {
            properties.load(input);
        }
        catch (IOException e)
        {
            LOG.error(e.getMessage(), e);
        }
    }

    private String getProperty(String name)
    {
        return getProperty(name, null);
    }

    private String getProperty(String name, IParameterValidator validator)
    {
        return getProperty(name, validator, String::toString);
    }

    private <T> T getProperty(String name, IParameterValidator validator, IStringConverter<T> converter)
    {
        return getProperty(name, null, converter, validator);
    }

    private <T> T getProperty(String name, T defaultValue, IParameterValidator validator, IStringConverter<T> converter)
    {
        return getProperty(name, defaultValue == null ? null : defaultValue.toString(), converter, validator);
    }

    private <T> T getProperty(String name, String defaultValue, IStringConverter<T> converter,
            IParameterValidator validator)
    {
        String value = properties.getProperty(name);
        if (value == null && defaultValue != null)
        {
            value = defaultValue;
        }
        if (validator != null)
        {
            validator.validate(name, value);
        }
        return value == null ? null : converter.convert(value);
    }

    @Override
    public String getPathToBrowserExecutable()
    {
        return getProperty(PropertiesConstants.BROWSER_EXECUTABLE_PATH);
    }

    @Override
    public String getPathToJmxTemplateFile()
    {
        return getProperty(PropertiesConstants.JMETER_TEMPLATE_PATH);
    }

    @Override
    public String getPathToRecordingFile()
    {
        return getProperty(PropertiesConstants.RECORDING_PATH);
    }

    @Override
    public String getPathToScriptEventHandlerScript()
    {
        return getProperty(PropertiesConstants.SCRIPT_EVENT_HANDLER_SCRIPT_PATH);
    }

    @Override
    public String getPathToElementLookupScript()
    {
        return getProperty(PropertiesConstants.ELEMENT_LOOKUP_SCRIPT_PATH);
    }

    @Override
    public String getPathToJmeterScenarioProcessorScript()
    {
        return getProperty(PropertiesConstants.JMETER_SCENARIO_PROCESSOR_SCRIPT_PATH);
    }

    @Override
    public String getPathToJmeterStepProcessorScript()
    {
        return getProperty(PropertiesConstants.JMETER_STEP_PROCESSOR_SCRIPT_PATH);
    }

    @Override
    public String getPathToDuplicateHandlerScript()
    {
        return getProperty(PropertiesConstants.DUPLICATE_HANDLER_SCRIPT_PATH);
    }

    @Override
    public String getPathToShouldSkipKeyboardScript()
    {
        return getProperty(PropertiesConstants.SHOULD_SKIP_KEYBOARD_SCRIPT_PATH);
    }

    @Override
    public String getPathToIsUiShownScript()
    {
        return getProperty(PropertiesConstants.IS_UI_SHOWN_SCRIPT_PATH);
    }

    @Override
    public String getPathToIsSelectElementScript()
    {
        return getProperty(PropertiesConstants.IS_SELECT_ELEMENT_SCRIPT_PATH);
    }

    @Override
    public String getPathToIsBrowserHaveErrorScript()
    {
        return getProperty(PropertiesConstants.IS_BROWSER_HAVE_ERROR_SCRIPT_PATH);
    }

    @Override
    public String getPathToIsAsyncRequestsCompletedScript()
    {
        return getProperty(PropertiesConstants.IS_ASYNC_REQUESTS_COMPLETED_SCRIPT_PATH);
    }

    @Override
    public String getPathToPreProcessorScript()
    {
        return getProperty(PropertiesConstants.PRE_PROCESS_SCRIPT_PATH);
    }

    @Override
    public Integer getStartStep()
    {
        return getProperty(PropertiesConstants.START_STEP, DefaultValues.START_STEP, new PositiveInteger(),
                Integer::new);
    }

    @Override
    public Integer getFinishStep()
    {
        return getProperty(PropertiesConstants.FINISH_STEP, DefaultValues.FINISH_STEP, new PositiveInteger(),
                Integer::new);
    }

    @Override
    public String getKeepBrowserXpath()
    {
        return getProperty(PropertiesConstants.BROWSER_KEEP_XPATH);
    }

    @Override
    public String getSelectXpath()
    {
        return getProperty(PropertiesConstants.SELECT_XPATH);
    }

    @Override
    public String getGeneratedJmeterScenarioName()
    {
        return getProperty(PropertiesConstants.JMETER_GENERATED_SCENARIO_NAME, DefaultValues.GENERATED_SCENARIO_NAME,
                String::toString, null);
    }

    @Override
    public Integer getAsyncRequestsCompletedTimeoutInSeconds()
    {
        return getProperty(PropertiesConstants.ASYNC_REQUESTS_COMPLETED_TIMEOUT_IN_SECONDS,
                DefaultValues.ASYNC_REQUESTS_COMPLETED_TIMEOUT_IN_SECONDS, null, Integer::new);
    }

    @Override
    public Integer getUiShownTimeoutInSeconds()
    {
        return getProperty(PropertiesConstants.UI_SHOWN_TIMEOUT, DefaultValues.UI_SHOWN_TIMEOUT, null, Integer::new);
    }

    @Override
    public Integer getIntervalBetweenUiChecksInMs()
    {
        return getProperty(PropertiesConstants.UI_CHECKS_INTERVAL_IN_MS, DefaultValues.INTERVAL_BETWEEN_UI_CHECKS_IN_MS,
                new PositiveInteger(), Integer::new);
    }

    @Override
    public String getProxyHost()
    {
        return getProperty(PropertiesConstants.PROXY_HOST);
    }

    @Override
    public Integer getProxyPort()
    {
        return getProperty(PropertiesConstants.PROXY_PORT, new PositiveInteger(), Integer::new);
    }

    public Integer getXvfbDisplayLowerBound()
    {
        return getProperty(PropertiesConstants.XVFB_LOWER_BOUND, DefaultValues.XVFB_ZERO_DISPLAY,
                new PositiveInteger(), Integer::new);
    }

    public Integer getXvfbDisplayUpperBound()
    {
        return getProperty(PropertiesConstants.XVFB_UPPER_BOUND, DefaultValues.XVFB_ZERO_DISPLAY,
                new PositiveInteger(), Integer::new);
    }

    @Override
    public String getScreenshotsDirectory()
    {
        return getProperty(PropertiesConstants.SCREENSHOT_DIRECTORY, DefaultValues.SCREENSHOTS_DIRECTORY, null,
                String::toString);
    }

    @Override
    public BrowserType getBrowserType()
    {
        return getProperty(PropertiesConstants.BROWSER_TYPE, DefaultValues.BROWSER_TYPE, null, BrowserType::valueOf);
    }

    @Override
    public Boolean shouldEnableRecording()
    {
        return getProperty(PropertiesConstants.RECORDING_ENABLED, DefaultValues.ENABLE_RECORDING, null, Boolean::new);
    }

    @Override
    public Boolean isHeadlessModeEnabled()
    {
        return getProperty(PropertiesConstants.HEADLESS_ENABLED, DefaultValues.HEADLESS, null, Boolean::new);
    }

    @Override
    public Boolean shouldUseRandomChars()
    {
        return getProperty(PropertiesConstants.USE_RANDOM_CHARS, DefaultValues.USE_RANDOM_CHARS, null, Boolean::new);
    }

    @Override
    public Boolean shouldMakeScreenshots()
    {
        return getProperty(PropertiesConstants.MAKE_SCREENSHOTS, DefaultValues.MAKE_SCREENSHOTS, null, Boolean::new);
    }
}
