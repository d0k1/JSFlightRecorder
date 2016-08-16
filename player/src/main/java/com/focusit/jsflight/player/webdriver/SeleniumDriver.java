package com.focusit.jsflight.player.webdriver;

import com.focusit.jsflight.player.constants.EventConstants;
import com.focusit.jsflight.player.constants.EventType;
import com.focusit.jsflight.player.scenario.UserScenario;
import com.focusit.jsflight.player.script.PlayerScriptProcessor;
import com.focusit.script.constants.ScriptBindingConstants;
import com.google.common.base.Predicate;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Selenium webdriver proxy: runs a browser, sends events, make screenshots
 *
 * @author Denis V. Kirpichenkov
 */
public class SeleniumDriver
{

    /**
     * Non operational element indicating step processing must be aborted
     * returned from weblookup script
     */
    public static final RemoteWebElement NO_OP_ELEMENT = new RemoteWebElement();
    private static final Logger LOG = LoggerFactory.getLogger(SeleniumDriver.class);

    private static final int DISPLAY_CAPACITY = 200;

    private static final String UI_NOT_SHOWED_UP_MSG = "UI didn't show up";

    private static final int PROCESS_SIGNAL_STOP = -19;
    private static final int PROCESS_SIGNAL_CONT = -18;
    private static final int PROCESS_SIGNAL_FORCE_KILL = -9;

    static
    {
        //This must be set due to equals of WebElement
        NO_OP_ELEMENT.setId("NO_OP");
    }

    /**
     * As we close unsued browsers, 200 number of displays is more than enough
     */
    private List<String> availiableDisplays = new ArrayList<>(DISPLAY_CAPACITY);
    private HashMap<String, String> driverDisplay = new HashMap<>();
    private HashMap<String, WebDriver> drivers = new HashMap<>();

    private Map<String, String> lastUrls = new HashMap<>();
    private StringGenerator stringGen;
    private UserScenario scenario;
    private int pageTimeoutMs;
    private String checkPageJs;
    private String lookupScript;
    private String maxElementGroovy;
    private String uiShownScript;
    private boolean useRandomChars;
    private List<String> emptySelections;
    private String selectXpath;
    private String selectDeterminerScript;
    private String processSignalScript;
    private String getFirefoxPidScript;
    /**
     * Interval in seconds between awaiting UI attempts
     */
    private long intervalBetweenUiChecksMs;
    /**
     * Timeout in seconds for UI to appear
     */
    private int uiShowTimeoutSeconds;
    private String formDialogXpath;

    public SeleniumDriver(UserScenario scenario)
    {
        this.scenario = scenario;

        //init queue
        //staring from 1, rather than 0, cuz on :0 firefox open on real display
        for (int i = 1; i < DISPLAY_CAPACITY; i++)
        {
            availiableDisplays.add(":" + i);
        }
    }

    public static RemoteWebElement getNoOpElement()
    {
        return NO_OP_ELEMENT;
    }

    private static boolean isNullOrWhiteSpace(String path)
    {
        return path == null || isWhiteSpace(path);
    }

    private static boolean isWhiteSpace(String str)
    {

        int length = str.length();
        if (length == 0)
        {
            return true;
        }
        int middle = length / 2;
        if (!Character.isWhitespace(str.charAt(middle)))
        {
            return false;
        }
        for (int i = 0; i < middle; i++)
        {
            if (!Character.isWhitespace(str.charAt(i)) || !Character.isWhitespace(str.charAt(length - 1 - i)))
                return false;
        }
        return true;
    }

    public SeleniumDriver setFormDialogXpath(String formDialogXpath)
    {
        this.formDialogXpath = formDialogXpath;
        return this;
    }

    public SeleniumDriver setProcessSignalScript(String processSignalScript)
    {
        this.processSignalScript = processSignalScript;
        return this;
    }

    public SeleniumDriver setSelectDeterminerScript(String selectDeterminerScript)
    {
        this.selectDeterminerScript = selectDeterminerScript;
        return this;
    }

    public SeleniumDriver setSelectXpath(String selectXpath)
    {
        this.selectXpath = selectXpath;
        return this;
    }

    public SeleniumDriver setEmptySelections(String emptySelections)
    {
        this.emptySelections = Arrays.asList(emptySelections.split(","));
        return this;
    }

    public int getUiShowTimeoutSeconds()
    {
        return uiShowTimeoutSeconds;
    }

    public SeleniumDriver setUiShowTimeoutSeconds(int uiShowTimeoutSeconds)
    {
        this.uiShowTimeoutSeconds = uiShowTimeoutSeconds;
        return this;
    }

    public SeleniumDriver setLastUrls(Map<String, String> externalUrls)
    {
        this.lastUrls = externalUrls;
        return this;
    }

    public SeleniumDriver setUseRandomChars(boolean useRandomChars)
    {
        this.useRandomChars = useRandomChars;
        return this;
    }

    public SeleniumDriver setPageTimeoutMs(int pageTimeoutMs)
    {
        this.pageTimeoutMs = pageTimeoutMs;
        return this;
    }

    public SeleniumDriver setGetFirefoxPidScript(String getFirefoxPidScript)
    {
        this.getFirefoxPidScript = getFirefoxPidScript;
        return this;
    }

    public SeleniumDriver setCheckPageJs(String checkPageJs)
    {
        this.checkPageJs = checkPageJs;
        return this;
    }

    public SeleniumDriver setLookupScript(String lookupScript)
    {
        this.lookupScript = lookupScript;
        return this;
    }

    public SeleniumDriver setUiShownScript(String uiShownScript)
    {
        this.uiShownScript = uiShownScript;
        return this;
    }

    public SeleniumDriver setMaxElementGroovy(String maxElementGroovy)
    {
        this.maxElementGroovy = maxElementGroovy;
        return this;
    }

    public void closeWebDrivers()
    {
        PlayerScriptProcessor processor = new PlayerScriptProcessor(scenario);
        drivers.values().forEach(driver -> {
            processor.executeProcessSignalScript(processSignalScript, PROCESS_SIGNAL_CONT, getFirefoxPid(driver));
            driver.close();
        });
    }

    public WebElement findTargetWebElement(WebDriver wd, JSONObject event, String target)
    {
        waitPageReadyWithRefresh(wd, event);

        Map<String, Object> binding = PlayerScriptProcessor.getEmptyBindingsMap();
        binding.put(ScriptBindingConstants.WEB_DRIVER, wd);
        binding.put(ScriptBindingConstants.TARGET, target);
        binding.put(ScriptBindingConstants.EVENT, event);
        return new PlayerScriptProcessor(scenario).executeGroovyScript(lookupScript, binding, WebElement.class);
    }

    public WebDriver getDriverForEvent(JSONObject event, boolean firefox, String path, String display,
            String proxyHost, String proxyPort)
    {
        String tabUuid = event.getString(EventConstants.TAB_UUID);
        WebDriver driver = drivers.get(tabUuid);

        try
        {
            if (driver != null)
            {
                return driver;
            }

            DesiredCapabilities cap = new DesiredCapabilities();
            if (!isNullOrWhiteSpace(proxyHost))
            {
                String host = proxyHost;
                if (!isNullOrWhiteSpace(proxyPort))
                {
                    host += ":" + proxyPort;
                }
                Proxy proxy = new Proxy();
                proxy.setHttpProxy(host).setFtpProxy(host).setSslProxy(host);
                cap.setCapability(CapabilityType.PROXY, proxy);
            }
            if (firefox)
            {
                FirefoxProfile profile = createProfile();
                FirefoxBinary binary;
                if (!isNullOrWhiteSpace(path))
                {
                    binary = new FirefoxBinary(new File(path));
                }
                else
                {
                    binary = new FirefoxBinary();
                }
                if (!isNullOrWhiteSpace(display))
                {
                    display = availiableDisplays.remove(0);
                    LOG.info("Binding to {} display", display);
                    binary.setEnvironmentProperty("DISPLAY", display);
                }
                LOG.info("Firefox path is: {}", path);

                driver = createFirefoxDriver(cap, profile, binary);
            }
            else
            {
                if (!isNullOrWhiteSpace(path))
                {
                    cap.setCapability("phantomjs.binary.path", path);

                    driver = new PhantomJSDriver(cap);
                }
                else
                {
                    driver = new PhantomJSDriver(cap);
                }
            }
            driver = WebDriverWrapper.wrap(driver);

            drivers.put(tabUuid, driver);

            //as actual webdriver is RemoteWebdriver, calling to string return browser name, platform and sessionid
            //which are not subject to change, so we can use it as key;
            driverDisplay.put(driver.toString(), display);
            return driver;
        }
        catch (Throwable ex)
        {
            LOG.error(ex.toString(), ex);
            throw ex;
        }
        finally
        {
            prioritize(driver);
        }
    }

    private FirefoxDriver createFirefoxDriver(DesiredCapabilities cap, FirefoxProfile profile, FirefoxBinary binary)
    {
        try
        {
            return new FirefoxDriver(binary, profile, cap);
        }
        catch (WebDriverException ex)
        {
            LOG.warn(ex.getMessage());
            awakenAllDrivers();
            return createFirefoxDriver(cap, profile, binary);
        }
    }

    public String getDriverDisplay(WebDriver webdriver)
    {
        return driverDisplay.getOrDefault(webdriver.toString(), "No display");
    }

    public String getLastUrl(JSONObject event)
    {
        String no_result = "";
        String result = lastUrls.get(scenario.getTagForEvent(event));
        if (result == null)
        {
            lastUrls.put(scenario.getTagForEvent(event), no_result);
            result = no_result;
        }

        return result;
    }

    public void makeAShot(WebDriver wd, OutputStream outputStream) throws IOException
    {
        TakesScreenshot shooter = (TakesScreenshot)wd;
        byte[] shot = shooter.getScreenshotAs(OutputType.BYTES);
        outputStream.write(shot);
    }

    public void openEventUrl(WebDriver wd, JSONObject event)
    {
        String eventUrl = event.getString(EventConstants.URL);

        resizeForEvent(wd, event);
        if (wd.getCurrentUrl().equals("about:blank") || !getLastUrl(event).equals(eventUrl))
        {
            wd.get(eventUrl);

            waitUi(wd);
            updateLastUrl(event, eventUrl);
        }
    }

    public WebElement waitElement(WebDriver wd, String xpath)
    {
        try
        {
            return new WebDriverWait(wd, 20L, 500).until(new ExpectedCondition<WebElement>()
            {
                @Override
                public WebElement apply(WebDriver input)
                {
                    try
                    {
                        return wd.findElement(By.xpath(xpath));
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
            throw new NoSuchElementException("Element was not found within timeout. Xpath " + xpath);
        }
    }

    public void processKeyboardEvent(WebDriver wd, JSONObject event) throws UnsupportedEncodingException
    {
        WebElement element = findTargetWebElement(wd, event, scenario.getTargetForEvent(event));
        if (element.equals(NO_OP_ELEMENT))
        {
            LOG.warn("Non operational element returned. Aborting event {} processing. Target xpath {}",
                    event.get(EventConstants.EVENT_ID), event.getString(EventConstants.SECOND_TARGET));
            return;
        }
        initializeStringGenerator(useRandomChars);

        //TODO remove this when recording of cursor in text box is implemented
        if (skipKeyboardForElement(element))
        {
            LOG.warn("Keyboard processing for non empty Date is disabled");
            return;
        }

        //Before processing keyboard events focus MUST be on the browser window, otherwise there is no
        //guaranties for correct processing.
        //Firefox ignores all fired events when window is not in focus
        //Selenium uses dark magic to deal with it
        wd.switchTo().window(wd.getWindowHandle());

        if (event.getString(EventConstants.TYPE).equalsIgnoreCase(EventType.KEY_PRESS))
        {
            if (event.has(EventConstants.CHAR_CODE))
            {
                char ch = (char)event.getBigInteger(EventConstants.CHAR_CODE).intValue();
                String keys = stringGen.getAsString(ch);
                if (!element.getTagName().contains("iframe"))
                {
                    String prevText = element.getAttribute("value");
                    //If current value indicates a empty selection it must be discarded
                    if (emptySelections.contains(prevText))
                    {
                        prevText = "";
                    }
                    element.clear();
                    element.sendKeys(prevText + keys);

                }
                else
                {
                    WebDriver frame = wd.switchTo().frame(element);
                    WebElement editor = frame.findElement(By.tagName("body"));
                    editor.sendKeys(keys);
                    wd.switchTo().defaultContent();
                }
            }
        }

        if (event.getString(EventConstants.TYPE).equalsIgnoreCase(EventType.KEY_UP)
                || event.getString(EventConstants.TYPE).equalsIgnoreCase(EventType.KEY_DOWN))
        {
            if (event.has(EventConstants.CHAR_CODE))
            {
                int code = event.getBigInteger(EventConstants.CHAR_CODE).intValue();
                if (code == 0)
                {
                    code = event.getInt(EventConstants.KEY_CODE);
                }
                if (event.getBoolean(EventConstants.CTRL_KEY))
                {
                    element.sendKeys(Keys.chord(Keys.CONTROL, new String(new byte[] { (byte)code },
                            StandardCharsets.UTF_8)));
                }
                else
                {
                    switch (code)
                    {
                    case 8:
                        element.sendKeys(Keys.BACK_SPACE);
                        break;
                    case 27:
                        element.sendKeys(Keys.ESCAPE);
                        break;
                    case 46:
                        element.sendKeys(Keys.DELETE);
                        break;
                    case 13:
                        element.sendKeys(Keys.ENTER);
                        break;
                    case 37:
                        element.sendKeys(Keys.ARROW_LEFT);
                        break;
                    case 38:
                        element.sendKeys(Keys.ARROW_UP);
                        break;
                    case 39:
                        element.sendKeys(Keys.ARROW_RIGHT);
                        break;
                    case 40:
                        element.sendKeys(Keys.ARROW_DOWN);
                        break;
                    default:
                        break;
                    }
                }
            }
        }
    }

    public void processMouseEvent(WebDriver wd, JSONObject event)
    {
        WebElement element = findTargetWebElement(wd, event, scenario.getTargetForEvent(event));
        if (element.equals(NO_OP_ELEMENT))
        {
            LOG.warn("Non operational element returned. Aborting event {} processing. Target xpath {}",
                    event.get(EventConstants.EVENT_ID), event.getString(EventConstants.SECOND_TARGET));
            return;
        }
        ensureElementInWindow(wd, element);
        boolean isSelect = new PlayerScriptProcessor(scenario).executeSelectDeterminerScript(selectDeterminerScript,
                wd, element);
        click(wd, event, element);
        if (isSelect)
        {
            //Wait for select to popup
            waitElement(wd, selectXpath);
        }

    }

    private void click(WebDriver wd, JSONObject event, WebElement element)
    {
        if (element.isDisplayed())
        {

            if (event.getInt(EventConstants.BUTTON) == 2)
            {
                try
                {
                    new Actions(wd).contextClick(element).perform();
                }
                catch (WebDriverException ex)
                {
                    try
                    {
                        LOG.warn("Error simulation right click. Retrying after 2 sec.");
                        Thread.sleep(2000);

                        new Actions(wd).contextClick(element).perform();
                    }
                    catch (Exception e)
                    {
                        LOG.error(e.toString(), e);
                    }
                }
            }
            else
            {
                element.click();
            }
        }
        else
        {
            JavascriptExecutor executor = (JavascriptExecutor)wd;
            executor.executeScript("arguments[0].click();", element);
        }
    }

    public void processMouseWheel(WebDriver wd, JSONObject event, String target)
    {
        if (!event.has(EventConstants.DELTA_Y))
        {
            LOG.error("event has no deltaY - cant process scroll", new Exception());
            return;
        }
        WebElement el = findTargetWebElement(wd, event, target);
        if (el == null)
        {
            throw new RuntimeException("Weblookup script returned null");
        }
        if (NO_OP_ELEMENT.equals(el))
        {
            LOG.warn("Non operational element returned. Aborting event {} processing. Target xpath {}",
                    event.get(EventConstants.EVENT_ID), event.getString(EventConstants.SECOND_TARGET));
            return;
        }
        //Web lookup script MUST return //body element if scroll occurs not in a popup
        if (!el.getTagName().equalsIgnoreCase("html"))
        {
            ((JavascriptExecutor)wd).executeScript("arguments[0].scrollTop = arguments[0].scrollTop + arguments[1]",
                    el, event.getInt(EventConstants.DELTA_Y));
        }
        else
        {
            ((JavascriptExecutor)wd).executeScript("window.scrollBy(0, arguments[0])",
                    event.getInt(EventConstants.DELTA_Y));
        }
    }

    public void processScroll(WebDriver wd, JSONObject event, String target)
    {
        long timeout = System.currentTimeMillis() + 20000L;
        if (checkElementPresent(wd, target))
        {
            return;
        }
        do
        {
            waitPageReadyWithRefresh(wd, event);
            // TODO WebLookup script must return the element
            try
            {
                WebElement el = getMax(wd, maxElementGroovy);
                scroll((JavascriptExecutor)wd, el);
                if (checkElementPresent(wd, target))
                {
                    return;
                }
            }
            catch (Exception ex)
            {
                LOG.error(ex.toString(), ex);
            }
        }
        while (System.currentTimeMillis() < timeout);
        throw new NoSuchElementException("Element was not found during scroll");
    }

    public void releaseBrowser(WebDriver driver, JSONObject event)
    {
        if (isOnForm(driver))
        {
            LOG.debug("Browser is on form, or form or dialog xpath is not specified");
            return;
        }

        String display = driverDisplay.get(driver.toString());
        LOG.info("Display {} is available again", display);
        availiableDisplays.add(display);

        String tabUuid = event.getString(EventConstants.TAB_UUID);
        drivers.remove(tabUuid);
        driver.quit();
        try
        {
            PlayerScriptProcessor processor = new PlayerScriptProcessor(scenario);
            String firefoxPid = getFirefoxPid(driver);
            LOG.info("Trying to kill Firefox. PID: {}", firefoxPid);
            processor.executeProcessSignalScript(processSignalScript, PROCESS_SIGNAL_FORCE_KILL, firefoxPid);
        }
        catch (Throwable ex)
        {
            LOG.error(ex.getMessage(), ex);
        }
    }

    private String getFirefoxPid(WebDriver driver)
    {
        PlayerScriptProcessor processor = new PlayerScriptProcessor(scenario);
        Map<String, Object> binding = PlayerScriptProcessor.getEmptyBindingsMap();
        binding.put(ScriptBindingConstants.WEB_DRIVER, getSeleniumDriver(driver));
        return processor.executeGroovyScript(getFirefoxPidScript, binding, String.class);
    }

    private WebDriver getSeleniumDriver(WebDriver driver)
    {
        return driver instanceof WebDriverWrapper ? ((WebDriverWrapper)driver).getWrappedDriver() : driver;
    }

    public void resetLastUrls()
    {
        lastUrls.clear();
    }

    public void setScenario(UserScenario scenario)
    {
        this.scenario = scenario;
    }

    public void updateLastUrl(JSONObject event, String url)
    {
        lastUrls.put(scenario.getTagForEvent(event), url);
    }

    public void waitPageReadyWithRefresh(WebDriver wd, JSONObject event)
    {
        try
        {
            waitPageReady(wd, event);
        }
        catch (IllegalStateException e)
        {
            if (isOnForm(wd))
            {
                throw e;
            }
            else
            {
                wd.navigate().refresh();
                waitPageReady(wd, event);
            }
        }
    }

    private boolean isOnForm(WebDriver wd)
    {
        return formDialogXpath != null && !formDialogXpath.isEmpty()
                && !wd.findElements(By.xpath(formDialogXpath)).isEmpty();
    }

    public void waitPageReady(WebDriver wd, JSONObject event)
    {
        String type = event.getString(EventConstants.TYPE);
        if (type.equalsIgnoreCase(EventType.XHR) || type.equalsIgnoreCase(EventType.SCRIPT))
        {
            return;
        }
        LOG.info("Waiting page readiness for {} seconds", pageTimeoutMs);
        try
        {
            new WebDriverWait(wd, pageTimeoutMs, 500).until(new Predicate<WebDriver>()
            {
                @Override
                public boolean apply(WebDriver input)
                {
                    try
                    {
                        Object result = ((JavascriptExecutor)wd).executeScript(checkPageJs);
                        LOG.info("Page readiness checking: {}", result);
                        return result != null && Boolean.parseBoolean(result.toString().toLowerCase());
                    }
                    catch (WebDriverException e)
                    {
                        return false;
                    }
                }
            });
        }
        catch (TimeoutException e)
        {
            throw new IllegalStateException(String.format("Page was not ready within specified timeout: %s",
                    event.getString(EventConstants.URL)));
        }
    }

    private boolean checkElementPresent(WebDriver wd, String target)
    {
        try
        {
            wd.findElement(By.xpath(target));
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private void ensureElementInWindow(WebDriver wd, WebElement element)
    {
        int windowHeight = wd.manage().window().getSize().getHeight();
        int elementYCoord = element.getLocation().getY();
        if (elementYCoord > windowHeight)
        {
            //Using division of the Y coordinate by 2 ensures target element visibility in the browser view
            //anyway TODO think of not using hardcoded constants in scrolling
            String scrollScript = "window.scrollTo(0, " + elementYCoord / 2 + ");";
            ((JavascriptExecutor)wd).executeScript(scrollScript);
        }
    }

    private void initializeStringGenerator(boolean useRandomChars)
    {
        if (stringGen == null)
        {
            if (useRandomChars)
            {
                stringGen = new RandomStringGenerator();
            }
            else
            {
                stringGen = new CharStringGenerator();
            }
        }
    }

    private boolean skipKeyboardForElement(WebElement element)
    {
        //TODO remove this when recording of cursor in text box is implemented
        return !element.getAttribute("value").isEmpty()
                && (element.getAttribute("class").contains("date") || element.getAttribute("id").contains("date"));
    }

    private WebElement getMax(WebDriver wd, String script)
    {
        Map<String, Object> binding = PlayerScriptProcessor.getEmptyBindingsMap();
        binding.put(ScriptBindingConstants.WEB_DRIVER, wd);
        return new PlayerScriptProcessor(scenario).executeGroovyScript(script, binding, WebElement.class);
    }

    private void resizeForEvent(WebDriver wd, JSONObject event)
    {
        boolean hasProperty = event.has("window.width");
        JSONObject target = hasProperty ? event : event.getJSONObject("window");
        int width = target.getInt(hasProperty ? "window.width" : "width");
        int height = target.getInt(hasProperty ? "window.height" : "height");

        width = width > 0 ? width : 1000;
        height = height > 0 ? height : 1000;

        LOG.info("Resizing to {}x{}", width, height);
        wd.manage().window().setSize(new Dimension(width, height));
    }

    private void scroll(JavascriptExecutor js, WebElement element)
    {
        js.executeScript("arguments[0].scrollIntoView(true)", element);
    }

    private void waitUi(WebDriver wd)
    {
        try
        {
            new WebDriverWait(wd, uiShowTimeoutSeconds, intervalBetweenUiChecksMs).until(new Predicate<WebDriver>()
            {
                @Override
                public boolean apply(WebDriver driver)
                {
                    try
                    {
                        Map<String, Object> binding = PlayerScriptProcessor.getEmptyBindingsMap();
                        binding.put(ScriptBindingConstants.WEB_DRIVER, driver);
                        return new PlayerScriptProcessor(scenario).executeGroovyScript(uiShownScript, binding) != null;
                    }
                    catch (WebDriverException e)
                    {
                        return false;
                    }
                }
            });
        }
        catch (TimeoutException e)
        {
            throw new NoSuchElementException(UI_NOT_SHOWED_UP_MSG);
        }
    }

    private void awakenAllDrivers()
    {
        PlayerScriptProcessor processor = new PlayerScriptProcessor(scenario);
        drivers.values().forEach(
                driver -> processor.executeProcessSignalScript(processSignalScript, PROCESS_SIGNAL_CONT,
                        getFirefoxPid(driver)));
    }

    private void prioritize(WebDriver wd)
    {
        if (drivers.size() == 1)
        {
            return;
        }
        PlayerScriptProcessor processor = new PlayerScriptProcessor(scenario);
        String firefoxPid = getFirefoxPid(wd);
        processor.executeProcessSignalScript(processSignalScript, PROCESS_SIGNAL_CONT, firefoxPid);
        LOG.info("Prioritizing driver with pid: {}", firefoxPid);
        drivers.values()
                .stream()
                .filter(driver -> !driver.equals(wd))
                .forEach(
                        driver -> processor.executeProcessSignalScript(processSignalScript, PROCESS_SIGNAL_STOP,
                                getFirefoxPid(driver)));
    }

    private FirefoxProfile createProfile()
    {
        FirefoxProfile firefoxProfile = new FirefoxProfile();
        firefoxProfile.setPreference("nglayout.initialpaint.delay", "0");
        firefoxProfile.setPreference("network.http.pipelining", true);
        firefoxProfile.setPreference("image.animation_mode", "none");
        firefoxProfile.setPreference("layers.acceleration.force-enabled", true);
        firefoxProfile.setPreference("layers.offmainthreadcomposition.enabled", true);
        firefoxProfile.setPreference("browser.sessionstore.interval", 3600000);
        firefoxProfile.setPreference("privacy.trackingprotection.enabled", true);
        firefoxProfile.setPreference("content.notify.interval", 849999);
        firefoxProfile.setPreference("content.notify.backoffcount", 5);
        firefoxProfile.setPreference("network.http.max-connections", 50);
        firefoxProfile.setPreference("network.http.max-connections-per-server", 150);
        firefoxProfile.setPreference("network.http.pipelining.aggressive", false);
        firefoxProfile.setPreference("browser.tabs.animate", false);
        firefoxProfile.setPreference("browser.display.show_image_placeholders", false);
        firefoxProfile.setPreference("browser.cache.use_new_backend", 1);
        firefoxProfile.setPreference("ui.submenuDelay", 0);
        firefoxProfile.setPreference("browser.cache.disk.enable", false);
        firefoxProfile.setPreference("browser.cache.memory.enable", true);
        firefoxProfile.setPreference("browser.cache.memory.capacity", 128000);
        return firefoxProfile;
    }

    public SeleniumDriver setIntervalBetweenUiChecksMs(long intervalBetweenUiChecksMs)
    {
        this.intervalBetweenUiChecksMs = intervalBetweenUiChecksMs;
        return this;
    }

    private interface StringGenerator
    {
        String getAsString(char ch) throws UnsupportedEncodingException;
    }

    private static class CharStringGenerator implements StringGenerator
    {

        @Override
        public String getAsString(char ch) throws UnsupportedEncodingException
        {
            return String.valueOf(ch);
        }

    }

    private static class RandomStringGenerator implements StringGenerator
    {

        @Override
        public String getAsString(char ch)
        {
            return RandomStringUtils.randomAlphanumeric(1);
        }

    }
}
