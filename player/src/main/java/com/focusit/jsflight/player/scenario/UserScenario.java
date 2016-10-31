package com.focusit.jsflight.player.scenario;

import com.focusit.jsflight.player.cli.config.IConfig;
import com.focusit.jsflight.player.configurations.CommonConfiguration;
import com.focusit.jsflight.player.configurations.Configuration;
import com.focusit.jsflight.player.configurations.ScriptsConfiguration;
import com.focusit.jsflight.player.constants.EventConstants;
import com.focusit.jsflight.player.constants.EventType;
import com.focusit.jsflight.player.input.EventsParser;
import com.focusit.jsflight.player.input.FileInput;
import com.focusit.jsflight.player.script.PlayerScriptProcessor;
import com.focusit.jsflight.script.player.PlayerContext;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Recorded scenario encapsulation: parses file, plays the scenario by step, modifies the scenario, saves to a disk.
 *
 * @author Denis V. Kirpichenkov
 */
public class UserScenario
{
    private static final Logger LOG = LoggerFactory.getLogger(UserScenario.class.getSimpleName());

    private static final Set<String> ALLOWED_EVENT_TYPES = new HashSet<>(Arrays.asList(EventType.CLICK,
            EventType.KEY_PRESS, EventType.KEY_UP, EventType.KEY_DOWN, EventType.SCROLL_EMULATION,
            EventType.MOUSE_WHEEL, EventType.MOUSE_DOWN, EventType.SCRIPT));
    private static HashMap<String, JSONObject> lastEvents = new HashMap<>();
    private volatile int position = 0;
    private List<JSONObject> events = new ArrayList<>();
    private String preProcessScenarioScript;
    private PlayerContext context = new PlayerContext();
    private Configuration configuration = new Configuration();

    public void initFromConfig(IConfig config)
    {
        CommonConfiguration commonConfiguration = getConfiguration().getCommonConfiguration();
        commonConfiguration.setPathToBrowserExecutable(config.getPathToBrowserExecutable());
        commonConfiguration.setMakeShots(config.shouldMakeScreenshots());
        commonConfiguration.setAsyncRequestsCompletedTimeoutInSeconds(config
                .getAsyncRequestsCompletedTimeoutInSeconds());
        commonConfiguration.setProxyHost(config.getProxyHost());
        commonConfiguration.setScreenshotsDirectory(config.getScreenshotsDirectory());
        commonConfiguration.setBrowserType(config.getBrowserType());
        commonConfiguration.setUseRandomChars(config.shouldUseRandomChars());
        commonConfiguration.setFormOrDialogXpath(config.getKeepBrowserXpath());
        commonConfiguration.setUiShownTimeoutSeconds(config.getUiShownTimeoutInSeconds());
        commonConfiguration.setIntervalBetweenUiChecksMs(config.getIntervalBetweenUiChecksInMs());

        ScriptsConfiguration scriptsConfiguration = getConfiguration().getScriptsConfiguration();
        scriptsConfiguration.setDuplicationHandlerScript(readFile(config.getPathToDuplicateHandlerScript()));
        scriptsConfiguration.setElementLookupScript(readFile(config.getPathToElementLookupScript()));
        scriptsConfiguration.setIsBrowserHaveErrorScript(readFile(config.getPathToIsBrowserHaveErrorScript()));
        scriptsConfiguration.setIsSelectElementScript(readFile(config.getPathToIsSelectElementScript()));
        scriptsConfiguration.setIsUiShownScript(readFile(config.getPathToIsUiShownScript()));
        scriptsConfiguration.setScenarioProcessorScript(readFile(config.getPathToJmeterScenarioProcessorScript()));
        scriptsConfiguration.setStepProcessorScript(readFile(config.getPathToJmeterStepProcessorScript()));
        scriptsConfiguration.setScriptEventHandlerScript(readFile(config.getPathToScriptEventHandlerScript()));
        scriptsConfiguration.setShouldSkipKeyboardScript(readFile(config.getPathToShouldSkipKeyboardScript()));
        scriptsConfiguration.setIsAsyncRequestsCompletedScript(readFile(config
                .getPathToIsAsyncRequestsCompletedScript()));

        getConfiguration().getWebConfiguration().setSelectXpath(config.getSelectXpath());

        setPreProcessScenarioScript(readFile(config.getPathToPreProcessorScript()));

        getConfiguration().loadDefaults();
    }


    public static String getTagForEvent(JSONObject event)
    {
        return event.has(EventConstants.TAG) ? event.getString(EventConstants.TAG) : "null";
    }

    public static String getTargetForEvent(JSONObject event)
    {
        if (event.has(EventConstants.SECOND_TARGET))
        {
            return event.getString(EventConstants.SECOND_TARGET);
        }
        if (!event.has(EventConstants.FIRST_TARGET))
        {
            return "";
        }
        JSONArray array = event.getJSONArray(EventConstants.FIRST_TARGET);
        if (array.isNull(0))
        {
            return "";
        }

        return array.getJSONObject(0).getString("getxp");
    }

    private String readFile(String path)
    {
        try
        {
            return new String(Files.readAllBytes(Paths.get(path.trim())), StandardCharsets.UTF_8);
        }
        catch (Throwable e)
        {
            LOG.warn("Tried to read file {}, but exception occurred", path);
            LOG.warn(e.getMessage(), e);
            return null;
        }
    }

    public void checkStep(int position)
    {
        LOG.warn("checkStep method invoked, but wasn't implemented");
    }

    public void copyStep(int position)
    {
        String event = events.get(position).toString();
        JSONObject clone = new JSONObject(event);
        events.add(position, clone);
    }

    public void deleteStep(int position)
    {
        events.remove(position);
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }

    public PlayerContext getContext()
    {
        return context;
    }

    public int getPosition()
    {
        return position;
    }

    public void setPosition(int position)
    {
        this.position = position;
    }

    public JSONObject getPrevEvent(JSONObject event)
    {
        return lastEvents.get(getTagForEvent(event));
    }

    public String getScenarioFilename()
    {
        return "";
    }

    public JSONObject getStepAt(int position)
    {
        return events.get(position);
    }

    public int getStepsCount()
    {
        return events.size();
    }

    public boolean isEventBad(JSONObject event)
    {
        return !isEventOfType(event, EventType.SCRIPT) && isFieldOfEventIsNull(event, EventConstants.TARGET);
    }

    private boolean isFieldOfEventIsNull(JSONObject event, String filedName)
    {
        return !event.has(filedName) || event.get(filedName) == null || event.get(filedName) == JSONObject.NULL;
    }

    private boolean isEventOfType(JSONObject event, String type)
    {
        return event.getString(EventConstants.TYPE).equalsIgnoreCase(type);
    }

    public boolean isEventIgnored(JSONObject event)
    {
        return !ALLOWED_EVENT_TYPES.contains(event.getString(EventConstants.TYPE));
    }

    public boolean isStepDuplicates(String script, JSONObject event)
    {
        JSONObject prev = getPrevEvent(event);

        return prev != null && new PlayerScriptProcessor(this).executeDuplicateHandlerScript(script, event, prev);

    }

    public void moveToNextStep()
    {
        setPosition(Math.min(getPosition() + 1, getStepsCount()));
    }

    public void parse(String filename) throws IOException
    {
        events.clear();
        events.addAll(EventsParser.parse(FileInput.getContent(filename)));
        context.reset();
    }

    public void parseNextLine(String filename) throws IOException
    {
        events.clear();
        List<JSONObject> result = EventsParser.parse(FileInput.getLineContent(filename));
        if (result != null)
        {
            events.addAll(result);
        }
    }

    public long getEventsDuration()
    {
        return getStepAt(Math.max(0, getStepsCount() - 1)).getBigDecimal(EventConstants.TIMESTAMP).longValue()
                - getStepAt(0).getBigDecimal(EventConstants.TIMESTAMP).longValue();
    }

    public void preProcessScenario()
    {
        if (!StringUtils.isBlank(preProcessScenarioScript))
        {
            new PlayerScriptProcessor(this).preProcessScenario(preProcessScenarioScript, events);
        }
    }

    public void moveToPreviousStep()
    {
        setPosition(Math.max(0, getPosition() - 1));
    }

    public void rewind()
    {
        context.reset();
        setPosition(0);
    }

    public void runPostProcessor(String script)
    {
        PlayerScriptProcessor engine = new PlayerScriptProcessor(this);
        engine.testPostProcess(script, events);
    }

    public void saveScenario(String filename) throws IOException
    {
        FileInput.saveEvents(events, filename);
    }

    public void setPreProcessScenarioScript(String preProcessScenarioScript)
    {
        this.preProcessScenarioScript = preProcessScenarioScript;
    }

    public void skip()
    {
        setPosition(getPosition() + 1);
    }

    public void updateEvent(JSONObject event)
    {
        lastEvents.put(getTagForEvent(event), event);
    }

    public void updateStep(int position, JSONObject event)
    {
        events.set(position, event);
    }
}
