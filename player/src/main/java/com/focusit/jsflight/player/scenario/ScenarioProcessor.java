package com.focusit.jsflight.player.scenario;

import com.focusit.jsflight.player.configurations.CommonConfiguration;
import com.focusit.jsflight.player.configurations.ScriptsConfiguration;
import com.focusit.jsflight.player.constants.BrowserType;
import com.focusit.jsflight.player.constants.EventConstants;
import com.focusit.jsflight.player.constants.EventType;
import com.focusit.jsflight.player.iframe.FrameSwitcher;
import com.focusit.jsflight.player.script.PlayerScriptProcessor;
import com.focusit.jsflight.player.webdriver.SeleniumDriver;
import com.focusit.jsflight.script.constants.ScriptBindingConstants;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Class that really replays an event in given scenario and given selenium driver
 * Created by doki on 05.05.16.
 */
public class ScenarioProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(ScenarioProcessor.class);

    private static WebDriver getWebDriver(UserScenario scenario, SeleniumDriver seleniumDriver, JSONObject event)
    {
        BrowserType browserType = scenario.getConfiguration().getCommonConfiguration().getBrowserType();
        String path = scenario.getConfiguration().getCommonConfiguration().getPathToBrowserExecutable();
        String proxyHost = scenario.getConfiguration().getCommonConfiguration().getProxyHost();
        Integer proxyPort = scenario.getConfiguration().getCommonConfiguration().getProxyPort();

        return seleniumDriver.getDriverForEvent(event, browserType, path, proxyHost, proxyPort);
    }

    /**
     * Method that check if a browser has error dialog visible.
     * and if it has then throws an exception.
     * A browser after any step should not contain any error
     *
     * @param scenario
     * @param wd
     * @throws Exception
     */
    protected void throwIfBrowserHaveAnError(UserScenario scenario, WebDriver wd) throws Exception
    {
        String findBrowserErrorScript = scenario.getConfiguration().getScriptsConfiguration()
                .getIsBrowserHaveErrorScript();
        Map<String, Object> binding = PlayerScriptProcessor.getEmptyBindingsMap();
        binding.put(ScriptBindingConstants.WEB_DRIVER, wd);
        boolean pageContainsError = new PlayerScriptProcessor(scenario).executeGroovyScript(findBrowserErrorScript,
                binding, Boolean.class);
        if (pageContainsError)
        {
            throw new IllegalStateException("Browser contains some error after step processing");
        }
    }

    /**
     * This method will decide whether step processing should be terminated at current step or not.
     * Or, in other words, should an exception be there or not.
     * Default implementation just logs
     *
     * @param position
     * @param ex
     * @throws Exception
     */
    protected void processClickException(int position, Exception ex) throws Exception
    {
        LOG.error("Failed to process step: " + position, ex);
    }

    /**
     * Make a screenshot and save to a file
     *
     * @param scenario
     * @param seleniumDriver
     * @param theWebDriver
     * @param position
     */
    protected void makeAShot(UserScenario scenario, SeleniumDriver seleniumDriver, WebDriver theWebDriver, int position,
            boolean isError)
    {
        if (scenario.getConfiguration().getCommonConfiguration().getMakeShots())
        {
            LOG.info("Making screenshot");
            String screenDir = scenario.getConfiguration().getCommonConfiguration().getScreenshotsDirectory();
            File dir = new File(screenDir, Paths.get(scenario.getScenarioFilename()).getFileName().toString());

            if (!dir.exists() && !dir.mkdirs())
            {
                return;
            }
            String errorPart = isError ? "_error_" : "";
            File file = Paths.get(dir.getAbsolutePath(), errorPart + String.format("%05d", position) + ".png").toFile();
            try (FileOutputStream fos = new FileOutputStream(file))
            {
                seleniumDriver.makeAShot(theWebDriver, fos);
            }
            catch (IOException e)
            {
                LOG.error(e.toString(), e);
            }
        }
    }

    public void applyStep(UserScenario scenario, SeleniumDriver seleniumDriver, int position)
    {
        JSONObject event = scenario.getStepAt(position);
        scenario.getContext().setCurrentScenarioStep(event);

        ScriptsConfiguration scriptsConfiguration = scenario.getConfiguration().getScriptsConfiguration();
        String eventUrl = new PlayerScriptProcessor(scenario)
                .executeUrlReplacementScript(scriptsConfiguration.getUrlReplacementScript(), event);
        event.put(EventConstants.URL, eventUrl);

        String type = event.getString(EventConstants.TYPE);
        LOG.info("Event type: {}", type);

        new PlayerScriptProcessor(scenario).runStepPrePostScript(event, position, true);
        event = new PlayerScriptProcessor(scenario).runStepTemplating(scenario, event);

        // there must be update for eventUrl after templating!
        eventUrl = event.getString(EventConstants.URL);

        //if template processing fails for URL we cannot process this step, so we skip
        if (eventUrl.matches(".*(\\$\\{.*\\}).*"))
        {
            LOG.error("Event at position {} cannot be processed due to url contains unprocessed templates\n"
                    + "EventId: {}\n" + "URL: {}", position, event.get(EventConstants.EVENT_ID), eventUrl);
            return;
        }

        LOG.info("Current step eventId " + event.get(EventConstants.EVENT_ID).toString() + " URL: {}",
                event.get(EventConstants.URL));

        WebDriver theWebDriver = null;
        boolean error = false;
        CommonConfiguration commonConfiguration = scenario.getConfiguration().getCommonConfiguration();
        try
        {
            if (scenario.isStepDuplicates(scriptsConfiguration.getDuplicationHandlerScript(), event))
            {
                LOG.warn("Event duplicates previous");
                return;
            }

            if (UserScenario.isEventIgnored(event) || UserScenario.isEventBad(event))
            {
                StringBuilder builder = new StringBuilder();
                if (event.has(EventConstants.TARGET))
                {
                    builder.append(" Target: '");
                    builder.append(event.get(EventConstants.TARGET));
                    builder.append("';");
                }

                if (event.has(EventConstants.FIRST_TARGET))
                {
                    builder.append(" First Target: '");
                    builder.append(event.get(EventConstants.FIRST_TARGET));
                    builder.append("';");
                }

                if (event.has(EventConstants.SECOND_TARGET))
                {
                    builder.append(" Second Target: '");
                    builder.append(event.get(EventConstants.SECOND_TARGET));
                    builder.append("';");
                }

                LOG.warn("Event is ignored or bad. Type: " + event.get(EventConstants.TYPE) + builder.toString());
                return;
            }

            if (type.equalsIgnoreCase(EventType.SCRIPT))
            {
                new PlayerScriptProcessor(scenario)
                        .executeScriptEvent(scriptsConfiguration.getScriptEventHandlerScript(), event);
                return;
            }

            //Configure webdriver for this event, setting params here so we can change parameters while playback is
            //paused
            seleniumDriver
                    .setAsyncRequestsCompletedTimeoutInSeconds(
                            commonConfiguration.getAsyncRequestsCompletedTimeoutInSeconds())
                    .setIsAsyncRequestsCompletedScript(scriptsConfiguration.getIsAsyncRequestsCompletedScript())
                    .setMaxElementGroovy(commonConfiguration.getMaxElementGroovy())
                    .setElementLookupScript(scriptsConfiguration.getElementLookupScript())
                    .setIsUiShownScript(scriptsConfiguration.getIsUiShownScript())
                    .setUseRandomStringGenerator(commonConfiguration.isUseRandomChars())
                    .setIntervalBetweenUiChecksInMs(commonConfiguration.getIntervalBetweenUiChecksMs())
                    .setUiShownTimeoutInSeconds(commonConfiguration.getUiShownTimeoutSeconds())
                    .setPlaceholders(scenario.getConfiguration().getWebConfiguration().getPlaceholders())
                    .setSelectXpath(scenario.getConfiguration().getWebConfiguration().getSelectXpath())
                    .setIsSelectElementScript(scriptsConfiguration.getIsSelectElementScript())
                    .setSendSignalToProcessScript(scriptsConfiguration.getSendSignalToProcessScript())
                    .setSkipKeyboardScript(scriptsConfiguration.getShouldSkipKeyboardScript())
                    .setGetWebDriverPidScript(scriptsConfiguration.getGetWebDriverPidScript())
                    .setKeepBrowserXpath(commonConfiguration.getFormOrDialogXpath());

            theWebDriver = getWebDriver(scenario, seleniumDriver, event);
            if (theWebDriver == null)
            {
                throw new NullPointerException("getWebDriver return null");
            }
            seleniumDriver.openEventUrl(theWebDriver, event);

            LOG.info("Event {}. Display {}", position, seleniumDriver.getDriverDisplay(theWebDriver));

            seleniumDriver.waitWhileAsyncRequestsWillCompletedWithRefresh(theWebDriver, event);

            FrameSwitcher.switchToWorkingFrame(theWebDriver, event);

            try
            {
                String target = UserScenario.getTargetForEvent(event);

                switch (type)
                {
                case EventType.MOUSE_WHEEL:
                    seleniumDriver.processMouseWheel(theWebDriver, event, target);
                    break;
                case EventType.SCROLL_EMULATION:
                    seleniumDriver.processScroll(theWebDriver, event, target);
                    break;
                case EventType.MOUSE_DOWN:
                case EventType.CLICK:
                    seleniumDriver.processMouseEvent(theWebDriver, event);
                    break;
                case EventType.KEY_UP:
                case EventType.KEY_DOWN:
                    seleniumDriver.processKeyDownKeyUpEvents(theWebDriver, event);
                    break;
                case EventType.KEY_PRESS:
                    seleniumDriver.processKeyPressEvent(theWebDriver, event);
                    break;
                default:
                    break;
                }

                seleniumDriver.waitWhileAsyncRequestsWillCompletedWithRefresh(theWebDriver, event);
                new PlayerScriptProcessor(scenario).doWaitAfterEvent(seleniumDriver, theWebDriver, event);

                throwIfBrowserHaveAnError(scenario, theWebDriver);
            }
            catch (Exception e)
            {
                processClickException(position, e);
            }
        }
        catch (Exception e)
        {
            error = true;
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        finally
        {
            //webdriver can stay null if event is ignored or bad, thus can`t be postprocessed
            if (theWebDriver != null)
            {
                if (!error)
                {
                    scenario.updateEvent(event);
                    new PlayerScriptProcessor(scenario).runStepPrePostScript(event, position, false);
                }
                makeAShot(scenario, seleniumDriver, theWebDriver, position, error);
                seleniumDriver.releaseBrowser(theWebDriver, event);
            }
            else
            {
                LOG.warn("Unable to make screenshot, because web driver is null");
            }
        }
    }

    public void play(UserScenario scenario, SeleniumDriver seleniumDriver)
    {
        play(scenario, seleniumDriver, 0, 0);
    }

    public void play(UserScenario scenario, SeleniumDriver seleniumDriver, int start, int finish)
    {
        long begin = System.currentTimeMillis();

        LOG.info("Playing the scenario");
        if (start > 0)
        {
            LOG.info("Skipping first {} events.", start);
            scenario.setPosition(start);
        }

        int maxPosition = finish > 0 ? Math.min(finish, scenario.getStepsCount()) : scenario.getStepsCount();
        LOG.info("Playing scenario. Start step: {}, finish step: {}. Steps count: {}", start, maxPosition,
                maxPosition - start);
        try {
            while (scenario.getPosition() != maxPosition) {
                LOG.info("Step " + scenario.getPosition());
                applyStep(scenario, seleniumDriver, scenario.getPosition());
                scenario.moveToNextStep();
            }
            LOG.info(String.format("Done(%d):playing", System.currentTimeMillis() - begin));
        } catch (Throwable throwable) {
            LOG.error("Playing interrupted by unhandled exception", throwable);
            throw throwable;
        } finally {
            seleniumDriver.closeWebDrivers();
        }
    }
}
