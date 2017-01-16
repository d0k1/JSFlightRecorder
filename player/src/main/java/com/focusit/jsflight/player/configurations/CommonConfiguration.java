package com.focusit.jsflight.player.configurations;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.focusit.jsflight.player.constants.BrowserType;
import com.focusit.jsflight.script.ScriptsClassLoader;

/**
 * Common configuration i.e. everything about player. browser settings, timeout settings
 * Created by dkirpichenkov on 06.05.16.
 */
public class CommonConfiguration
{
    @Transient
    @JsonIgnore
    private transient static final Logger LOG = LoggerFactory.getLogger(CommonConfiguration.class);
    @Transient
    @JsonIgnore
    private transient ReentrantLock scriptClassloaderLock;

    private Integer proxyPort;
    private String proxyHost;

    private String pathToBrowserExecutable;
    private Integer asyncRequestsCompletedTimeoutInSeconds;

    private String screenshotsDirectory;
    private String webDriverTag;
    private BrowserType browserType;

    private boolean makeShots;
    private boolean useRandomChars;
    private String formOrDialogXpath;
    private String maxElementGroovy;

    /**
     * Timeout in seconds for UI to appear
     */
    private int uiShownTimeoutSeconds;
    /**
     * Interval in seconds between awaiting UI attempts
     */
    private long intervalBetweenUiChecksMs;
    @Transient
    @JsonIgnore
    transient private ScriptsClassLoader scriptClassloader = null;
    private CharSequence targetBaseUrl;
    private Long maxRequestsPerScenario;

    public CommonConfiguration()
    {
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

    public long getIntervalBetweenUiChecksMs()
    {
        return intervalBetweenUiChecksMs;
    }

    public void setIntervalBetweenUiChecksMs(long intervalBetweenUiChecksMs)
    {
        this.intervalBetweenUiChecksMs = intervalBetweenUiChecksMs;
    }

    public int getUiShownTimeoutSeconds()
    {
        return uiShownTimeoutSeconds;
    }

    public void setUiShownTimeoutSeconds(int uiShownTimeoutSeconds)
    {
        this.uiShownTimeoutSeconds = uiShownTimeoutSeconds;
    }

    private List<URL> findClasspathForScripts(@Nullable String path)
    {
        try
        {
            return Files.walk(Paths.get(path)).filter(Files::isRegularFile).map(CommonConfiguration::toUrl)
                    .filter(Objects::nonNull).collect(Collectors.toList());
        }
        catch (Exception e)
        {
            LOG.error(e.toString(), e);
        }
        return new ArrayList<>();
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

    public Integer getAsyncRequestsCompletedTimeoutInSeconds()
    {
        return asyncRequestsCompletedTimeoutInSeconds;
    }

    public void setAsyncRequestsCompletedTimeoutInSeconds(Integer asyncRequestsCompletedTimeoutInSeconds)
    {
        this.asyncRequestsCompletedTimeoutInSeconds = asyncRequestsCompletedTimeoutInSeconds;
    }

    public String getPathToBrowserExecutable()
    {
        return pathToBrowserExecutable;
    }

    public void setPathToBrowserExecutable(String pathToBrowserExecutable)
    {
        this.pathToBrowserExecutable = pathToBrowserExecutable;
    }

    public String getProxyHost()
    {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost)
    {
        this.proxyHost = proxyHost;
    }

    public Integer getProxyPort()
    {
        return proxyPort;
    }

    public void setProxyPort(Integer proxyPort)
    {
        this.proxyPort = proxyPort;
    }

    public String getScreenshotsDirectory()
    {
        return screenshotsDirectory;
    }

    public void setScreenshotsDirectory(String screenshotsDirectory)
    {
        this.screenshotsDirectory = screenshotsDirectory;
    }

    public String getWebDriverTag()
    {
        return webDriverTag;
    }

    public void setWebDriverTag(String webDriverTag)
    {
        this.webDriverTag = webDriverTag;
    }

    public BrowserType getBrowserType()
    {
        return browserType;
    }

    public void setBrowserType(BrowserType browserType)
    {
        this.browserType = browserType;
    }

    public boolean isUseRandomChars()
    {
        return useRandomChars;
    }

    public void setUseRandomChars(boolean useRandomChars)
    {
        this.useRandomChars = useRandomChars;
    }

    public void loadDefaultMaxElementScript()
    {
        if (getMaxElementGroovy() == null)
        {
            setMaxElementGroovy(
                    "def list = webdriver.findElements(org.openqa.selenium.By.xpath(\"//div[@id='gwt-debug-PopupListSelect']//div[@__idx]\"));\n"
                            + "\n" + "def maxEl = null;\n" + "Integer val = null;\n" + "\n" + "def tempEl = null;\n"
                            + "def tempVal = 0;\n" + "\n" + "for(int i=0;i<list.size();i++)\n" + "{\n"
                            + "\ttempEl = list.get(i);\n"
                            + "\ttempVal = Integer.parseInt(tempEl.getAttribute(\"__idx\"));\n" + "\n"
                            + "\tif(maxEl==null)\n" + "\t{\n" + "\t\tmaxEl = tempEl;\n" + "\t\tval = tempVal;\n"
                            + "\t\tcontinue;\n" + "\t}\n" + "\n" + "\tif(val<tempVal){\n" + "\t\tmaxEl = tempEl;\n"
                            + "\t\tval = tempVal;\n" + "\t}\n" + "}\n" + "\n" + "return maxEl;\n");
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
                ArrayList<URL> urls = new ArrayList<>();
                urls.addAll(findClasspathForScripts(System.getProperty("cp")));
                scriptClassloader = new ScriptsClassLoader(this.getClass().getClassLoader(),
                        urls.toArray(new URL[urls.size()]));
            }
            return scriptClassloader;
        }
        finally
        {
            scriptClassloaderLock.unlock();
        }
    }

    public CharSequence getTargetBaseUrl()
    {
        return targetBaseUrl;
    }

    public void setTargetBaseUrl(CharSequence targetBaseUrl)
    {
        this.targetBaseUrl = targetBaseUrl;
    }

    public Long getMaxRequestsPerScenario()
    {
        return maxRequestsPerScenario;
    }

    public void setMaxRequestsPerScenario(Long maxRequestsPerScenario)
    {
        this.maxRequestsPerScenario = maxRequestsPerScenario;
    }
}
