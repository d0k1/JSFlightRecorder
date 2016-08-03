package com.focusit.jsflight.player.script;

import com.focusit.jsflight.player.scenario.UserScenario;
import com.focusit.jsflight.player.webdriver.WebDriverWrapper;
import com.focusit.script.ScriptEngine;
import groovy.lang.Binding;
import groovy.lang.Script;
import groovy.text.SimpleTemplateEngine;
import org.json.JSONObject;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/**
 * PlayerScriptProcessor that runs groovy scripts or GString templates
 *
 * @author Denis V. Kirpichenkov
 */
public class PlayerScriptProcessor {
    private static final Logger log = LoggerFactory.getLogger(PlayerScriptProcessor.class);
    private ScriptEngine engine;
    private UserScenario scenario;

    public PlayerScriptProcessor(UserScenario scenario) {
        engine = new ScriptEngine(scenario.getConfiguration().getCommonConfiguration().getScriptClassloader());
        this.scenario = scenario;
    }

    public boolean executeSelectDeterminerScript(String script, WebDriver wd, WebElement element) {
        Binding binding = getBasicBinding();
        binding.setVariable("webdriver", wd);
        binding.setVariable("element", element);

        Script scr = engine.getThreadBindedScript(script);
        scr.setBinding(binding);
        return (boolean) scr.run();
    }

    /**
     * @param script
     * @param currentEvent
     * @param prevEvent
     * @return true if currentEvent duplicates prevEvent and should be skipped
     */
    public boolean executeDuplicateHandlerScript(String script, JSONObject currentEvent, JSONObject prevEvent) {
        Binding binding = getBasicBinding();
        binding.setVariable("current", currentEvent);
        binding.setVariable("previous", prevEvent);
        Script scr = engine.getThreadBindedScript(script);

        if (scr == null) {
            return false;
        }

        scr.setBinding(binding);
        return (boolean) scr.run();
    }

    public void executeScriptEvent(String script, JSONObject event) {
        Binding binging = getBasicBinding();
        binging.setVariable("event", event);

        Script scr = engine.getThreadBindedScript(script);
        scr.setBinding(binging);
        scr.run();
    }

    /**
     * @return found element for further processing or null
     */
    @Nullable
    public Object executeWebLookupScript(String script, WebDriver wd, String target, JSONObject event) {
        Binding binding = getBasicBinding();
        binding.setVariable("webdriver", wd);
        binding.setVariable("target", target);
        binding.setVariable("event", event);
        Script scr = engine.getThreadBindedScript(script);

        if (scr == null) {
            throw new NoSuchElementException("no web lookup script provided");
        }

        scr.setBinding(binding);
        return scr.run();
    }

    /**
     * Modifies existing array of events.
     * Modification defined by script
     *
     * @param events
     */
    public void postProcessScenario(String script, List<JSONObject> events) {
        Binding binding = getBasicBinding();
        //binding.setVariable("context", context);
        binding.setVariable("events", events);
        Script s = engine.getThreadBindedScript(script);

        if (s == null) {
            return;
        }

        s.setBinding(binding);
        s.run();
    }

    public void runStepPrePostScript(JSONObject event, int step, boolean pre) {
        String script = "";
        if (pre) {
            script = event.has("pre") ? event.getString("pre") : "";
        } else {
            script = event.has("post") ? event.getString("post") : "";
        }

        if (script.trim().length() == 0) {
            return;
        }

        Binding binding = getBasicBinding();
        binding.setVariable("scenario", scenario);
        binding.setVariable("step", step);
        binding.setVariable("pre", pre);
        binding.setVariable("post", !pre);
        Script s = engine.getThreadBindedScript(script);

        if (s == null) {
            return;
        }

        s.setBinding(binding);
        s.run();
    }

    public JSONObject runStepTemplating(UserScenario scenario, JSONObject step) {
        SimpleTemplateEngine templateEngine = new SimpleTemplateEngine();
        Binding binding = scenario.getContext().asBindings();
        JSONObject result = new JSONObject(step.toString());
        result.keySet().forEach(key -> {
            if (result.get(key) instanceof String) {
                try {
                    String source = result.getString(key);

                    source = source.replaceAll("(\\$)(?!\\{)", Matcher.quoteReplacement("\\$"));

                    String parsed = templateEngine.createTemplate(source).make(binding.getVariables()).toString();
                    result.put(key, parsed);
                } catch (Exception e) {
                    log.error(e.toString(), e);
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
    public void testPostProcess(String script, List<JSONObject> events) {
        Binding binding = getBasicBinding();
        binding.setVariable("ctx", new ConcurrentHashMap<>());
        binding.setVariable("events", new ArrayList<>(events));

        Script s = engine.getThreadBindedScript(script);
        s.setBinding(binding);
        s.run();
    }

    public void executeDriverSignalScript(String script, WebDriver wd, int signal) {
        Binding binding = getBasicBinding();
        binding.setVariable("signal", signal);
        WebDriver d = wd instanceof WebDriverWrapper ? ((WebDriverWrapper) wd).getWrappedDriver() : wd;
        binding.setVariable("webdriver", d);

        Script scr = engine.getThreadBindedScript(script);
        scr.setBinding(binding);
        scr.run();
    }

    private Binding getBasicBinding() {

        Binding basicBinding = new Binding();
        basicBinding.setVariable("logger", log);
        basicBinding.setVariable("classloader", engine.getClassLoader());
        basicBinding.setVariable("playerContext", scenario.getContext());

        return basicBinding;
    }

}
