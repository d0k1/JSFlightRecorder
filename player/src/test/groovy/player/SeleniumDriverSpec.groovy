package player

import com.focusit.jsflight.player.constants.BrowserType
import com.focusit.jsflight.player.scenario.UserScenario
import com.focusit.jsflight.player.webdriver.SeleniumDriver
import com.focusit.jsflight.player.webdriver.WebDriverWrapper
import com.focusit.jsflight.script.ScriptEngine
import org.json.JSONObject
import spock.lang.Shared
import spock.lang.Specification

class SeleniumDriverSpec extends Specification {
    @Shared keepBrowserXpath = "//*[@id='errorPageContainer']";

    SeleniumDriver sd;

    def setup() {
        ScriptEngine.init(ClassLoader.getSystemClassLoader())
        sd = new SeleniumDriver(new UserScenario())
        sd.setKeepBrowserXpath(keepBrowserXpath)
        sd.setGetWebDriverPidScript('"echo 1".execute().text');
        sd.setSendSignalToProcessScript("println()");
        sd.setIsAsyncRequestsCompletedScript("return true");
        sd.setSkipKeyboardScript("return false");
        sd.setUseRandomStringGenerator(false);
        sd.setPlaceholders("test");
        sd.setElementLookupScript('return webdriver.findElement(org.openqa.selenium.By.tagName("body"));');
    }

    def "should not close browser with element matched by keepBrowserXpath"() {
        JSONObject testEvent = getSimpleEvent();
        testEvent.put("tabuuid", "1")
        WebDriverWrapper wd = getWd(testEvent)
        wd.get('http://localhost')

        when:
        sd.releaseBrowser(wd.getWrappedDriver(), testEvent)

        then:
        !sd.tabUuidDrivers.isEmpty()
    }

    def "should close browser without element matched by keepBrowserXpath"() {
        JSONObject testEvent = getSimpleEvent();
        testEvent.put("tabuuid", "2");
        testEvent.put("uuid", "123");
        WebDriverWrapper wd = getWd(testEvent)
        when:
        sd.releaseBrowser(wd.getWrappedDriver(), testEvent)
        then:
        sd.tabUuidDrivers.isEmpty()
    }

    def "processKeyPress must work with CHAR_CODE field of an event"() {
        given:
        JSONObject event = getSimpleEvent();
        event.put("type", "keypress");
        event.put("charCode", 48.0);
        when:
        WebDriverWrapper wd = getWd(event);
        sd.processKeyPressEvent(wd, event);
        then:
        !sd.tabUuidDrivers.isEmpty()
    }

    def "processKeyPress must work with CHAR field of an event"() {
        given:
        JSONObject event = getSimpleEvent();
        event.put("type", "keypress");
        event.put("char", "0");
        when:
        WebDriverWrapper wd = getWd(event);
        sd.processKeyPressEvent(wd, event);
        then:
        !sd.tabUuidDrivers.isEmpty()
    }

    def "processKeyPress throws exception if event has neither CHAR nor CHAR_COD"() {
        given:
        JSONObject event = getSimpleEvent();
        event.put("type", "keypress");
        when:
        WebDriverWrapper wd = getWd(event);
        sd.processKeyPressEvent(wd, event);
        then:
        IllegalStateException ex = thrown()
    }

    def cleanup() {
        sd.tabUuidDrivers.values().each { it ->
            it.quit()
        }
        sd.tabUuidDrivers.clear()
    }

    JSONObject getSimpleEvent() {
        JSONObject event = new JSONObject();
        event.put("tabuuid", "2");
        event.put("window.width", 1500);
        event.put("window.height", 1500);
        return event;
    }

    WebDriverWrapper getWd(JSONObject event) {
        String ffPath = System.getProperty("test.ff.path");
        if (ffPath == null) {
            ffPath = '';
        }
        return sd.getDriverForEvent(event, BrowserType.FIREFOX, ffPath, '', 0)
    }
}
