package com.focusit.jsflight.player.cli;

import com.beust.jcommander.Parameter;

public class CliConfig
{
    public static final String DEFAULT_RECORDING_NAME = "test.jmx";

    @Parameter(names = { "-hl", "--headless" }, description = "Launching in headless mode")
    private boolean headless = false;

    @Parameter(names = { "-ss", "--startStep" }, description = "Skip steps before start")
    private String startStep = "0";

    @Parameter(names = { "-fs", "--finishStep" }, description = "Events to process")
    private String finishStep = "0";

    @Parameter(names = { "-p", "--path" }, description = "Path to recording json")
    private String pathToRecording = "";

    @Parameter(names = { "-ph", "--proxyhost" }, description = "Selenium proxy host")
    private String proxyHost = "";

    @Parameter(names = { "-pp", "--proxyport" }, description = "Selenium proxy port")
    private String proxyPort = "";

    @Parameter(names = { "-tp", "--templatepath" }, description = "Path to jmx template to generate scenario")
    private String jmxTemplatePath = "";

    @Parameter(names = { "-r", "--recording" }, description = "Record browser interactions with Jmeter")
    private boolean enableRecording = false;

    @Parameter(names = { "-f", "--firefox" }, description = "Use Firefox as browser")
    private boolean useFifefox = true;

    @Parameter(names = { "-pj", "--phantomjs" }, description = "Use phantomjs as browser")
    private boolean usePhantomJS = false;

    @Parameter(names = { "-ffp", "--firefoxpath" }, description = "Path to firefox binary")
    private String ffPath = "";

    @Parameter(names = { "-pjp", "--phantomjspath" }, description = "Path to phantomjs binary")
    private String pjsPath = "";

    @Parameter(names = { "-s", "--shots" }, description = "Need to make screenshots")
    private boolean makeShots = true;

    @Parameter(names = { "-sp", "--screenshotspath" }, description = "Directory to store screenshots")
    private String pathToScreenShots = "shots";

    @Parameter(names = { "-jrn", "--jmeterrecordingname" }, description = "JMX recording file name")
    private String jmeterRecordingName = DEFAULT_RECORDING_NAME;

    @Parameter(names = { "-pt", "--pagereadytimout" }, description = "Timeout for page to get ready")
    private String pageReadyTimeout = "30";

    @Parameter(names = { "-wl", "--weblookuppath" }, description = "Path to web lookup script")
    private String webLookUpScriptPath = "scripts/weblookup.groovy";

    @Parameter(names = { "-dh", "--duplicatehandler" }, description = "Path to duplicate events handler script")
    private String duplicateHandlerScriptPath = "scripts/duplicateHandler.groovy";

    @Parameter(names = { "-seh", "--scripteventhandler" }, description = "Path to script that handles script events")
    private String scriptEventHandlerScriptPath = "scripts/scriptEventHandler.groovy";

    @Parameter(names = { "-urc", "--userandomchars" }, description = "Use random chars for keypress events instead of recorded ones")
    private boolean useRandomChars = false;

    @Parameter(names = { "-h", "--help" }, description = "Show this help")
    private boolean help = false;

    @Parameter(names = { "-js1", "--jmeterpreprocesstep" }, description = "Script to preprocess jmeter's sample")
    private String jmeterStepPreprocess;

    @Parameter(names = { "-js2", "--jmeterpreprocesscenario" }, description = "Script to preprocess whole jmeter's scenario item just before saving to disk")
    private String jmeterScenarioPreprocess;

    @Parameter(names = { "-fdxp", "--formdialogxpath" }, description = "Xpath to detect if window has open form or dialog. If specified unused browser windows will be closed")
    private String formDialogXpath = "";

    public String getDuplicateHandlerScriptPath()
    {
        return duplicateHandlerScriptPath;
    }

    public String getFfPath()
    {
        return ffPath;
    }

    public String getFinishStep()
    {
        return finishStep;
    }

    public void setFinishStep(String finishStep)
    {
        this.finishStep = finishStep;
    }

    public String getFormDialogXpath()
    {
        return formDialogXpath;
    }

    public String getJMeterLogLevel()
    {
        return "INFO";
    }

    public String getJmeterRecordingName()
    {
        return jmeterRecordingName;
    }

    public String getJmeterScenarioPreprocess()
    {
        return jmeterScenarioPreprocess;
    }

    public String getJmeterStepPreprocess()
    {
        return jmeterStepPreprocess;
    }

    public String getJmxTemplatePath()
    {
        return jmxTemplatePath;
    }

    public boolean getMakeShots()
    {
        return makeShots;
    }

    public String getPageReadyTimeout()
    {
        return pageReadyTimeout;
    }

    public String getPathToRecording()
    {
        return pathToRecording;
    }

    public String getPjsPath()
    {
        return pjsPath;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public String getProxyPort()
    {
        return proxyPort;
    }

    public String getScreenDir()
    {
        return pathToScreenShots;
    }

    public String getScriptEventHandlerScriptPath()
    {
        return scriptEventHandlerScriptPath;
    }

    public String getStartStep()
    {
        return startStep;
    }

    public String getWebLookupScriptPath()
    {
        return webLookUpScriptPath;
    }

    public boolean isEnableRecording()
    {
        return enableRecording;
    }

    public boolean isHeadLess()
    {
        return headless;
    }

    public boolean isUseFirefox()
    {
        return useFifefox;
    }

    public boolean isUsePhantomJs()
    {
        return usePhantomJS;
    }

    public boolean isUseRandomChars()
    {
        return useRandomChars;
    }

    public boolean showHelp()
    {
        return help;
    }
}
