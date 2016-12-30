package com.focusit.jsflight.player.cli.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;
import com.focusit.jsflight.player.constants.BrowserType;

public class CliConfig implements IConfig
{
    @Parameter(names = { "-h", "--headless" }, description = "Launching in headless mode")
    private Boolean headless = DefaultValues.HEADLESS;

    @Parameter(names = { "-s",
            "--startStep" }, description = "Skip steps before start", validateWith = PositiveInteger.class)
    private Integer startStep = DefaultValues.START_STEP;

    @Parameter(names = { "-f",
            "--finishStep" }, description = "Events to process", validateWith = PositiveInteger.class)
    private Integer finishStep = DefaultValues.FINISH_STEP;

    @Parameter(names = { "--elementLookupScript" }, description = "Path to web lookup script")
    private String pathToElementLookupScript;

    @Parameter(names = { "--duplicateHandlerScript" }, description = "Path to duplicate events handler script")
    private String pathToDuplicateHandlerScript;

    @Parameter(names = { "--scriptEventHandler" }, description = "Path to script that handles script events")
    private String pathToScriptEventHandlerScript;

    @Parameter(names = { "--jmeterStepProcessorScript" }, description = "Script to process jmeter's sample")
    private String pathToJmeterStepProcessorScript;

    @Parameter(names = {
            "--jmeterScenarioProcessorScript" }, description = "Script to process whole jmeter's scenario just before saving to disk")
    private String pathToJmeterScenarioProcessorScript;

    @Parameter(names = {
            "--shouldSkipKeyboardScript" }, description = "Script to determine whether keyboard event should be skipped")
    private String pathToShouldSkipKeyboardScript;

    @Parameter(names = { "--isUiShownScript" }, description = "Script to check, that page is ready")
    private String pathToIsUiShownScript;

    @Parameter(names = {
            "--isSelectElementScript" }, description = "Script to determine whether WebElement is element of type 'select'")
    private String pathToIsSelectElementScript;

    @Parameter(names = {
            "--isBrowserHaveErrorScript" }, description = "Script to determine whether page is in error state")
    private String pathToIsBrowserHaveErrorScript;

    @Parameter(names = { "--preProcessorScript" }, description = "Script to pre process all events")
    private String pathToPreProcessorScript;

    @Parameter(names = { "--isAsyncCompletedScript" }, description = "Script to determine whether async requests done")
    private String pathToIsAsyncRequestsCompletedScript;

    @Parameter(names = { "-rf", "--recordingFile" }, description = "Path to recording json", required = true)
    private String pathToRecordingFile;

    @Parameter(names = { "-tf", "--templateFile" }, description = "Path to jmx template to generate scenario")
    private String pathToJmxTemplateFile;

    @Parameter(names = { "-be", "--browserExecutable" }, description = "Path to used browser binary")
    private String pathToBrowserExecutable;

    @Parameter(names = { "-bt",
            "--browserType" }, description = "Which browser to use. See BrowserType enum", converter = BrowserTypeConverter.class)
    private BrowserType browserType = DefaultValues.BROWSER_TYPE;

    @Parameter(names = { "-ph", "--proxyHost" }, description = "Selenium proxy host")
    private String proxyHost;

    @Parameter(names = { "-pp", "--proxyPort" }, description = "Selenium proxy port")
    private Integer proxyPort;

    @Parameter(names = { "-r", "--enableRecording" }, description = "Record browser interactions with Jmeter")
    private Boolean enableRecording = DefaultValues.ENABLE_RECORDING;

    @Parameter(names = { "--makeScreenshots" }, description = "Need to make screenshots")
    private Boolean makeScreenshots = DefaultValues.MAKE_SCREENSHOTS;

    @Parameter(names = { "--screenshotsDirectory" }, description = "Directory to store screenshots")
    private String screenshotsDirectory = DefaultValues.SCREENSHOTS_DIRECTORY;

    @Parameter(names = { "--resultFile" }, description = "Generated JMX scenario file name")
    private String generatedJmeterScenarioName = DefaultValues.GENERATED_SCENARIO_NAME;

    @Parameter(names = { "--asyncRequestsCompletedTimeoutInSeconds" }, description = "Timeout for page to get ready")
    private Integer asyncRequestsCompletedTimeoutInSeconds = DefaultValues.ASYNC_REQUESTS_COMPLETED_TIMEOUT_IN_SECONDS;

    @Parameter(names = { "--uiShownTimeoutInSeconds" }, description = "Timeout for waiting of the UI appearance")
    private Integer uiShownTimeoutInSeconds = DefaultValues.UI_SHOWN_TIMEOUT;

    @Parameter(names = { "--intervalBetweenUiChecksInMs" }, description = "Timeout for waiting of the UI appearance")
    private Integer intervalBetweenUiChecksInMs = DefaultValues.INTERVAL_BETWEEN_UI_CHECKS_IN_MS;

    @Parameter(names = {
            "--useRandomChars" }, description = "Use random chars for keypress events instead of recorded ones")
    private Boolean useRandomChars = DefaultValues.USE_RANDOM_CHARS;

    @Parameter(names = { "-h", "--help" }, description = "Show this help", help = true)
    private Boolean help = false;

    @Parameter(names = {
            "--keepBrowserXpath" }, description = "If during browser closing xpath matches any element, browser will not be closed.")
    private String keepBrowserXpath;

    @Parameter(names = {
            "--selectXpath" }, description = "If target element is select element, player will wait until element located by this xpath will not be shown")
    private String selectXpath;

    @Parameter(names = { "--lowerXvfb" }, description = "Lower bound of the Xvfb displays pool")
    private Integer xvfbDisplayLowerBound = DefaultValues.XVFB_ZERO_DISPLAY;

    @Parameter(names = { "--upperXvfb" }, description = "Upper bound of the Xvfb displays pool")
    private Integer xvfbDisplayUpperBound = DefaultValues.XVFB_ZERO_DISPLAY;

    @Parameter(names = {
            "--targetBaseUrl" }, description = "Base url of the target server. Will be used for filling templates in event url", required = true)
    private String targetBaseUrl;

    @Parameter(names = { "--maxRequestsPerScenario" }, description = "The maximum number of requests per one generated JMeter scenario. Default is Long.MAX_VALUE")
    private Long maximumCountOfRequestPerJMeterScenario = DefaultValues.MAX_REQUESTS_PER_SCENARIO;

    public Boolean shouldShowUsage()
    {
        return help;
    }

    @Override
    public String getPathToDuplicateHandlerScript()
    {
        return pathToDuplicateHandlerScript;
    }

    @Override
    public String getPathToShouldSkipKeyboardScript()
    {
        return pathToShouldSkipKeyboardScript;
    }

    @Override
    public String getPathToIsUiShownScript()
    {
        return pathToIsUiShownScript;
    }

    @Override
    public String getPathToIsSelectElementScript()
    {
        return pathToIsSelectElementScript;
    }

    @Override
    public String getPathToIsBrowserHaveErrorScript()
    {
        return pathToIsBrowserHaveErrorScript;
    }

    @Override
    public String getPathToIsAsyncRequestsCompletedScript()
    {
        return pathToIsAsyncRequestsCompletedScript;
    }

    @Override
    public String getPathToUrlReplacementScript()
    {
        return null;
    }

    @Override
    public String getPathToBrowserExecutable()
    {
        return pathToBrowserExecutable;
    }

    @Override
    public Integer getFinishStep()
    {
        return finishStep;
    }

    @Override
    public String getKeepBrowserXpath()
    {
        return keepBrowserXpath;
    }

    @Override
    public String getSelectXpath()
    {
        return selectXpath;
    }

    @Override
    public String getGeneratedJmeterScenarioName()
    {
        return generatedJmeterScenarioName;
    }

    @Override
    public String getPathToJmeterScenarioProcessorScript()
    {
        return pathToJmeterScenarioProcessorScript;
    }

    @Override
    public String getPathToJmeterStepProcessorScript()
    {
        return pathToJmeterStepProcessorScript;
    }

    @Override
    public String getPathToJmxTemplateFile()
    {
        return pathToJmxTemplateFile;
    }

    @Override
    public Boolean shouldMakeScreenshots()
    {
        return makeScreenshots;
    }

    @Override
    public String getPathToPreProcessorScript()
    {
        return pathToPreProcessorScript;
    }

    @Override
    public String getTargetBaseUrl()
    {
        return targetBaseUrl;
    }

    @Override
    public Long getMaximumCountOfRequestPerJMeterScenario() {
        return maximumCountOfRequestPerJMeterScenario;
    }

    @Override
    public Integer getAsyncRequestsCompletedTimeoutInSeconds()
    {
        return asyncRequestsCompletedTimeoutInSeconds;
    }

    @Override
    public Integer getUiShownTimeoutInSeconds()
    {
        return uiShownTimeoutInSeconds;
    }

    public Integer getIntervalBetweenUiChecksInMs()
    {
        return intervalBetweenUiChecksInMs;
    }

    @Override
    public String getPathToRecordingFile()
    {
        return pathToRecordingFile;
    }

    @Override
    public String getProxyHost()
    {
        return proxyHost;
    }

    @Override
    public Integer getProxyPort()
    {
        return proxyPort;
    }

    public Integer getXvfbDisplayLowerBound()
    {
        return xvfbDisplayLowerBound;
    }

    public Integer getXvfbDisplayUpperBound()
    {
        return xvfbDisplayUpperBound;
    }

    @Override
    public String getScreenshotsDirectory()
    {
        return screenshotsDirectory;
    }

    @Override
    public BrowserType getBrowserType()
    {
        return browserType;
    }

    @Override
    public String getPathToScriptEventHandlerScript()
    {
        return pathToScriptEventHandlerScript;
    }

    @Override
    public Integer getStartStep()
    {
        return startStep;
    }

    @Override
    public String getPathToElementLookupScript()
    {
        return pathToElementLookupScript;
    }

    @Override
    public Boolean shouldEnableRecording()
    {
        return enableRecording;
    }

    @Override
    public Boolean isHeadlessModeEnabled()
    {
        return headless;
    }

    @Override
    public Boolean shouldUseRandomChars()
    {
        return useRandomChars;
    }
}
