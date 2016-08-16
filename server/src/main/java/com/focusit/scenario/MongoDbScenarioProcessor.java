package com.focusit.scenario;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.focusit.jsflight.player.scenario.ScenarioProcessor;
import com.focusit.jsflight.player.scenario.UserScenario;
import com.focusit.jsflight.player.webdriver.SeleniumDriver;
import com.focusit.player.ErrorInBrowserPlaybackException;
import com.focusit.service.MongoDbStorageService;

/**
 * This class user scenario process based on mongodb
 * It add some specific to ordinal scenario processor such as:
 * - storing screenshot in mongodb's gridfs
 * - throws an exception if something went wrong to interrupt a player
 * - validates browser's DOM looking for modal dialogs with an error
 * Created by doki on 12.05.16.
 */
public class MongoDbScenarioProcessor extends ScenarioProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbScenarioProcessor.class);
    private MongoDbStorageService screenshotsService;

    public MongoDbScenarioProcessor(MongoDbStorageService screenshotsService)
    {
        this.screenshotsService = screenshotsService;
    }

    @Override
    protected void hasBrowserAnError(UserScenario scenario, WebDriver wd) throws Exception
    {
        try
        {
            super.hasBrowserAnError(scenario, wd);
        }
        catch (IllegalStateException e)
        {
            LOG.error(e.toString(), e);
            throw new ErrorInBrowserPlaybackException(e.getMessage());
        }
    }

    @Override
    public void applyStep(UserScenario scenario, SeleniumDriver seleniumDriver, int position)
    {
        LOG.info("Applying event: " + scenario.getStepAt(position).get("eventId"));
        super.applyStep(scenario, seleniumDriver, position);
    }

    @Override
    protected void processClickException(int position, Exception ex) throws Exception
    {
        super.processClickException(position, ex);
        throw ex;
    }

    @Override
    protected void makeAShot(UserScenario scenario, SeleniumDriver seleniumDriver, WebDriver theWebDriver,
            int position, boolean isError)
    {
        MongoDbScenario mongoDbScenario = (MongoDbScenario)scenario;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            seleniumDriver.makeAShot(theWebDriver, baos);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray()))
            {
                screenshotsService.storeScreenshot(mongoDbScenario, position, bais, isError);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
