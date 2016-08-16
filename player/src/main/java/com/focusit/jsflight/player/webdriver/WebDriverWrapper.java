package com.focusit.jsflight.player.webdriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.HasInputDevices;
import org.openqa.selenium.interactions.Keyboard;
import org.openqa.selenium.interactions.Mouse;
import org.openqa.selenium.internal.WrapsDriver;

/**
 * Created by dkolmogortsev on 15.06.16.
 * Wrapper for {@link WebDriver} which produces {@link LocatedElement}
 * Implements only several interfaces of {@link org.openqa.selenium.remote.RemoteWebDriver}
 * only used in player.
 * Implement others on demand
 */
public class WebDriverWrapper implements WebDriver, JavascriptExecutor, TakesScreenshot, WrapsDriver, HasInputDevices
{

    private WebDriver driver;

    private WebDriverWrapper(WebDriver driver)
    {
        this.driver = driver;
    }

    public static WebDriverWrapper wrap(WebDriver driver)
    {
        return new WebDriverWrapper(driver);
    }

    @Override
    public Object executeScript(String script, Object... args)
    {
        return ((JavascriptExecutor)driver).executeScript(script, args);
    }

    @Override
    public Object executeAsyncScript(String script, Object... args)
    {
        return ((JavascriptExecutor)driver).executeAsyncScript(script, args);
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException
    {
        return ((TakesScreenshot)driver).getScreenshotAs(target);
    }

    @Override
    public void get(String url)
    {
        driver.get(url);
    }

    @Override
    public String getCurrentUrl()
    {
        return driver.getCurrentUrl();
    }

    @Override
    public String getTitle()
    {
        return driver.getTitle();
    }

    @Override
    public List<WebElement> findElements(By by)
    {
        List<WebElement> result = new ArrayList<>();
        for (WebElement element : driver.findElements(by))
        {
            result.add(new LocatedElement(element, this, by));
        }
        return result;
    }

    @Override
    public WebElement findElement(By by)
    {
        return new LocatedElement(driver.findElement(by), this, by);
    }

    @Override
    public String getPageSource()
    {
        return driver.getPageSource();
    }

    @Override
    public void close()
    {
        driver.close();
    }

    @Override
    public void quit()
    {
        driver.quit();
    }

    @Override
    public Set<String> getWindowHandles()
    {
        return driver.getWindowHandles();
    }

    @Override
    public String getWindowHandle()
    {
        return driver.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo()
    {
        return driver.switchTo();
    }

    @Override
    public Navigation navigate()
    {
        return driver.navigate();
    }

    @Override
    public Options manage()
    {
        return driver.manage();
    }

    @Override
    public WebDriver getWrappedDriver()
    {
        return driver;
    }

    @Override
    public Keyboard getKeyboard()
    {
        return ((HasInputDevices)driver).getKeyboard();
    }

    @Override
    public Mouse getMouse()
    {
        return ((HasInputDevices)driver).getMouse();
    }

    @Override
    public String toString()
    {
        return driver.toString();
    }
}
