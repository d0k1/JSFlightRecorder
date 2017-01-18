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

    @Shared
            formXp = "//*[@id='errorPageContainer']";


    SeleniumDriver sd;

    def setup() {
        ScriptEngine.init(ClassLoader.getSystemClassLoader())
        sd = new SeleniumDriver(new UserScenario())
        sd.setKeepBrowserXpath(formXp)
        sd.setGetWebDriverPidScript('"echo 1".execute().text');
        sd.setSendSignalToProcessScript("println()");
    }

    def "browser containing form is not closed"() {

        JSONObject testEvent = new JSONObject();
        testEvent.put("tabuuid", "1")
        testEvent.put("window.width", 1500);
        testEvent.put("window.height", 1500);

        WebDriverWrapper wd = getWd(testEvent)

        wd.get('http://localhost')

        when:
        sd.releaseBrowser(wd.getWrappedDriver(), testEvent)

        then:
        !sd.tabUuidDrivers.isEmpty()
    }


    def "browser without form quits"() {
        JSONObject testEvent = new JSONObject();
        testEvent.put("tabuuid", "2");
        testEvent.put("uuid", "123");
        testEvent.put("window.width", 1500);
        testEvent.put("window.height", 1500);
        WebDriverWrapper wd = getWd(testEvent)
        when:
        sd.releaseBrowser(wd.getWrappedDriver(), testEvent)
        then:
        sd.tabUuidDrivers.isEmpty()
    }


    def cleanup() {
        sd.tabUuidDrivers.values().each { it ->
            it.quit()
        }
        sd.tabUuidDrivers.clear()
    }

    WebDriverWrapper getWd(JSONObject event) {
        return sd.getDriverForEvent(event, BrowserType.FIREFOX, '', '', 0)
    }
}
