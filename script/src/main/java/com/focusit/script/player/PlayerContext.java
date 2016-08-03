package com.focusit.script.player;

import com.focusit.script.jmeter.JMeterJSFlightBridge;
import groovy.lang.Binding;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class PlayerContext {
    private static final ConcurrentHashMap<String, Object> context = new ConcurrentHashMap<>();
    private JMeterJSFlightBridge jmeterBridge;

    public Binding asBindings() {
        return new Binding(context);
    }

    public Object get(String key) {
        return context.get(key);
    }

    public void setCurrentScenarioStep(JSONObject currentScenarioStep) {
        if (jmeterBridge != null) {
            jmeterBridge.setCurrentScenarioStep(currentScenarioStep);
        }
    }

    public void put(String key, Object value) {
        context.put(key, value);
    }

    public void reset() {
        context.keySet().forEach(context::remove);
    }

    public void setJMeterBridge(JMeterJSFlightBridge JMeterBridge) {
        this.jmeterBridge = JMeterBridge;
    }
}
