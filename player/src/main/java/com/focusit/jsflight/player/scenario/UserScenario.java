package com.focusit.jsflight.player.scenario;

import com.focusit.jsflight.player.config.Configuration;
import com.focusit.jsflight.player.constants.EventConstants;
import com.focusit.jsflight.player.constants.EventType;
import com.focusit.jsflight.player.input.Events;
import com.focusit.jsflight.player.input.FileInput;
import com.focusit.jsflight.player.script.PlayerScriptProcessor;
import com.focusit.script.player.PlayerContext;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * Recorded scenario encapsulation: parses file, plays the scenario by step, modifies the scenario, saves to a disk.
 *
 * @author Denis V. Kirpichenkov
 */
public class UserScenario
{
    private static final Set<String> ALLOWED_EVENT_TYPES = new HashSet<>(Arrays.asList(EventType.CLICK,
            EventType.KEY_PRESS, EventType.KEY_UP, EventType.KEY_DOWN, EventType.SCROLL_EMULATION,
            EventType.MOUSEWHEEL, EventType.MOUSEDOWN, EventType.SCRIPT));
    // TODO add classpath for scripts
    private static HashMap<String, JSONObject> lastEvents = new HashMap<>();
    private volatile int position = 0;
    private List<JSONObject> events = new ArrayList<>();
    private String postProcessScenarioScript = "";
    private List<Boolean> checks = new ArrayList<>();
    private PlayerContext context = new PlayerContext();
    private Configuration configuration = new Configuration();

    public void checkStep(int position)
    {

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

    public String getTagForEvent(JSONObject event)
    {
        return event.has(EventConstants.TAG) ? event.getString(EventConstants.TAG) : "null";
    }

    public String getTargetForEvent(JSONObject event)
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
        checks.set(getPosition(), true);
        setPosition(Math.min(getPosition() + 1, getStepsCount()));
    }

    public void parse(String filename) throws IOException
    {
        events.clear();
        events.addAll(new Events().parse(FileInput.getContent(filename)));
        context.reset();
    }

    public void parseNextLine(String filename) throws IOException
    {
        events.clear();
        List<JSONObject> result = new Events().parse(FileInput.getLineContent(filename));
        if (result != null)
        {
            events.addAll(result);
        }
    }

    public long postProcessScenario()
    {
        if (!postProcessScenarioScript.isEmpty())
        {
            new PlayerScriptProcessor(this).postProcessScenario(postProcessScenarioScript, events);
        }
        checks = new ArrayList<>(getStepsCount());
        for (int i = 0; i < getStepsCount(); i++)
        {
            checks.add(Boolean.FALSE);
        }

        return getStepAt(Math.max(0, getStepsCount() - 1)).getBigDecimal(EventConstants.TIMESTAMP).longValue()
                - getStepAt(0).getBigDecimal(EventConstants.TIMESTAMP).longValue();
    }

    public void moveToPreviousStep()
    {
        setPosition(Math.max(0, getPosition() - 1));
    }

    public void rewind()
    {
        checks.replaceAll(previousValue -> false);
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

    public void setPostProcessScenarioScript(String postProcessScenarioScript)
    {
        this.postProcessScenarioScript = postProcessScenarioScript;
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
