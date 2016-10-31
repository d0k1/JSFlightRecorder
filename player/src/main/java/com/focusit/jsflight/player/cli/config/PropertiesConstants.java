package com.focusit.jsflight.player.cli.config;

/**
 * Created by Gallyam Biktashev on 10.10.16.
 */
public class PropertiesConstants {
    public static final String BROWSER_EXECUTABLE_PATH = "browser.executable.path";
    public static final String JMETER_TEMPLATE_PATH = "jmeter.template.path";
    public static final String RECORDING_PATH = "recording.path";

    public static final String SCRIPT_EVENT_HANDLER_SCRIPT_PATH = "scripts.scriptEventHandler.path";
    public static final String ELEMENT_LOOKUP_SCRIPT_PATH = "scripts.elementLookup.path";
    public static final String JMETER_SCENARIO_PROCESSOR_SCRIPT_PATH = "scripts.jmeterScenarioProcessor.path";
    public static final String JMETER_STEP_PROCESSOR_SCRIPT_PATH = "scripts.jmeterStepProcessor.path";
    public static final String DUPLICATE_HANDLER_SCRIPT_PATH = "scripts.duplicationHandler.path";
    public static final String PRE_PROCESS_SCRIPT_PATH = "scripts.preProcess.path";
    public static final String IS_BROWSER_HAVE_ERROR_SCRIPT_PATH = "scripts.isBrowserHaveError.path";
    public static final String IS_SELECT_ELEMENT_SCRIPT_PATH = "scripts.isSelectElement.path";
    public static final String IS_UI_SHOWN_SCRIPT_PATH = "scripts.isUiShown.path";
    public static final String SHOULD_SKIP_KEYBOARD_SCRIPT_PATH = "scripts.shouldSkipKeyboard.path";
    public static final String IS_ASYNC_REQUESTS_COMPLETED_SCRIPT_PATH = "scripts.isAsyncCompleted.path";

    public static final String START_STEP = "start.step";
    public static final String FINISH_STEP = "finish.step";

    public static final String BROWSER_KEEP_XPATH = "xpath.keepBrowser";
    public static final String SELECT_XPATH = "xpath.select";

    public static final String JMETER_GENERATED_SCENARIO_NAME = "jmeter.generatedScenario.name";
    public static final String ASYNC_REQUESTS_COMPLETED_TIMEOUT_IN_SECONDS = "page.asyncRequests.timeoutInSec";

    public static final String UI_SHOWN_TIMEOUT = "ui.shownTimeout";
    public static final String UI_CHECKS_INTERVAL_IN_MS = "ui.checksIntervalInMs";

    public static final String PROXY_HOST = "proxy.host";
    public static final String PROXY_PORT = "proxy.port";

    public static final String SCREENSHOT_DIRECTORY = "screenshot.directory";
    public static final String MAKE_SCREENSHOTS = "screenshot.make";

    public static final String BROWSER_TYPE = "browser.type";

    public static final String RECORDING_ENABLED = "recording.enabled";
    public static final String HEADLESS_ENABLED = "headless.enabled";
    public static final String USE_RANDOM_CHARS = "useRandomChars";

    public static final String XVFB_LOWER_BOUND = "xvfb.lower";
    public static final String XVFB_UPPER_BOUND = "xvfb.upper";
}
