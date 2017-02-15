package com.focusit.player.specs

import com.focusit.jsflight.player.constants.EventConstants
import com.focusit.jsflight.player.scenario.UserScenario
import com.focusit.jsflight.player.webdriver.SeleniumDriver
import com.focusit.jsflight.script.ScriptEngine
import org.json.JSONObject
import org.openqa.selenium.By
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
        WebDriver webDriver = getWd(testEvent)
        def locator = Mock(WebDriver.TargetLocator)

        when:
        sd.releaseBrowser(webDriver, testEvent)

        then:
        1 * webDriver.findElements(_) >> [Mock(WebElement)]
        1 * webDriver.switchTo() >> locator
        1 * webDriver.getWindowHandle() >> "handle"
        1 * locator.window(_)
        0 * webDriver.quit()
    }

    def "should close browser without element matched by keepBrowserXpath"() {
        given:
        JSONObject testEvent = getSimpleEvent()
        testEvent.put(EventConstants.TAB_UUID, "2")
        testEvent.put(EventConstants.TAG, "123")
        WebDriver webDriver = getWd(testEvent)
        def locator = Mock(WebDriver.TargetLocator)

        when:
        sd.releaseBrowser(webDriver, testEvent)

        then:
        1 * webDriver.findElements(_) >> []
        1 * webDriver.switchTo() >> locator
        1 * webDriver.getWindowHandle() >> "handle"
        1 * locator.window(_)
        1 * webDriver.quit()
    }

    def "processKeyPress must work with CHAR_CODE field of an event"() {
        given:
        JSONObject event = getSimpleEvent()
        event.put(EventConstants.TYPE, "keypress")
        event.put(EventConstants.CHAR_CODE, 48.0)
        event.put(EventConstants.URL, "url")
        WebDriver wd = getWd(event)
        WebElement element = Mock(WebElement)
        when:
        sd.processKeyPressEvent(wd, event)
        then:
        1 * wd.findElement(_) >> element
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
        when:
        sd.processKeyPressEvent(wd, event)
        then:
        1 * wd.findElement(_) >> element
        1 * element.sendKeys('0')
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
        1 * wd.findElement(_) >> Mock(WebElement)
        thrown(IllegalStateException)
    }

    def "should switch into iframe by indices"() {
        given:
        JSONObject event = getSimpleEvent()
        event.put(EventConstants.EVENT_ID, 5)
        event.put(EventConstants.IFRAME_INDICES, "1.2.3")
        WebDriver webDriver = Mock(WebDriver)
        WebDriver.TargetLocator locator = Mock(WebDriver.TargetLocator)
        def windowHandle = "windowHandle"

        when:
        SeleniumDriver.switchToWorkingFrame(webDriver, event)

        then:
        1 * webDriver.getWindowHandle() >> windowHandle
        4 * webDriver.switchTo() >> locator
        1 * locator.window(windowHandle)
        1 * locator.frame(1)
        1 * locator.frame(2)
        1 * locator.frame(3)
        0 * _
    }

    def "should switch into iframe by xpaths"() {
        given:
        JSONObject event = getSimpleEvent()
        event.put(EventConstants.EVENT_ID, 5)
        event.put(EventConstants.IFRAME_XPATHS, "//iframe[0]||iframe[1]||/path/to/frame")
        WebDriver webDriver = Mock(WebDriver)
        WebDriver.TargetLocator locator = Mock(WebDriver.TargetLocator)
        def windowHandle = "windowHandle"

        when:
        SeleniumDriver.switchToWorkingFrame(webDriver, event)

        then:
        1 * webDriver.getWindowHandle() >> windowHandle
        4 * webDriver.switchTo() >> locator
        1 * locator.window(windowHandle)
        1 * webDriver.findElement(By.xpath("//iframe[0]")) >> Mock(WebElement)
        1 * webDriver.findElement(By.xpath("iframe[1]")) >> Mock(WebElement)
        1 * webDriver.findElement(By.xpath("/path/to/frame")) >> Mock(WebElement)
        3 * locator.frame(_ as WebElement)
        0 * _
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
