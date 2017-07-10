package com.focusit.player.specs

import com.focusit.jsflight.player.configurations.ScriptsConfiguration
import com.focusit.jsflight.player.constants.EventConstants
import com.focusit.jsflight.player.handler.KeyPressEventHandler
import com.focusit.jsflight.script.ScriptEngine
import org.json.JSONObject
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.remote.RemoteWebDriver
import spock.lang.Shared

/**
 * Created by Gallyam Biktashev on 10.07.17.
 */
class KeyPressEventHandlerSpec extends BaseSpec {
    @Shared def scriptsConfig

    def setup() {
        ScriptEngine.init(ClassLoader.getSystemClassLoader())
        scriptsConfig = new ScriptsConfiguration()
        scriptsConfig.loadDefaults();
    }

    def "processKeyPress must work with CHAR_CODE field of an event"() {
        given:
        JSONObject event = getSimpleEvent()
        event.put(EventConstants.TYPE, "keypress")
        event.put(EventConstants.CHAR_CODE, 48.0)
        event.put(EventConstants.URL, "url")
        WebDriver wd = getWd(event)
        WebElement element = Mock(WebElement)

        def eventHandler = Spy(KeyPressEventHandler)
        eventHandler.addAdditionalProperty(KeyPressEventHandler.SHOULD_SKIP_KEYBOARD_SCRIPT, scriptsConfig.shouldSkipKeyboardScript)
        eventHandler.addAdditionalProperty(KeyPressEventHandler.USE_RANDOM_CHARS, Boolean.FALSE.toString())
        when:
        eventHandler.handleEvent(wd, event)
        then:
        1 * eventHandler.findTargetWebElement(_, _, _) >> element
        1 * element.sendKeys('0')
    }

    def "processKeyPress must work with CHAR field of an event"() {
        given:
        JSONObject event = getSimpleEvent()
        event.put(EventConstants.TYPE, "keypress")
        event.put(EventConstants.CHAR, '0')
        event.put(EventConstants.URL, "url")
        WebDriver wd = getWd(event)
        WebElement element = Mock(WebElement)

        def eventHandler = Spy(KeyPressEventHandler)
        eventHandler.addAdditionalProperty(KeyPressEventHandler.SHOULD_SKIP_KEYBOARD_SCRIPT, scriptsConfig.shouldSkipKeyboardScript)
        eventHandler.addAdditionalProperty(KeyPressEventHandler.USE_RANDOM_CHARS, Boolean.FALSE.toString())
        when:
        eventHandler.handleEvent(wd, event)
        then:
        1 * eventHandler.findTargetWebElement(_, _, _) >> element
        1 * element.sendKeys('0')
    }

    def "processKeyPress throws exception if event has neither CHAR nor CHAR_COD"() {
        given:
        JSONObject event = getSimpleEvent()
        event.put(EventConstants.TYPE, "keypress")
        event.put(EventConstants.URL, "url")
        WebDriver wd = getWd(event)

        def eventHandler = Spy(KeyPressEventHandler)
        eventHandler.addAdditionalProperty(KeyPressEventHandler.SHOULD_SKIP_KEYBOARD_SCRIPT, scriptsConfig.shouldSkipKeyboardScript)
        eventHandler.addAdditionalProperty(KeyPressEventHandler.USE_RANDOM_CHARS, Boolean.FALSE.toString())
        when:
        eventHandler.handleEvent(wd, event)
        then:
        1 * eventHandler.findTargetWebElement(_, _, _) >> Mock(WebElement)
        thrown(IllegalStateException)
    }

    JSONObject getSimpleEvent() {
        JSONObject event = new JSONObject()
        event.put("tabuuid", "2")
        event.put("window.width", 1500)
        event.put("window.height", 1500)
        return event
    }

    WebDriver getWd(JSONObject event) {
        return Mock(RemoteWebDriver);
    }
}
