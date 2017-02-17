package com.focusit.jsflight.player.handler;

import com.focusit.jsflight.script.player.PlayerContext;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

/**
 * Created by Gallyam Biktashev on 16.02.17.
 */
public abstract class BaseEventHandler
{
    protected PlayerContext context;
    protected Map<String, String> additionalProperties;

    public abstract void handleEvent(WebDriver webDriver, JSONObject event);

    public WebElement findTargetWebElement(WebDriver webDriver, JSONObject event, String target)
    {
        return null;
    };

    public void addAdditionalProperties(Map<String, String> properties)
    {
        additionalProperties.putAll(properties);
    }
}
