package com.focusit.jsflight.player.script;

import com.focusit.jsflight.player.constants.EventConstants;
import com.focusit.jsflight.player.scenario.UserScenario;
import com.focusit.script.ScriptEngine;
import com.focusit.script.constants.ScriptBindingConstants;
import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.text.SimpleTemplateEngine;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/**
 * PlayerScriptProcessor that runs groovy scripts or GString templates
 *
 * @author Denis V. Kirpichenkov
 */
public class PlayerScriptProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(PlayerScriptProcessor.class);
    private ScriptEngine engine;
    private UserScenario scenario;

    public PlayerScriptProcessor(UserScenario scenario)
    {
        engine = new ScriptEngine(scenario.getConfiguration().getCommonConfiguration().getScriptClassloader());
        this.scenario = scenario;
    }

    public static Map<String, Object> getEmptyBindingsMap()
    {
        return new HashMap<>();
    }

    public boolean executeSelectDeterminerScript(String script, WebDriver wd, WebElement element)
    {
        Map<String, Object> binding = getEmptyBindingsMap();
        binding.put(ScriptBindingConstants.WEB_DRIVER, wd);
        binding.put(ScriptBindingConstants.ELEMENT, element);

        return executeGroovyScript(script, binding, Boolean.class);
    }

    /**
     * @param script
     * @param currentEvent
     * @param prevEvent
     * @return true if currentEvent duplicates prevEvent and should be skipped
     */
    public boolean executeDuplicateHandlerScript(String script, JSONObject currentEvent, JSONObject prevEvent)
    {
        Map<String, Object> binding = getEmptyBindingsMap();
        binding.put(ScriptBindingConstants.CURRENT, currentEvent);
        binding.put(ScriptBindingConstants.PREVIOUS, prevEvent);

        try
        {
            return executeGroovyScript(script, binding, Boolean.class);
        }
        catch (Throwable e)
        {
            LOG.warn("Failed to create duplicateHandler script. Default value is false", e);
            return false;
        }
    }

    public void executeScriptEvent(String script, JSONObject event)
    {
        Map<String, Object> binding = getEmptyBindingsMap();
        binding.put(ScriptBindingConstants.EVENT, event);

        executeGroovyScript(script, binding);
    }

    /**
     * Modifies existing array of events.
     * Modification defined by script
     *
     * @param events
     */
    public void postProcessScenario(String script, List<JSONObject> events)
    {
        Map<String, Object> binding = getEmptyBindingsMap();
        binding.put(ScriptBindingConstants.EVENTS, events);

        try
        {
            executeGroovyScript(script, binding);
        }
        catch (Throwable ignored)
        {
        }
    }

    public void runStepPrePostScript(JSONObject event, int step, boolean pre)
    {
        String script;
        if (pre)
        {
            script = event.has(EventConstants.PRE) ? event.getString(EventConstants.PRE) : "";
        }
        else
        {
            script = event.has(EventConstants.POST) ? event.getString(EventConstants.POST) : "";
        }

        if (script.trim().isEmpty())
        {
            return;
        }

        Map<String, Object> binding = getEmptyBindingsMap();
        binding.put(ScriptBindingConstants.SCENARIO, scenario);
        binding.put(ScriptBindingConstants.STEP, step);
        binding.put(ScriptBindingConstants.PRE, pre);
        binding.put(ScriptBindingConstants.POST, !pre);

        try
        {
            executeGroovyScript(script, binding);
        }
        catch (Throwable ignored)
        {
        }
    }

    public JSONObject runStepTemplating(UserScenario scenario, JSONObject step)
    {
        SimpleTemplateEngine templateEngine = new SimpleTemplateEngine();
        Binding binding = scenario.getContext().asBindings();
        JSONObject result = new JSONObject(step.toString());
        result.keySet().forEach(key -> {
            if (result.get(key) instanceof String)
            {
                try
                {
                    String source = result.getString(key);

                    source = source.replaceAll("(\\$)(?!\\{)", Matcher.quoteReplacement("\\$"));

                    String parsed = templateEngine.createTemplate(source).make(binding.getVariables()).toString();
                    result.put(key, parsed);
                }
                catch (Exception e)
                {
                    LOG.error(e.toString(), e);
                }
            }
        });
        return result;
    }

    /**
     * Test of events modification script.
     * Doesn't modify real loaded events. just use it's clone, modify and println every event to stdout
     *
     * @param events
     */
    public void testPostProcess(String script, List<JSONObject> events)
    {
        Map<String, Object> binding = getEmptyBindingsMap();
        binding.put(ScriptBindingConstants.CONTEXT, new ConcurrentHashMap<>());
        binding.put(ScriptBindingConstants.EVENTS, new ArrayList<>(events));

        executeGroovyScript(script, binding);
    }

    public void executeProcessSignalScript(String script, int signal, String pid)
    {
        Map<String, Object> binding = getEmptyBindingsMap();
        binding.put(ScriptBindingConstants.SIGNAL, signal);
        binding.put(ScriptBindingConstants.PID, pid);

        executeGroovyScript(script, binding);
    }

    public Object executeGroovyScript(String script, Map<String, Object> bindings)
    {
        return executeGroovyScript(script, bindings, Object.class);
    }

    public <T> T executeGroovyScript(String scriptBody, Map<String, Object> bindings, Class<T> clazz)
    {
        LOG.debug("Executing script:\n", scriptBody);
        Binding binding = new Binding(bindings);
        addBasicBindings(binding);

        Script script = engine.getThreadBindedScript(scriptBody);

        if (script == null)
        {
            LOG.error("Failed to create script");
            throw new RuntimeException("Failed to create script:" + scriptBody);
        }

        script.setBinding(binding);
        try
        {
            return clazz.cast(script.run());
        }
        catch (ClassCastException ex)
        {
            LOG.warn(ex.getMessage(), ex);
            return null;
        }
    }

    private void addBasicBindings(Binding binding)
    {
        binding.setVariable(ScriptBindingConstants.LOGGER, LOG);
        binding.setVariable(ScriptBindingConstants.CLASSLOADER, engine.getClassLoader());
        binding.setVariable(ScriptBindingConstants.PLAYER_CONTEXT, scenario.getContext());
    }
}
