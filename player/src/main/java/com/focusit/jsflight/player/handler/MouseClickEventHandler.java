package com.focusit.jsflight.player.handler;

import com.focusit.jsflight.player.constants.EventConstants;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.focusit.jsflight.player.scenario.UserScenario;
import com.focusit.jsflight.player.script.PlayerScriptProcessor;

/**
 * Created by Gallyam Biktashev on 16.02.17.
 */
public class MouseClickEventHandler extends BaseEventHandler
{
    public static final String IS_SELECT_ELEMENT_SCRIPT = "isSelectElementScript";
    public static final String SELECT_XPATH = "selectXpath";
    private static final Logger LOG = LoggerFactory.getLogger(MouseClickEventHandler.class);

    @Override
    public void handleEvent(WebDriver webDriver, JSONObject event)
    {
        WebElement element = findTargetWebElement(webDriver, event, UserScenario.getTargetForEvent(event));

        ensureElementInWindow(webDriver, element);
        boolean isSelect = new PlayerScriptProcessor(context).executeSelectDeterminerScript(
                additionalProperties.get(IS_SELECT_ELEMENT_SCRIPT), webDriver, element);
        click(webDriver, event, element);
        if (isSelect)
        {
            //Wait for select to popup
            LOG.debug("Mouse event is kind of select");
            waitElement(webDriver, additionalProperties.get(SELECT_XPATH));
        }

    }

    public WebElement waitElement(WebDriver wd, String xpath)
    {
        try
        {
            return new WebDriverWait(wd, 20L, 500).until((ExpectedCondition<WebElement>) input -> {
                try
                {
                    return wd.findElement(By.xpath(xpath));
                }
                catch (NoSuchElementException e)
                {
                    return null;
                }
            });
        }
        catch (TimeoutException e)
        {
            throw new NoSuchElementException("Element was not found within timeout. Xpath " + xpath);
        }
    }

    private void ensureElementInWindow(WebDriver wd, WebElement element)
    {
        int windowHeight = wd.manage().window().getSize().getHeight();
        int elementY = element.getLocation().getY();
        if (elementY > windowHeight)
        {
            //Using division of the Y coordinate by 2 ensures target element visibility in the browser view
            //anyway TODO think of not using hardcoded constants in scrolling
            String scrollScript = "window.scrollTo(0, " + elementY / 2 + ");";
            ((JavascriptExecutor)wd).executeScript(scrollScript);
        }
    }

    private void click(WebDriver wd, JSONObject event, WebElement element)
    {
        if (element.isDisplayed())
        {

            if (event.getInt(EventConstants.BUTTON) == 2)
            {
                try
                {
                    new Actions(wd).contextClick(element).perform();
                }
                catch (WebDriverException ex)
                {
                    try
                    {
                        LOG.warn("Error simulation right click. Retrying after 2 sec.");
                        Thread.sleep(2000);

                        new Actions(wd).contextClick(element).perform();
                    }
                    catch (Exception e)
                    {
                        LOG.error("Error while simulating right click.", e);
                    }
                }
            }
            else
            {
                element.click();
            }
        }
        else
        {
            JavascriptExecutor executor = (JavascriptExecutor)wd;
            executor.executeScript("arguments[0].click();", element);
        }
    }
}
