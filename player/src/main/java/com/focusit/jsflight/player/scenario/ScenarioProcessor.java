package com.focusit.jsflight.player.scenario;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.focusit.jsflight.player.config.CommonConfiguration;
import com.focusit.jsflight.player.constants.EventConstants;
import com.focusit.jsflight.player.constants.EventType;
import com.focusit.jsflight.player.script.PlayerScriptProcessor;
import com.focusit.jsflight.player.webdriver.SeleniumDriver;
import com.focusit.script.constants.ScriptBindingConstants;

/**
 * Class that really replays an event in given scenario and given selenium driver
 * Created by doki on 05.05.16.
 */
public class ScenarioProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(ScenarioProcessor.class);

    private static WebDriver getWebDriver(UserScenario scenario, SeleniumDriver seleniumDriver, JSONObject event)
    {
        boolean firefox = scenario.getConfiguration().getCommonConfiguration().isUseFirefox();
        String path = firefox ? scenario.getConfiguration().getCommonConfiguration().getFfPath() : scenario
                .getConfiguration().getCommonConfiguration().getPjsPath();
        String proxyHost = scenario.getConfiguration().getCommonConfiguration().getProxyHost();
        String proxyPort = scenario.getConfiguration().getCommonConfiguration().getProxyPort();
        String display = scenario.getConfiguration().getCommonConfiguration().getFirefoxDisplay();

        return seleniumDriver.getDriverForEvent(event, firefox, path, display, proxyHost, proxyPort);
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
    protected void hasBrowserAnError(UserScenario scenario, WebDriver wd) throws Exception
    {
        String findBrowserErrorScript = scenario.getConfiguration().getWebConfiguration().getFindBrowserErrorScript();
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
    protected void makeAShot(UserScenario scenario, SeleniumDriver seleniumDriver, WebDriver theWebDriver,
            int position, boolean isError)
    {
        if (scenario.getConfiguration().getCommonConfiguration().getMakeShots())
        {
            String screenDir = scenario.getConfiguration().getCommonConfiguration().getScreenDir();
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

        LOG.info("Current step URL: {}", event.getString(EventConstants.URL));

        new PlayerScriptProcessor(scenario).runStepPrePostScript(event, position, true);
        event = new PlayerScriptProcessor(scenario).runStepTemplating(scenario, event);

        String eventUrl = event.getString(EventConstants.URL);
        //if template processing fails for URL we cannot process this step, so we skip
        if (eventUrl.matches(".*(\\$\\{.*\\}).*"))
        {
            LOG.warn("Event at position {} cannot be processed due to url contains unprocessed templates\n"
                    + "EventId: {}\n" + "URL: {}", position, event.get(EventConstants.EVENT_ID), eventUrl);
            return;
        }

        WebDriver theWebDriver = null;
        boolean error = false;
        CommonConfiguration commonConfiguration = scenario.getConfiguration().getCommonConfiguration();
        try
        {
            if (scenario.isStepDuplicates(scenario.getConfiguration().getWebConfiguration().getDuplicationScript(),
                    event))
            {
                LOG.warn("Event duplicates previous");
                return;
            }

            String type = event.getString(EventConstants.TYPE);

            if (scenario.isEventIgnored(event) || scenario.isEventBad(event))
            {
                LOG.warn("Event is ignored or bad");
                return;
            }

            if (type.equalsIgnoreCase(EventType.SCRIPT))
            {
                new PlayerScriptProcessor(scenario).executeScriptEvent(scenario.getConfiguration()
                        .getScriptEventConfiguration().getScript(), event);
                return;
            }

            //Configure webdriver for this event, setting params here so we can change parameters while playback is
            //paused
            seleniumDriver
                    .setPageTimeoutMs(Integer.parseInt(commonConfiguration.getPageReadyTimeout()))
                    .setCheckPageJs(commonConfiguration.getCheckPageJs())

                    .setMaxElementGroovy(commonConfiguration.getMaxElementGroovy())
                    .setLookupScript(scenario.getConfiguration().getWebConfiguration().getLookupScript())
                    .setUiShownScript(commonConfiguration.getUiShownScript())
                    .setUseRandomChars(commonConfiguration.isUseRandomChars())
                    .setIntervalBetweenUiChecksMs(commonConfiguration.getIntervalBetweenUiChecksMs())
                    .setUiShowTimeoutSeconds(commonConfiguration.getUiShowTimeoutSeconds())
                    .setEmptySelections(scenario.getConfiguration().getWebConfiguration().getEmptySelections())
                    .setSelectXpath(scenario.getConfiguration().getWebConfiguration().getSelectXpath())
                    .setSelectDeterminerScript(
                            scenario.getConfiguration().getWebConfiguration().getSelectDeterminerScript())
                    .setProcessSignalScript(commonConfiguration.getProcessSignalScript())
                    .setGetFirefoxPidScript(commonConfiguration.getGetFirefoxPidScript())
                    .setFormDialogXpath(commonConfiguration.getFormOrDialogXpath());

            theWebDriver = getWebDriver(scenario, seleniumDriver, event);
            if (theWebDriver == null)
            {
                throw new NullPointerException("getWebDriver return null");
            }
            seleniumDriver.openEventUrl(theWebDriver, event);

            String target = scenario.getTargetForEvent(event);

            LOG.info("Event type: {}", type);
            LOG.info("Event {}, Display {}", position, seleniumDriver.getDriverDisplay(theWebDriver));

            seleniumDriver.waitPageReadyWithRefresh(theWebDriver, event);

            try
            {
                switch (type)
                {
                case EventType.MOUSEWHEEL:
                    seleniumDriver.processMouseWheel(theWebDriver, event, target);
                    break;
                case EventType.SCROLL_EMULATION:
                    seleniumDriver.processScroll(theWebDriver, event, target);
                    break;

                case EventType.MOUSEDOWN:
                case EventType.CLICK:
                    seleniumDriver.processMouseEvent(theWebDriver, event);
                    seleniumDriver.waitPageReadyWithRefresh(theWebDriver, event);
                    break;
                case EventType.KEY_UP:
                case EventType.KEY_DOWN:
                case EventType.KEY_PRESS:
                    seleniumDriver.processKeyboardEvent(theWebDriver, event);
                    seleniumDriver.waitPageReadyWithRefresh(theWebDriver, event);
                    break;
                default:
                    break;
                }

                makeAShot(scenario, seleniumDriver, theWebDriver, position, error);

                hasBrowserAnError(scenario, theWebDriver);
            }
            catch (Exception e)
            {
                processClickException(position, e);
            }

        }
        catch (NullPointerException e)
        {
            error = true;
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        catch (Exception e)
        {
            error = true;
            LOG.error(e.getMessage(), e);
            makeAShot(scenario, seleniumDriver, theWebDriver, position, error);
            throw new RuntimeException(e);
        }
        finally
        {
            //webdriver can stay null if event is ignored or bad, thus can`t be postprocessed
            if (!error && theWebDriver != null)
            {
                scenario.updateEvent(event);
                try
                {
                    new PlayerScriptProcessor(scenario).runStepPrePostScript(event, position, false);
                }
                catch (Exception e)
                {
                    LOG.error(e.getMessage(), e);
                    makeAShot(scenario, seleniumDriver, theWebDriver, position, true);
                    throw e;
                }

                seleniumDriver.releaseBrowser(theWebDriver, event);
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

        LOG.info("playing the scenario");
        if (start > 0)
        {
            LOG.info("skiping " + start + " events.");
            scenario.setPosition(start);
        }

        int maxPosition = finish > 0 ? finish : scenario.getStepsCount();

        do
        {
            LOG.info("Step " + scenario.getPosition());
            applyStep(scenario, seleniumDriver, scenario.getPosition());
            scenario.moveToNextStep();
        }
        while (scenario.getPosition() != maxPosition);
        LOG.info(String.format("Done(%d):playing", System.currentTimeMillis() - begin));
        seleniumDriver.closeWebDrivers();
    }
}
