package com.focusit.player.specs

import com.focusit.jsflight.player.configurations.ScriptsConfiguration
import com.focusit.jsflight.player.constants.BrowserType
import com.focusit.jsflight.player.constants.EventConstants
import com.focusit.jsflight.player.scenario.UserScenario
import com.focusit.jsflight.player.webdriver.SeleniumDriver
import com.focusit.jsflight.script.ScriptEngine
import org.json.JSONObject
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

/**
 * Created by Gallyam Biktashev on 17.02.17.
 */
class WebDriverSpec extends BaseSpec {
    SeleniumDriver seleniumDriver
    JSONObject event

    def setup() {
        seleniumDriver = new SeleniumDriver(new UserScenario().getContext())
        def scriptsConfiguration = new ScriptsConfiguration();
        scriptsConfiguration.loadDefaults()
        seleniumDriver.setGetWebDriverPidScript(scriptsConfiguration.getGetWebDriverPidScript())
            .setSendSignalToProcessScript(scriptsConfiguration.getSendSignalToProcessScript())
        ScriptEngine.init(ClassLoader.getSystemClassLoader())


        event = new JSONObject()
        event.put(EventConstants.TAB_UUID, "123")
        event.put("window.width", 1000)
        event.put("window.height", 1000)
    }

    def "should correctly open"() {
        when:
        WebDriver webDriver = seleniumDriver.getDriverForEvent(event, BrowserType.FIREFOX, null, null, null)

        then:
        noExceptionThrown()
        webDriver.currentUrl == "about:blank"
    }

    def "should found elements"() {
        when:
        WebDriver webDriver = seleniumDriver.getDriverForEvent(event, BrowserType.FIREFOX, null, null, null)
        WebElement element = webDriver.findElement(By.xpath("//body"))

        then:
        noExceptionThrown()
        element != null
        element.displayed
    }

    def "should throw exception when no such element at the page"() {
        when:
        WebDriver webDriver = seleniumDriver.getDriverForEvent(event, BrowserType.FIREFOX, null, null, null)
        webDriver.findElement(By.xpath("//non-existing-element"))

        then:
        thrown(NoSuchElementException)
    }

    def cleanup() {
        seleniumDriver.closeWebDrivers()
    }
}
