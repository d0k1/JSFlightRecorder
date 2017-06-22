package com.focusit.jsflight.jmeter;

import com.focusit.jsflight.script.constants.ScriptBindingConstants;
import com.focusit.jsflight.script.ScriptEngine;
import com.focusit.jsflight.script.jmeter.JMeterJSFlightBridge;
import groovy.lang.Binding;
import groovy.lang.Script;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to run groovy scripts against recorded samples
 * Created by doki on 25.03.16.
 */
public class JMeterScriptProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(JMeterScriptProcessor.class);
    public static final boolean SHOULD_BE_PROCESSED_DEFAULT = true;
    // script called at recording phase. Can skip sample
    private String stepProcessorScript;
    // script callled at storing phase. Can skip sample
    private String scenarioProcessorScript;
    private final ClassLoader classLoader;

    public JMeterScriptProcessor()
    {
        this.classLoader = ScriptEngine.getClassLoader();
    }

    public String getStepProcessorScript()
    {
        return stepProcessorScript;
    }

    public void setStepProcessorScript(String stepProcessorScript)
    {
        this.stepProcessorScript = stepProcessorScript;
    }

    public String getScenarioProcessorScript()
    {
        return scenarioProcessorScript;
    }

    public void setScenarioProcessorScript(String scenarioProcessorScript)
    {
        this.scenarioProcessorScript = scenarioProcessorScript;
    }

    /**
     * Post process sample with groovy script.
     *
     * @param sampler
     * @param result
     * @param recorder
     * @return is sample ok
     */
    public boolean processSampleDuringRecord(HTTPSamplerBase sampler, SampleResult result, JMeterRecorder recorder)
    {
        Binding binding = new Binding();
        binding.setVariable(ScriptBindingConstants.LOGGER, LOG);
        binding.setVariable(ScriptBindingConstants.SAMPLER, sampler);
        binding.setVariable(ScriptBindingConstants.SAMPLE, result);
        binding.setVariable(ScriptBindingConstants.CONTEXT, recorder.getContext());
        binding.setVariable(ScriptBindingConstants.JSFLIGHT, JMeterJSFlightBridge.getInstance());
        binding.setVariable(ScriptBindingConstants.CLASSLOADER, classLoader);

        Script script = ScriptEngine.getScript(getStepProcessorScript());
        if (script == null)
        {
            LOG.warn(sampler.getName() + ". No script found. Default result is " + SHOULD_BE_PROCESSED_DEFAULT);
            return SHOULD_BE_PROCESSED_DEFAULT;
        }
        script.setBinding(binding);
        LOG.info(sampler.getName() + ". Running compiled script");
        Object scriptResult = script.run();

        boolean shouldBeProcessed;
        if (scriptResult != null && scriptResult instanceof Boolean)
        {
            shouldBeProcessed = (boolean)scriptResult;
            LOG.info(sampler.getName() + ". Script result " + shouldBeProcessed);
        }
        else
        {
            shouldBeProcessed = SHOULD_BE_PROCESSED_DEFAULT;
            LOG.warn(sampler.getName() + ". Script result UNDEFINED. Default result is " + SHOULD_BE_PROCESSED_DEFAULT);
        }

        return shouldBeProcessed;
    }

    /**
     * Post process every stored request just before it get saved to disk
     *
     * @param sampler recorded http-request (sampler)
     * @param tree   HashTree (XML like data structure) that represents exact recorded sampler
     */
    public void processScenario(HTTPSamplerBase sampler, HashTree tree, Arguments userVariables, JMeterRecorder recorder)
    {
        Binding binding = new Binding();
        binding.setVariable(ScriptBindingConstants.LOGGER, LOG);
        binding.setVariable(ScriptBindingConstants.SAMPLER, sampler);
        binding.setVariable(ScriptBindingConstants.TREE, tree);
        binding.setVariable(ScriptBindingConstants.CONTEXT, recorder.getContext());
        binding.setVariable(ScriptBindingConstants.JSFLIGHT, JMeterJSFlightBridge.getInstance());
        binding.setVariable(ScriptBindingConstants.USER_VARIABLES, userVariables);
        binding.setVariable(ScriptBindingConstants.CLASSLOADER, classLoader);

        Script compiledProcessScript = ScriptEngine.getScript(getScenarioProcessorScript());
        if (compiledProcessScript == null)
        {
            return;
        }
        compiledProcessScript.setBinding(binding);
        LOG.info("Run compiled script");
        try {
            compiledProcessScript.run();
        } catch (Throwable throwable) {
            LOG.error(throwable.getMessage(), throwable);
        }
    }
}
