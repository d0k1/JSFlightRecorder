package com.focusit.jsflight.player.handler;

import com.focusit.jsflight.player.scenario.UserScenario;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

import com.focusit.jsflight.player.constants.EventConstants;
import org.slf4j.LoggerFactory;

/**
 * Created by Gallyam Biktashev on 16.02.17.
 */
public class MouseWheelEventHandler extends BaseEventHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(MouseWheelEventHandler.class);

    @Override
    public void handleEvent(WebDriver webDriver, JSONObject event)
    {

        if (!event.has(EventConstants.DELTA_Y))
        {
            LOG.error("event hasn't deltaY - can't process scroll", new Exception());
            return;
        }
        String target = UserScenario.getTargetForEvent(event);
        WebElement el = findTargetWebElement(webDriver, event, target);

        // TODO fix this hardcoded condition
        //Web lookup script MUST return //html element if scroll occurs not in a popup
        if (!el.getTagName().equalsIgnoreCase("html"))
        {
            ((JavascriptExecutor)webDriver).executeScript("arguments[0].scrollTop = arguments[0].scrollTop + arguments[1]",
                    el, event.getInt(EventConstants.DELTA_Y));
        }
        else
        {
            ((JavascriptExecutor)webDriver).executeScript("window.scrollBy(0, arguments[0])",
                    event.getInt(EventConstants.DELTA_Y));
        }
    }
}
