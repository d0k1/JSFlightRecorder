package com.focusit.jsflight.player.handler;

import com.focusit.jsflight.script.player.PlayerContext;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gallyam Biktashev on 16.02.17.
 */
public abstract class BaseEventHandler
{
    protected PlayerContext context;
    protected Map<String, String> additionalProperties = new HashMap<>();

    public abstract void handleEvent(WebDriver webDriver, JSONObject event);

    protected WebElement findTargetWebElement(WebDriver webDriver, JSONObject event, String target)
    {
        return null;
    };

    public void addAdditionalProperty(String key, String value)
    {
        additionalProperties.put(key, value);
    }
}
