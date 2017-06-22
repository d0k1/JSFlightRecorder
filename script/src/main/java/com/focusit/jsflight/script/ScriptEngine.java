package com.focusit.jsflight.script;

import groovy.lang.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Groovy script "compiler"
 * Class parses the script into some intermediate form and hold a reference to it.
 * Created by doki on 06.04.16.
 */
public final class ScriptEngine
{
    private static final ScriptEngine INSTANCE = new ScriptEngine();
    private static final Logger LOG = LoggerFactory.getLogger(ScriptEngine.class);
    private static final ConcurrentHashMap<String, Class<? extends Script>> SCRIPT_CLASSES = new ConcurrentHashMap<>();
    private static final Lock initLock = new ReentrantLock();
    private static Boolean initialized = false;
    private static GroovyClassLoader groovyClassLoader;

    private ScriptEngine()
    {
    }

    private static void throwIfNotInitialized()
    {
        if (!initialized)
        {
            throw new IllegalStateException("ScriptEngine wasn't initialized");
        }
    }

    private static ScriptEngine getInstance()
    {
        initLock.lock();
        try
        {
            throwIfNotInitialized();
            return INSTANCE;
        }
        finally
        {
            initLock.unlock();
        }
    }

    public static Script getScript(String scriptBody)
    {
        return getInstance().getScriptInternal(scriptBody);
    }

    public static void init(ClassLoader classLoader)
    {
        if (initialized)
        {
            return;
        }

        initLock.lock();
        try
        {
            if (initialized)
            {
                return;
            }
            LOG.info("Initializing script engine");
            groovyClassLoader = new GroovyClassLoader(classLoader);
            initialized = true;
        }
        finally
        {
            initLock.unlock();
        }
    }

    public static ClassLoader getClassLoader()
    {
        return getInstance().getClassLoaderInternal();
    }

    private Script getScriptInternal(String scriptBody)
    {
        if (StringUtils.isBlank(scriptBody))
        {
            return null;
        }

        Class<? extends Script> clazz = SCRIPT_CLASSES.computeIfAbsent(scriptBody,
                s -> groovyClassLoader.parseClass(s));
        try
        {
            Script script = clazz.newInstance();
            script.setBinding(new Binding());
            return script;
        }
        catch (InstantiationException e)
        {
            LOG.error(String.format("Failed to create script instance:\n%s", scriptBody), e);
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e)
        {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private ClassLoader getClassLoaderInternal()
    {
        return groovyClassLoader.getParent();
    }
}
