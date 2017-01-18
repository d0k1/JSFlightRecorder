package com.focusit.jsflight.player.cli.config;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.focusit.jsflight.player.constants.BrowserType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertiesConfig implements IConfig
{
    private static final Logger LOG = LoggerFactory.getLogger(PropertiesConfig.class.getSimpleName());

    private static final IParameterValidator REQUIRED = (name, value) -> {
        if (value == null)
        {
            throw new ParameterException("Parameter '" + name + "' is null");
        }
    };
    private static final IParameterValidator ZERO = (name, value) -> {
        if (value != null && Integer.valueOf(value) != 0)
        {
            throw new ParameterException("Parameter '" + name + "' must be a zero");
        }
    };
    private static final IParameterValidator POSITIVE_INTEGER = (name, value) -> {
        if (value != null && Integer.valueOf(value) <= 0)
        {
            throw new ParameterException("Parameter '" + name + "' must be a valid positive integer");
        }
    };
    private static final IParameterValidator POSITIVE_LONG = (name, value) -> {
        if (value != null && Long.valueOf(value) <= 0)
        {
            throw new ParameterException("Parameter '" + name + "' must be a valid positive long");
        }
    };

    private Properties properties = new Properties();

    public PropertiesConfig()
    {

    }

    public PropertiesConfig(String propertiesFilePath)
    {
        try (InputStream input = new FileInputStream(propertiesFilePath))
        {
            LOG.info("Initializing properties from '{}' file", propertiesFilePath);
            properties.load(input);
        }
        catch (IOException e)
        {
            LOG.error(e.getMessage(), e);
        }
    }

    private IParameterValidator or(IParameterValidator... validators)
    {
        return (name, value) -> {
            boolean isValid = false;
            List<ParameterException> exceptions = new ArrayList<>();
            for (int i = 0; i < validators.length && !isValid; i++)
            {
                try
                {
                    validators[i].validate(name, value);
                    isValid = true;
                }
                catch (ParameterException e)
                {
                    exceptions.add(e);
                }
            }

            if (!isValid)
            {
                StringBuilder exceptionMessage = new StringBuilder();
                for (ParameterException exception : exceptions)
                {
                    if (exceptionMessage.length() != 0)
                    {
                        exceptionMessage.append(" or ");
                    }
                    exceptionMessage.append(exception.getMessage());
                }
                throw new ParameterException(exceptionMessage.toString());
            }
        };
    }

    private String getProperty(String name)
    {
        return getProperty(name, null);
    }

    private String getProperty(String name, IParameterValidator validator)
    {
        return getProperty(name, validator, (IStringConverter<String>)String::toString);
    }

    private <T> T getProperty(String name, IParameterValidator validator, IStringConverter<T> converter)
    {
        return getProperty(name, null, validator, converter);
    }

    private <T> T getProperty(String name, T defaultValue, IStringConverter<T> converter)
    {
        return getProperty(name, defaultValue, null, converter);
    }

    private <T> T getProperty(String name, T defaultValue, IParameterValidator validator, IStringConverter<T> converter)
    {
        String value = properties.getProperty(name);
        if (value == null && defaultValue != null)
        {
            value = defaultValue.toString();
        }
        if (validator != null)
        {
            validator.validate(name, value);
        }
        return value == null ? null : converter.convert(value);
    }

    @Override
    @Nullable
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
        return getProperty(PropertiesConstants.RECORDING_PATH, REQUIRED);
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
    public String getPathToUrlReplacementScript()
    {
        return getProperty(PropertiesConstants.URL_REPLACEMENT_SCRIPT_PATH);
    }

    @Override
    public String getPathToPreProcessorScript()
    {
        return getProperty(PropertiesConstants.PRE_PROCESS_SCRIPT_PATH);
    }

    @Override
    @Nonnull
    public String getTargetBaseUrl()
    {
        return getProperty(PropertiesConstants.TARGET_BASE_URL, REQUIRED);
    }

    @Override
    public Long getMaximumCountOfRequestPerJMeterScenario()
    {
        return getProperty(PropertiesConstants.MAX_REQUESTS_PER_SCENARIO, DefaultValues.MAX_REQUESTS_PER_SCENARIO,
                POSITIVE_LONG, Long::new);
    }

    @Override
    public Integer getStartStep()
    {
        return getProperty(PropertiesConstants.START_STEP, DefaultValues.START_STEP, or(ZERO, POSITIVE_INTEGER),
                Integer::new);
    }

    @Override
    public Integer getFinishStep()
    {
        return getProperty(PropertiesConstants.FINISH_STEP, DefaultValues.FINISH_STEP, or(ZERO, POSITIVE_INTEGER),
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
                String::toString);
    }

    @Override
    public Integer getAsyncRequestsCompletedTimeoutInSeconds()
    {
        return getProperty(PropertiesConstants.ASYNC_REQUESTS_COMPLETED_TIMEOUT_IN_SECONDS,
                DefaultValues.ASYNC_REQUESTS_COMPLETED_TIMEOUT_IN_SECONDS, POSITIVE_INTEGER, Integer::new);
    }

    @Override
    public Integer getUiShownTimeoutInSeconds()
    {
        return getProperty(PropertiesConstants.UI_SHOWN_TIMEOUT, DefaultValues.UI_SHOWN_TIMEOUT, POSITIVE_INTEGER,
                Integer::new);
    }

    @Override
    public Integer getIntervalBetweenUiChecksInMs()
    {
        return getProperty(PropertiesConstants.UI_CHECKS_INTERVAL_IN_MS,
                DefaultValues.INTERVAL_BETWEEN_UI_CHECKS_IN_MS, POSITIVE_INTEGER, Integer::new);
    }

    @Override
    public String getProxyHost()
    {
        return getProperty(PropertiesConstants.PROXY_HOST);
    }

    @Override
    public Integer getProxyPort()
    {
        return getProperty(PropertiesConstants.PROXY_PORT, POSITIVE_INTEGER, (IStringConverter<Integer>)Integer::new);
    }

    public Integer getXvfbDisplayLowerBound()
    {
        return getProperty(PropertiesConstants.XVFB_LOWER_BOUND, DefaultValues.XVFB_ZERO_DISPLAY,
                or(ZERO, POSITIVE_INTEGER), Integer::new);
    }

    public Integer getXvfbDisplayUpperBound()
    {
        return getProperty(PropertiesConstants.XVFB_UPPER_BOUND, DefaultValues.XVFB_ZERO_DISPLAY,
                or(ZERO, POSITIVE_INTEGER), Integer::new);
    }

    @Override
    public String getScreenshotsDirectory()
    {
        return getProperty(PropertiesConstants.SCREENSHOT_DIRECTORY, DefaultValues.SCREENSHOTS_DIRECTORY,
                String::toString);
    }

    @Override
    public BrowserType getBrowserType()
    {
        return getProperty(PropertiesConstants.BROWSER_TYPE, DefaultValues.BROWSER_TYPE, BrowserType::valueOf);
    }

    @Override
    public Boolean shouldEnableRecording()
    {
        return getProperty(PropertiesConstants.RECORDING_ENABLED, DefaultValues.ENABLE_RECORDING, Boolean::new);
    }

    @Override
    public Boolean isHeadlessModeEnabled()
    {
        return getProperty(PropertiesConstants.HEADLESS_ENABLED, DefaultValues.HEADLESS, Boolean::new);
    }

    @Override
    public Boolean shouldUseRandomChars()
    {
        return getProperty(PropertiesConstants.USE_RANDOM_CHARS, DefaultValues.USE_RANDOM_CHARS, Boolean::new);
    }

    @Override
    public Boolean shouldMakeScreenshots()
    {
        return getProperty(PropertiesConstants.MAKE_SCREENSHOTS, DefaultValues.MAKE_SCREENSHOTS, Boolean::new);
    }
}
