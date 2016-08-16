package com.focusit.jsflight.player.config;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.focusit.script.ScriptsClassLoader;
import com.focusit.script.constants.ScriptBindingConstants;

/**
 * Common configuration i.e. everything about player. browser settings, timeout settings
 * Created by dkirpichenkov on 06.05.16.
 */
public class CommonConfiguration
{
    private static final Logger LOG = LoggerFactory.getLogger(CommonConfiguration.class);
    private static final String CHECK_PAGE_JS_DEFAULT = "return (document.getElementById('state.dispatch')==null || document.getElementById('state.dispatch').getAttribute('value')==0) &&  (document.getElementById('state.context')==null ||  document.getElementById('state.context').getAttribute('value')=='ready');";
    @Transient
    @JsonIgnore
    private ReentrantLock scriptClassloaderLock;
    private String proxyPort;
    private String proxyHost;
    private String ffPath;
    private String pjsPath;
    private String pageReadyTimeout = "30";
    private boolean makeShots;
    private String screenDir;
    private String webDriverTag;
    private boolean useFirefox;
    private boolean usePhantomJs;
    private boolean useRandomChars;
    private String firefoxDisplay;
    private String formOrDialogXpath;
    private String checkPageJs;
    private String maxElementGroovy;
    private String uiShownScript;
    private int scrollTimeout;
    private int pageShownTimeout;
    private Map<String, Object> customProperties = new ConcurrentHashMap<>();
    private String extraClasspath = null;
    private String processSignalScript = String.format("\"kill ${%s} ${%s}\".execute()", ScriptBindingConstants.SIGNAL,
            ScriptBindingConstants.PID);
    private String getFirefoxPidScript = String.format(
            "\"echo ${%s.binary.process.process.executeWatchdog.getPID()}\".execute().text",
            ScriptBindingConstants.WEB_DRIVER);
    /**
     * Timeout in seconds for UI to appear
     */
    private int uiShowTimeoutSeconds = 180;
    /**
     * Interval in seconds between awaiting UI attempts
     */
    private long intervalBetweenUiChecksMs = 2000;
    @Transient
    @JsonIgnore
    transient private ArrayList<URL> urls = new ArrayList<>();
    @Transient
    @JsonIgnore
    transient private ScriptsClassLoader scriptClassloader = null;

    public CommonConfiguration()
    {
        checkPageJs = CHECK_PAGE_JS_DEFAULT;
        scriptClassloaderLock = new ReentrantLock();
    }

    private static URL toUrl(Path file)
    {
        try
        {
            return file.toUri().toURL();
        }
        catch (MalformedURLException e)
        {
            LOG.error(e.toString(), e);
            return null;
        }
    }

    public String getProcessSignalScript()
    {
        return processSignalScript;
    }

    public void setProcessSignalScript(String processSignalScript)
    {
        this.processSignalScript = processSignalScript;
    }

    public long getIntervalBetweenUiChecksMs()
    {
        return intervalBetweenUiChecksMs;
    }

    public void setIntervalBetweenUiChecksMs(long intervalBetweenUiChecksMs)
    {
        this.intervalBetweenUiChecksMs = intervalBetweenUiChecksMs;
    }

    public int getUiShowTimeoutSeconds()
    {
        return uiShowTimeoutSeconds;
    }

    public void setUiShowTimeoutSeconds(int uiShowTimeoutSeconds)
    {
        this.uiShowTimeoutSeconds = uiShowTimeoutSeconds;
    }

    private void findClasspathForScripts(String path)
    {
        try
        {
            urls.addAll(Files.walk(Paths.get(path)).filter(Files::isRegularFile).map(CommonConfiguration::toUrl)
                    .filter(Objects::nonNull).collect(Collectors.toList()));
        }
        catch (Exception e)
        {
            LOG.error(e.toString(), e);
        }
    }

    public String getCheckPageJs()
    {
        return checkPageJs;
    }

    public void setCheckPageJs(String checkPageJs)
    {
        this.checkPageJs = checkPageJs;
    }

    public String getFfPath()
    {
        return ffPath;
    }

    public void setFfPath(String ffPath)
    {
        this.ffPath = ffPath;
    }

    public String getFirefoxDisplay()
    {
        return firefoxDisplay;
    }

    public void setFirefoxDisplay(String firefoxDisplay)
    {
        this.firefoxDisplay = firefoxDisplay;
    }

    public boolean getMakeShots()
    {
        return makeShots;
    }

    public void setMakeShots(boolean makeShots)
    {
        this.makeShots = makeShots;
    }

    public String getMaxElementGroovy()
    {
        return maxElementGroovy;
    }

    public void setMaxElementGroovy(String maxElementGroovy)
    {
        this.maxElementGroovy = maxElementGroovy;
    }

    public String getPageReadyTimeout()
    {
        return pageReadyTimeout;
    }

    public void setPageReadyTimeout(String pageReadyTimeout)
    {
        this.pageReadyTimeout = pageReadyTimeout;
    }

    public int getPageShownTimeout()
    {
        return pageShownTimeout;
    }

    public void setPageShownTimeout(int pageShownTimeout)
    {
        this.pageShownTimeout = pageShownTimeout;
    }

    public String getPjsPath()
    {
        return pjsPath;
    }

    public void setPjsPath(String pjsPath)
    {
        this.pjsPath = pjsPath;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost)
    {
        this.proxyHost = proxyHost;
    }

    public String getProxyPort()
    {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort)
    {
        this.proxyPort = proxyPort;
    }

    public String getScreenDir()
    {
        return screenDir;
    }

    public void setScreenDir(String screenDir)
    {
        this.screenDir = screenDir;
    }

    public int getScrollTimeout()
    {
        return scrollTimeout;
    }

    public void setScrollTimeout(int scrollTimeout)
    {
        this.scrollTimeout = scrollTimeout;
    }

    public String getUiShownScript()
    {
        return uiShownScript;
    }

    public void setUiShownScript(String uiShownScript)
    {
        this.uiShownScript = uiShownScript;
    }

    public String getWebDriverTag()
    {
        return webDriverTag;
    }

    public void setWebDriverTag(String webDriverTag)
    {
        this.webDriverTag = webDriverTag;
    }

    public boolean isUseFirefox()
    {
        return useFirefox;
    }

    public void setUseFirefox(boolean useFirefox)
    {
        this.useFirefox = useFirefox;
    }

    public boolean isUsePhantomJs()
    {
        return usePhantomJs;
    }

    public void setUsePhantomJs(boolean usePhantomJs)
    {
        this.usePhantomJs = usePhantomJs;
    }

    public boolean isUseRandomChars()
    {
        return useRandomChars;
    }

    public void setUseRandomChars(boolean useRandomChars)
    {
        this.useRandomChars = useRandomChars;
    }

    public void loadDefaultValues()
    {
        if (getCheckPageJs() == null)
        {
            setCheckPageJs(CHECK_PAGE_JS_DEFAULT);
        }
        if (getMaxElementGroovy() == null)
        {
            setMaxElementGroovy("def list = webdriver.findElements(org.openqa.selenium.By.xpath(\"//div[@id='gwt-debug-PopupListSelect']//div[@__idx]\"));\n"
                    + "\n"
                    + "def maxEl = null;\n"
                    + "Integer val = null;\n"
                    + "\n"
                    + "def tempEl = null;\n"
                    + "def tempVal = 0;\n"
                    + "\n"
                    + "for(int i=0;i<list.size();i++)\n"
                    + "{\n"
                    + "\ttempEl = list.get(i);\n"
                    + "\ttempVal = Integer.parseInt(tempEl.getAttribute(\"__idx\"));\n"
                    + "\n"
                    + "\tif(maxEl==null)\n"
                    + "\t{\n"
                    + "\t\tmaxEl = tempEl;\n"
                    + "\t\tval = tempVal;\n"
                    + "\t\tcontinue;\n"
                    + "\t}\n"
                    + "\n"
                    + "\tif(val<tempVal){\n"
                    + "\t\tmaxEl = tempEl;\n"
                    + "\t\tval = tempVal;\n" + "\t}\n" + "}\n" + "\n" + "return maxEl;\n");
        }

        if (getUiShownScript() == null)
        {
            setUiShownScript("return webdriver.findElement(org.openqa.selenium.By.xpath(\"//*[@id='gwt-debug-editProfile']\"));");
        }

        if (getFormOrDialogXpath() == null)
        {
            setFormOrDialogXpath("//div[@id='gwt-debug-Form' or @id='gwt-debug-PropertyDialogBox']");
        }
    }

    public String getFormOrDialogXpath()
    {
        return formOrDialogXpath;
    }

    public void setFormOrDialogXpath(String formOrDialogXpath)
    {
        this.formOrDialogXpath = formOrDialogXpath;
    }

    public ScriptsClassLoader getScriptClassloader()
    {
        scriptClassloaderLock.lock();
        try
        {
            if (scriptClassloader == null)
            {
                urls.clear();
                findClasspathForScripts(System.getProperty("cp"));
                findClasspathForScripts(getExtraClasspath());
                scriptClassloader = new ScriptsClassLoader(this.getClass().getClassLoader(), urls.toArray(new URL[urls
                        .size()]));
            }
            return scriptClassloader;
        }
        finally
        {
            scriptClassloaderLock.unlock();
        }
    }

    public void resetScriptClassloader()
    {
        scriptClassloaderLock.lock();
        try
        {
            scriptClassloader = null;
        }
        finally
        {
            scriptClassloaderLock.unlock();
        }
    }

    public String getExtraClasspath()
    {
        return extraClasspath;
    }

    public void setExtraClasspath(String extraClasspath)
    {
        this.extraClasspath = extraClasspath;
    }

    public void addCustomProperty(String property, Object value)
    {
        customProperties.put(property, value);
    }

    public Object getCustomProperty(String property)
    {
        return customProperties.get(property);
    }

    public void deleteCustomProperty(String property)
    {
        customProperties.remove(property);
    }

    public String getGetFirefoxPidScript()
    {
        return getFirefoxPidScript;
    }
}
