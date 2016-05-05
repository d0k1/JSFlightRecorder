package com.focusit.script.player;

import com.focusit.script.jmeter.JMeterJSFlightBridge;
import groovy.lang.Binding;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class PlayerContext
{
    private static final ConcurrentHashMap<String, Object> context = new ConcurrentHashMap<>();

    public Binding asBindings()
    {
        Binding binding = new Binding(context);
        return binding;
    }

    public Object get(String key)
    {
        return context.get(key);
    }

    public void setCurrentScenarioStep(JSONObject currentScenarioStep)
    {
        JMeterJSFlightBridge.getInstace().setCurrentScenarioStep(currentScenarioStep);
    }

    public void put(String key, Object value)
    {
        context.put(key, value);
    }

    public void reset()
    {
        context.keySet().forEach(it -> {
            context.remove(it);
        });
    }

}