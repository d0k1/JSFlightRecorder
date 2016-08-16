package player

import com.focusit.jsflight.player.scenario.UserScenario
import com.focusit.jsflight.player.webdriver.SeleniumDriver
import com.focusit.jsflight.player.webdriver.WebDriverWrapper
import org.json.JSONObject
import spock.lang.Shared
import spock.lang.Specification

class SeleniumDriverSpec extends Specification {

    @Shared
            formXp = "//*[@id='errorPageContainer']";


    def sd;

    def setup() {
        sd = new SeleniumDriver(new UserScenario())
        sd.setFormDialogXpath(formXp)
    }

    def "browser containing form is not closed"() {

        JSONObject testEvent = new JSONObject();
        testEvent.put("tabuuid", "1")

        WebDriverWrapper wd = getWd(testEvent)

        wd.get('http://localhost')

        when:
        sd.releaseBrowser(wd.getWrappedDriver(), testEvent)

        then:
        !sd.drivers.isEmpty()
    }


    def "browser without form quits"() {
        JSONObject testEvent = new JSONObject();
        testEvent.put("tabuuid", "2")

        WebDriverWrapper wd = getWd(testEvent)

        when:
        sd.releaseBrowser(wd.getWrappedDriver(), testEvent)

        then:
        sd.drivers.isEmpty()
    }


    def cleanup() {
        sd.drivers.values().each { it ->
            it.quit()
        }
        sd.drivers.clear()
    }

    WebDriverWrapper getWd(def event) {
        return sd.getDriverForEvent(event, true, '', '', '', '')
    }
}
