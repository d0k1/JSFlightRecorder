package com.focusit.jsflight.player.iframe;

import com.focusit.jsflight.player.constants.EventConstants;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Gallyam Biktashev on 16.02.17.
 */
public class FrameSwitcher
{

    private static final Logger LOG = LoggerFactory.getLogger(FrameSwitcher.class);

    public static void switchToWorkingFrame(WebDriver webDriver, JSONObject event)
    {
        switchToTopWindow(webDriver);
        if (StringUtils.isBlank(event.optString(EventConstants.IFRAME_XPATHS))
                && StringUtils.isBlank(event.optString(EventConstants.IFRAME_INDICES)))
        {
            LOG.warn("Event with id {} has neither frame xpaths nor frame indices",
                    event.getInt(EventConstants.EVENT_ID));
        }
        else
        {
            List<String> frameXpaths = Arrays.stream(event.optString(EventConstants.IFRAME_XPATHS).split("\\|\\|"))
                    .filter(s -> !s.isEmpty()).collect(Collectors.toList());
            List<Integer> frameIndices = Arrays.stream(event.optString(EventConstants.IFRAME_INDICES).split("\\."))
                    .filter((s) -> !s.isEmpty()).map(Integer::parseInt).collect(Collectors.toList());
            LOG.info("Switching to frame {}({})", frameIndices, frameXpaths);
            switchToFrame(webDriver, frameIndices, frameXpaths);
        }

    }

    public static void switchToTopWindow(WebDriver webDriver)
    {
        LOG.info("Switching to main window");
        webDriver.switchTo().window(webDriver.getWindowHandle());
    }

    private static void switchToFrame(WebDriver webDriver, List<Integer> frameIndices, List<String> frameXpaths)
    {
        if (!frameIndices.isEmpty())
        {
            try
            {
                switchToFrameByIndices(webDriver, frameIndices);
                return;
            }
            catch (Exception ignored)
            {
                LOG.warn("Switching to frame by index was failed");
            }
        }
        try {
            switchToFrameByXpaths(webDriver, frameXpaths);
        }
        catch (Exception ignored)
        {
            LOG.error("Switching to frame by xpath was failed");
            switchToTopWindow(webDriver);
        }
    }

    private static void switchToFrameByIndices(WebDriver webDriver, List<Integer> path)
    {
        LOG.info("Switching to frame by indices");
        for (Integer i : path)
        {
            webDriver.switchTo().frame(i);
        }
    }

    private static void switchToFrameByXpaths(WebDriver webDriver, List<String> path)
    {
        LOG.info("Switching to frame by xpaths");
        for (String xpath : path)
        {
            WebElement frame = webDriver.findElement(By.xpath(xpath));
            webDriver.switchTo().frame(frame);
        }
    }
}
