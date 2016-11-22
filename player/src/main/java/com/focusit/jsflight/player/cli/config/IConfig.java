package com.focusit.jsflight.player.cli.config;

import com.focusit.jsflight.player.constants.BrowserType;

public interface IConfig
{
    String getPathToBrowserExecutable();

    String getPathToJmxTemplateFile();

    String getPathToRecordingFile();

    String getPathToScriptEventHandlerScript();

    String getPathToElementLookupScript();

    String getPathToJmeterScenarioProcessorScript();

    String getPathToJmeterStepProcessorScript();

    String getPathToDuplicateHandlerScript();

    String getPathToShouldSkipKeyboardScript();

    String getPathToIsUiShownScript();

    String getPathToIsSelectElementScript();

    String getPathToIsBrowserHaveErrorScript();

    String getPathToIsAsyncRequestsCompletedScript();


    Integer getStartStep();

    Integer getFinishStep();


    String getKeepBrowserXpath();

    String getSelectXpath();


    String getGeneratedJmeterScenarioName();

    Integer getAsyncRequestsCompletedTimeoutInSeconds();

    Integer getUiShownTimeoutInSeconds();

    Integer getIntervalBetweenUiChecksInMs();

    String getProxyHost();

    Integer getProxyPort();


    Integer getXvfbDisplayLowerBound();

    Integer getXvfbDisplayUpperBound();


    String getScreenshotsDirectory();

    BrowserType getBrowserType();


    Boolean shouldEnableRecording();

    Boolean isHeadlessModeEnabled();

    Boolean shouldUseRandomChars();

    Boolean shouldMakeScreenshots();

    String getPathToPreProcessorScript();

    String getTargetBaseUrl();
}
