package com.focusit.jsflight.player.cli.config;

import com.focusit.jsflight.player.constants.BrowserType;

/**
 * Created by Gallyam Biktashev on 04.10.16.
 */
public final class DefaultValues {
    public static final Boolean HEADLESS = false;
    public static final Boolean ENABLE_RECORDING = false;
    public static final Boolean USE_RANDOM_CHARS = false;
    public static final Boolean MAKE_SCREENSHOTS = false;

    public static final Integer START_STEP = 0;
    public static final Integer FINISH_STEP = 0;

    public static final BrowserType BROWSER_TYPE = BrowserType.FIREFOX;

    public static final String SCREENSHOTS_DIRECTORY = "screenshots";
    public static final String GENERATED_SCENARIO_NAME = "result.jmx";

    public static final Integer ASYNC_REQUESTS_COMPLETED_TIMEOUT_IN_SECONDS = 60;

    public static final Integer UI_SHOWN_TIMEOUT = 60;
    public static final Integer INTERVAL_BETWEEN_UI_CHECKS_IN_MS = 500;

    public static final Integer XVFB_ZERO_DISPLAY = 0;
}
