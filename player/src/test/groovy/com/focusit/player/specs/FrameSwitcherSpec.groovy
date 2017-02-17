package com.focusit.player.specs

import com.focusit.jsflight.player.constants.EventConstants
import com.focusit.jsflight.player.iframe.FrameSwitcher
import org.json.JSONObject
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
/**
 * Created by Gallyam Biktashev on 16.02.17.
 */
class FrameSwitcherSpec extends BaseSpec {
    WebDriver webDriver
    WebDriver.TargetLocator locator
    String windowHandle
    JSONObject event

    def setup() {
        webDriver = Mock()
        locator = Mock()
        windowHandle = "windowHandle"

        event = new JSONObject()
        event.put(EventConstants.EVENT_ID, 5)
    }

    def "should switch into iframe by indices"() {
        given:
        event.put(EventConstants.IFRAME_INDICES, "1.2.3")

        when:
        FrameSwitcher.switchToWorkingFrame(webDriver, event)

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
        event.put(EventConstants.IFRAME_XPATHS, "//iframe[0]||iframe[1]||/path/to/frame")

        when:
        FrameSwitcher.switchToWorkingFrame(webDriver, event)

        then:
        1 * webDriver.getWindowHandle() >> windowHandle
        4 * webDriver.switchTo() >> locator
        1 * locator.window(windowHandle)
        1 * webDriver.findElement(By.xpath("//iframe[0]"))
        1 * webDriver.findElement(By.xpath("iframe[1]"))
        1 * webDriver.findElement(By.xpath("/path/to/frame"))
        3 * locator.frame(_)
        0 * _
    }

    def "should switch to top window when frame parameters omitted"() {
        when:
        FrameSwitcher.switchToWorkingFrame(webDriver, event)

        then:
        1 * webDriver.getWindowHandle() >> windowHandle
        1 * webDriver.switchTo() >> locator
        1 * locator.window(windowHandle)
        0 * _
    }

    def "should switch to top window when frame parameters is empty"() {
        given:
        event.put(EventConstants.IFRAME_XPATHS, "")
        event.put(EventConstants.IFRAME_INDICES, "")

        when:
        FrameSwitcher.switchToWorkingFrame(webDriver, event)

        then:
        1 * webDriver.getWindowHandle() >> windowHandle
        1 * webDriver.switchTo() >> locator
        1 * locator.window(windowHandle)
        0 * _
    }
}
