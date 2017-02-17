package com.focusit.player.specs

import com.focusit.jsflight.player.constants.EventConstants
import com.focusit.jsflight.player.scenario.UserScenario
import com.focusit.jsflight.player.webdriver.SeleniumDriver
import com.focusit.jsflight.script.ScriptEngine
import org.json.JSONObject
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.remote.RemoteWebDriver
import spock.lang.Shared

@SuppressWarnings("GroovyAssignabilityCheck")
class SeleniumDriverSpec extends BaseSpec {
    @Shared keepBrowserXpath = "//*[@id='errorPageContainer']"

    SeleniumDriver sd

    def setup() {
        ScriptEngine.init(ClassLoader.getSystemClassLoader())
        sd = new SeleniumDriver(new UserScenario())
        sd.setKeepBrowserXpath(keepBrowserXpath)
        sd.setGetWebDriverPidScript('"echo 1".execute().text')
        sd.setSendSignalToProcessScript("println()")
        sd.setIsAsyncRequestsCompletedScript("return true")
        sd.setSkipKeyboardScript("return false")
        sd.setUseRandomStringGenerator(false)
        sd.setPlaceholders("test")
        sd.setElementLookupScript('return webdriver.findElement(org.openqa.selenium.By.tagName("body"));')
    }

    def "should not close browser with element matched by keepBrowserXpath"() {
        given:
        JSONObject testEvent = getSimpleEvent()
        testEvent.put(EventConstants.TAB_UUID, "1")
        WebDriver wd = getWd(testEvent)
        def locator = Mock(WebDriver.TargetLocator)

        when:
        sd.releaseBrowser(wd, testEvent)

        then:
        1 * wd.findElements(_) >> [Mock(WebElement)]
        1 * wd.switchTo() >> locator
        1 * wd.getWindowHandle() >> "handle"
        1 * locator.window(_)
        0 * wd.quit()
    }

    def "should close browser without element matched by keepBrowserXpath"() {
        given:
        JSONObject testEvent = getSimpleEvent()
        testEvent.put(EventConstants.TAB_UUID, "2")
        testEvent.put(EventConstants.TAG, "123")
        WebDriver wd = getWd(testEvent)
        def locator = Mock(WebDriver.TargetLocator)

        when:
        sd.releaseBrowser(wd, testEvent)

        then:
        1 * wd.findElements(_) >> []
        1 * wd.switchTo() >> locator
        1 * wd.getWindowHandle() >> "handle"
        1 * locator.window(_)
        1 * wd.quit()
    }

    def "processKeyPress must work with CHAR_CODE field of an event"() {
        given:
        JSONObject event = getSimpleEvent()
        event.put(EventConstants.TYPE, "keypress")
        event.put(EventConstants.CHAR_CODE, 48.0)
        event.put(EventConstants.URL, "url")
        WebDriver wd = getWd(event)
        when:
        sd.processKeyPressEvent(wd, event)
        then:
        1 * wd.executeScript(_) >> "true"
        1 * wd.findElement(_) >> Mock(WebElement)
    }

    def "processKeyPress must work with CHAR field of an event"() {
        given:
        JSONObject event = getSimpleEvent()
        event.put(EventConstants.TYPE, "keypress")
        event.put(EventConstants.CHAR, '0')
        event.put(EventConstants.URL, "url")
        WebDriver wd = getWd(event)
        when:
        sd.processKeyPressEvent(wd, event)
        then:
        1 * wd.executeScript(_) >> "true"
        1 * wd.findElement(_) >> Mock(WebElement)
    }

    def "processKeyPress throws exception if event has neither CHAR nor CHAR_COD"() {
        given:
        JSONObject event = getSimpleEvent()
        event.put(EventConstants.TYPE, "keypress")
        event.put(EventConstants.URL, "url")
        WebDriver wd = getWd(event)
        when:
        sd.processKeyPressEvent(wd, event)
        then:
        1 * wd.executeScript(_) >> "true"
        1 * wd.findElement(_) >> Mock(WebElement)
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
