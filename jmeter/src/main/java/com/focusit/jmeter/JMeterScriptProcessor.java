package com.focusit.jmeter;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.focusit.script.ScriptEngine;
import com.focusit.script.ScriptsClassLoader;
import com.focusit.script.constants.ScriptBindingConstants;

import groovy.lang.Binding;
import groovy.lang.Script;

/**
 * Helper class to run groovy scripts against recorded samples
 * Created by doki on 25.03.16.
 */
public class JMeterScriptProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(JMeterScriptProcessor.class);
    // script called at recording phase. Can skip sample
    private String recordingScript;
    // script callled at storing phase. Can skip sample
    private String processScript;
    private JMeterRecorder recorder;
    private ScriptsClassLoader classLoader;
    private ScriptEngine engine;

    public JMeterScriptProcessor(JMeterRecorder recorder, ScriptsClassLoader classLoader)
    {
        this.classLoader = classLoader;
        LOG.info(classLoader == null ? "Classloader is null" : classLoader.toString());
        engine = new ScriptEngine(classLoader);
        this.recorder = recorder;
    }

    public String getRecordingScript()
    {
        return recordingScript;
    }

    public void setRecordingScript(String recordingScript)
    {
        this.recordingScript = recordingScript;
    }

    public String getProcessScript()
    {
        return processScript;
    }

    public void setProcessScript(String processScript)
    {
        this.processScript = processScript;
    }

    /**
     * Post process sample with groovy script.
     *
     * @param sampler
     * @param result
     * @return is sample ok
     */
    public boolean processSampleDuringRecord(HTTPSamplerBase sampler, SampleResult result)
    {
        Binding binding = new Binding();
        binding.setVariable(ScriptBindingConstants.LOGGER, LOG);
        binding.setVariable(ScriptBindingConstants.REQUEST, sampler);
        binding.setVariable(ScriptBindingConstants.RESPONSE, result);
        binding.setVariable(ScriptBindingConstants.CONTEXT, recorder.getContext());
        binding.setVariable(ScriptBindingConstants.JSFLIGHT, recorder.getBridge());
        binding.setVariable(ScriptBindingConstants.CLASSLOADER, classLoader);

        boolean isOk = true;

        Script s = engine.getThreadBindedScript(recordingScript);
        if (s == null)
        {
            LOG.warn(Thread.currentThread().getName() + ":Sample " + sampler.getName()
                    + "No script found. default result " + isOk);
            return isOk;
        }
        s.setBinding(binding);
        LOG.info(Thread.currentThread().getName() + ":running " + sampler.getName() + " compiled script");
        Object scriptResult = s.run();

        if (scriptResult != null && scriptResult instanceof Boolean)
        {
            isOk = (boolean)scriptResult;
        }
        else
        {
            LOG.warn(Thread.currentThread().getName() + ":Sample " + sampler.getName()
                    + " script result UNDEFINED shifted to" + isOk);
        }

        LOG.info(Thread.currentThread().getName() + ":" + "Sample " + sampler.getName() + " script result " + isOk);
        return isOk;
    }

    /**
     * Post process every stored request just before it get saved to disk
     *
     * @param sample recorded http-request (sample)
     * @param tree   HashTree (XML like data structure) that represents exact recorded sample
     */
    public void processScenario(HTTPSamplerBase sample, HashTree tree, Arguments userVariables)
    {
        Binding binding = new Binding();
        binding.setVariable(ScriptBindingConstants.LOGGER, LOG);
        binding.setVariable(ScriptBindingConstants.SAMPLE, sample);
        binding.setVariable(ScriptBindingConstants.TREE, tree);
        binding.setVariable(ScriptBindingConstants.CONTEXT, recorder.getContext());
        binding.setVariable(ScriptBindingConstants.JSFLIGHT, recorder.getBridge());
        binding.setVariable(ScriptBindingConstants.USER_VARIABLES, userVariables);
        binding.setVariable(ScriptBindingConstants.CLASSLOADER, classLoader);

        Script compiledProcessScript = engine.getThreadBindedScript(processScript);
        if (compiledProcessScript == null)
        {
            return;
        }
        compiledProcessScript.setBinding(binding);
        compiledProcessScript.run();
    }

    public JMeterRecorder getRecorder()
    {
        return recorder;
    }
}
