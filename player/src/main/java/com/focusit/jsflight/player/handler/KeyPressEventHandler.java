package com.focusit.jsflight.player.handler;

import com.focusit.jsflight.player.constants.EventConstants;
import com.focusit.jsflight.player.scenario.UserScenario;
import com.focusit.jsflight.player.script.PlayerScriptProcessor;
import com.focusit.jsflight.script.constants.ScriptBindingConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import static com.focusit.jsflight.player.webdriver.SeleniumDriver.NO_OP_ELEMENT;

/**
 * Created by gallyamb on 11-Mar-17.
 */
public class KeyPressEventHandler extends BaseEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(KeyPressEventHandler.class);

    public static final String SHOULD_SKIP_KEYBOARD_SCRIPT = "shouldSkipKeyboardScript";
    public static final String USE_RANDOM_CHARS = "useRandomChars";

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

        if (!event.has(EventConstants.CHAR) && !event.has(EventConstants.CHAR_CODE))
        {
            throw new IllegalStateException("Keypress event don't have a char");
        }

        String keys;

        if (event.has(EventConstants.CHAR))
        {
            keys = event.getString(EventConstants.CHAR);
        }
        else
        {
            char ch = (char)event.getBigDecimal(EventConstants.CHAR_CODE).intValue();
            try {
                keys = (Boolean.valueOf(additionalProperties.get(USE_RANDOM_CHARS))
                    ? new RandomStringGenerator()
                    : new CharStringGenerator()).getAsString(ch);
            } catch (UnsupportedEncodingException e) {
                LOG.error("Can't get char in event with id {}", event.get(EventConstants.EVENT_ID));
                keys = "ERROR";
            }
        }

        LOG.info("Trying to fill input with: {}", keys);
        if (event.has(EventConstants.IFRAME_XPATHS) || event.has(EventConstants.IFRAME_INDICES))
        {
            LOG.info("Input is iframe");
            element.sendKeys(keys);
        }
        else
        {
            LOG.info("Input is ordinary input");
            String prevText = element.getAttribute("value");
            //If current value indicates a placeholder it must be discarded

            // TODO placeholders
            element.sendKeys(keys);
        }
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
    private static abstract class StringGenerator
    {
        protected static final Logger LOG = LoggerFactory.getLogger(StringGenerator.class);

        public String getAsString(char ch) throws UnsupportedEncodingException
        {
            String result = generate(ch);
            LOG.info("Returning {}", result);
            return result;
        }

        public abstract String generate(char ch) throws UnsupportedEncodingException;
    }

    private static class CharStringGenerator extends StringGenerator
    {
        @Override
        public String generate(char ch) throws UnsupportedEncodingException
        {
            return String.valueOf(ch);
        }

    }

    private static class RandomStringGenerator extends StringGenerator
    {
        @Override
        public String generate(char ch)
        {
            return RandomStringUtils.randomAlphanumeric(1);
        }

    }

}
