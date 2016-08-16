package com.focusit.jsflight.player.webdriver;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.openqa.selenium.internal.Locatable;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

/**
 * Created by dkolmogortsev on 15.06.16.
 * Wraps {@link WebElement} so each call handles {@link StaleElementReferenceException} recursevly
 * executing until element is stable or element is detached of DOM
 */
public class LocatedElement implements WebElement, WrapsElement, Locatable
{
    private static final Logger LOG = LoggerFactory.getLogger(LocatedElement.class);
    private WebElement delegate;
    private WebDriver driver;
    private By locator;

    LocatedElement(WebElement delegate, WebDriver driver, By locator)
    {
        this.delegate = delegate;
        this.driver = driver;
        this.locator = locator;
    }

    @Override
    public void click()
    {
        try
        {
            clickInternal();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            click();
        }
    }

    @Override
    public void submit()
    {
        try
        {
            delegate.submit();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            submit();
        }
    }

    @Override
    public void sendKeys(CharSequence... keysToSend)
    {
        try
        {
            delegate.sendKeys(keysToSend);
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            sendKeys(keysToSend);
        }
    }

    @Override
    public void clear()
    {
        try
        {
            delegate.clear();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            clear();
        }
    }

    @Override
    public String getTagName()
    {
        try
        {
            return delegate.getTagName();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return getTagName();
        }
    }

    @Override
    public String getAttribute(String name)
    {
        try
        {
            return delegate.getAttribute(name);
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return getAttribute(name);
        }
    }

    @Override
    public boolean isSelected()
    {
        try
        {
            return delegate.isSelected();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return isSelected();
        }
    }

    @Override
    public boolean isEnabled()
    {
        try
        {
            return delegate.isEnabled();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return isEnabled();
        }
    }

    @Override
    public String getText()
    {
        try
        {
            return delegate.getText();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return getText();
        }
    }

    @Override
    public List<WebElement> findElements(By by)
    {
        try
        {
            List<WebElement> result = new ArrayList<>();
            for (WebElement element : delegate.findElements(by))
            {
                result.add(new LocatedElement(element, driver, by));
            }
            return result;
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return findElements(by);
        }
    }

    @Override
    public WebElement findElement(By by)
    {
        try
        {
            return new LocatedElement(delegate.findElement(by), driver, by);
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return findElement(by);
        }
    }

    @Override
    public boolean isDisplayed()
    {
        try
        {
            return delegate.isDisplayed();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return isDisplayed();
        }
    }

    @Override
    public Point getLocation()
    {
        try
        {
            return delegate.getLocation();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return getLocation();
        }
    }

    @Override
    public Dimension getSize()
    {
        try
        {
            return delegate.getSize();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return getSize();
        }
    }

    @Override
    public Rectangle getRect()
    {
        try
        {
            return delegate.getRect();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return getRect();
        }
    }

    @Override
    public String getCssValue(String propertyName)
    {
        try
        {
            return delegate.getCssValue(propertyName);
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return getCssValue(propertyName);
        }
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException
    {
        try
        {
            return delegate.getScreenshotAs(target);
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return getScreenshotAs(target);
        }
    }

    private void reLocateElement()
    {
        LOG.warn("Stale element. Relocating");
        WebDriverWait wait = new WebDriverWait(driver, 20, 50l);
        try
        {
            delegate = wait.until(new Function<WebDriver, WebElement>()
            {
                @Override
                public WebElement apply(WebDriver driver)
                {
                    try
                    {
                        return driver.findElement(locator);
                    }
                    catch (NoSuchElementException e)
                    {
                        return null;
                    }
                }
            });
        }
        catch (TimeoutException e)
        {
            throw new RuntimeException("Element was not relocated within timeout by locator: " + locator.toString());
        }
    }

    @Override
    public WebElement getWrappedElement()
    {
        return delegate;
    }

    @Override
    public Coordinates getCoordinates()
    {
        try
        {
            return ((Locatable)delegate).getCoordinates();
        }
        catch (StaleElementReferenceException e)
        {
            reLocateElement();
            return getCoordinates();
        }
    }

    public boolean equals(WebElement obj)
    {
        return delegate.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }

    /**
     * Click implementation with javascript click if conventional click fails
     */
    private void clickInternal()
    {
        try
        {
            //Clicking with offset so driver won`t recalculate click point to a clickable point
            //Clicking as is.
            new Actions(driver).moveToElement(delegate).click().build().perform();
        }
        catch (WebDriverException e)
        {
            if (e instanceof StaleElementReferenceException)
            {
                throw e;
            }
            LOG.warn(e.toString(), e);
            ((JavascriptExecutor)driver).executeScript("arguments[0].click()", delegate);
        }
    }
}
