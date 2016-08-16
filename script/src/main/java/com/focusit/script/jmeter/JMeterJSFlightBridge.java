package com.focusit.script.jmeter;

import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

/**
 * Class holds references to user scenario step and associated http samples.
 * Can be used during post processing of every http sample to find an appropriate cookie or other auth mechanism
 */
public class JMeterJSFlightBridge
{
    public final JSONObject NO_SCENARIO_STEP;
    private final ConcurrentHashMap<Object, JSONObject> samplersEvents = new ConcurrentHashMap<>();
    private JSONObject currentScenarioStep;

    public JMeterJSFlightBridge()
    {
        NO_SCENARIO_STEP = new JSONObject();
        NO_SCENARIO_STEP.put("DO NOT USE IT", true);
        currentScenarioStep = NO_SCENARIO_STEP;
    }

    public void addSampler(Object sampler)
    {
        samplersEvents.put(sampler, getCurrentScenarioStep());
    }

    public boolean isCurrentStepEmpty()
    {
        return currentScenarioStep.equals(NO_SCENARIO_STEP);
    }

    public JSONObject getCurrentScenarioStep()
    {
        return currentScenarioStep;
    }

    public void setCurrentScenarioStep(JSONObject currentScenarioStep)
    {
        this.currentScenarioStep = currentScenarioStep;
    }

    public JSONObject getSourceEvent(Object sampler)
    {
        return samplersEvents.get(sampler);
    }
}
