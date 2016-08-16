package com.focusit.script.jmeter;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context to hold a http sample pre/post processing context.
 * Helps to modify requests with JMeter's context variables
 * Created by doki on 25.03.16.
 */
public class JMeterRecorderContext
{
    private final ConcurrentHashMap<String, Object> templateReplacemnts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> properties = new ConcurrentHashMap<>();

    /**
     * Store a template - jmeter's user devined variable.
     *
     * @param source   pattern to be used in a jmeter scenario. e.g. ${userObject.123}
     * @param template value to be default fo source variable in jmeter scenario. e.g. userObject$123
     */
    public void addTemplate(String source, Object template)
    {
        templateReplacemnts.put(source, template);
    }

    /**
     * Get existing template (user defined variable)
     *
     * @param source
     * @return default value for udf or null if no template available at the moment
     */
    public Object getTemplate(String source)
    {
        return templateReplacemnts.get(source);
    }

    /**
     * cleanup everything
     */
    public void reset()
    {
        templateReplacemnts.clear();
    }

    /**
     * get all stored templates
     *
     * @return
     */
    public Collection<String> getSources()
    {
        return templateReplacemnts.keySet();
    }

    public void addProperty(String key, Object value)
    {
        properties.put(key, value);
    }

    public Object getProperty(String key)
    {
        return properties.get(key);
    }
}
