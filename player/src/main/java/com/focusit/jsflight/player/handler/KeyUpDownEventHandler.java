package com.focusit.jsflight.player.handler;

import com.focusit.jsflight.player.constants.EventConstants;
import com.focusit.jsflight.player.scenario.UserScenario;
import com.focusit.jsflight.player.script.PlayerScriptProcessor;
import com.focusit.jsflight.script.constants.ScriptBindingConstants;
import org.json.JSONObject;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.focusit.jsflight.player.webdriver.SeleniumDriver.NO_OP_ELEMENT;

/**
 * Created by gallyamb on 11-Mar-17.
 */
public class KeyUpDownEventHandler extends BaseEventHandler {
    private static final Logger LOG = LoggerFactory.getLogger(KeyUpDownEventHandler.class);
    public static final String SHOULD_SKIP_KEYBOARD_SCRIPT = "shouldSkipKeyboardScript";

    private static final Map<String, Keys> SPECIAL_KEYS_MAPPING = new HashMap<>();
    static
    {
        SPECIAL_KEYS_MAPPING.put(EventConstants.CTRL_KEY, Keys.CONTROL);
        SPECIAL_KEYS_MAPPING.put(EventConstants.ALT_KEY, Keys.ALT);
        SPECIAL_KEYS_MAPPING.put(EventConstants.SHIFT_KEY, Keys.SHIFT);
        SPECIAL_KEYS_MAPPING.put(EventConstants.META_KEY, Keys.META);

    }


    private boolean isNoOp(JSONObject event, WebElement element)
    {
        if (element.equals(NO_OP_ELEMENT))
        {
            LOG.warn("Non operational element returned. Aborting event {} processing. Target xpath {}",
                    event.get(EventConstants.EVENT_ID), event.getString(EventConstants.SECOND_TARGET));
            return true;
        }
        return false;
    }

    private boolean skipKeyboardForElement(WebElement element)
    {
        //TODO remove this when recording of cursor in text box is implemented
        Map<String, Object> bindings = PlayerScriptProcessor.getEmptyBindingsMap();
        bindings.put(ScriptBindingConstants.ELEMENT, element);
        return new PlayerScriptProcessor(context).executeGroovyScript(
                additionalProperties.get(SHOULD_SKIP_KEYBOARD_SCRIPT), bindings, Boolean.class);
    }

    @Override
    public void handleEvent(WebDriver webDriver, JSONObject event) {
        WebElement element = findTargetWebElement(webDriver, event, UserScenario.getTargetForEvent(event));

        if (isNoOp(event, element))
        {
            return;
        }

        //TODO remove this when recording of cursor in text box is implemented
        if (skipKeyboardForElement(element))
        {
            LOG.warn("Keyboard processing for non empty Date is disabled");
            return;
        }

        if (!event.has(EventConstants.KEY_CODE) && !event.has(EventConstants.CHAR_CODE))
        {
            throw new IllegalStateException("Keydown/Keyup event don't have keyCode/charCode property");
        }

        Actions actions = new Actions(webDriver);

        SPECIAL_KEYS_MAPPING.keySet().forEach(property -> {
            if (event.getBoolean(property))
            {
                actions.keyDown(element, SPECIAL_KEYS_MAPPING.get(property));
            }
        });

        // Backward compatibility
        int code = event.getInt(EventConstants.CHAR_CODE);
        if (code == 0)
        {
            code = event.getInt(EventConstants.KEY_CODE);
        }
        switch (code)
        {
            case 8:
                actions.sendKeys(element, Keys.BACK_SPACE);
                break;
            case 27:
                actions.sendKeys(element, Keys.ESCAPE);
                break;
            case 46:
                actions.sendKeys(element, Keys.DELETE);
                break;
            case 13:
                actions.sendKeys(element, Keys.ENTER);
                break;
            case 37:
                actions.sendKeys(element, Keys.ARROW_LEFT);
                break;
            case 38:
                actions.sendKeys(element, Keys.ARROW_UP);
                break;
            case 39:
                actions.sendKeys(element, Keys.ARROW_RIGHT);
                break;
            case 40:
                actions.sendKeys(element, Keys.ARROW_DOWN);
                break;
        }

        SPECIAL_KEYS_MAPPING.keySet().forEach(property -> {
            if (event.getBoolean(property))
            {
                actions.keyUp(element, SPECIAL_KEYS_MAPPING.get(property));
            }
        });

        try
        {
            actions.perform();
        }
        catch (Exception ex)
        {
            // TODO Fix correctly
            LOG.error("Sending keys to and invisible element. must have JS workaround: " + ex.toString(), ex);
        }
    }

}
