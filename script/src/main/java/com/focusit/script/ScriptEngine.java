package com.focusit.script;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;

/**
 * Groovy script "compiler"
 * Class parses the script into some intermediate form and hold a reference to it.
 * Also class provides a thread local storage for scripts.
 * Created by doki on 06.04.16.
 */
public class ScriptEngine
{
    // Storage to hold compiled script(s) in thread's bounds. Thread safe!
    // One script for one thread. No way to manipulate script's bindings outside calling thread
    private static final ThreadLocal<HashMap<String, Script>> threadBindedScripts = new ThreadLocal<>();
    private static final Logger LOG = LoggerFactory.getLogger(ScriptEngine.class);
    // General purpose script storage.
    // Not Thread Safe, because compiled script has writable bindings. So one thread can easily change other one's bindings
    private final ConcurrentHashMap<String, Script> generalScripts = new ConcurrentHashMap<>();
    private final Script NO_SCRIPT = new Script()
    {
        @Override
        public Object run()
        {
            return null;
        }
    };
    private volatile ClassLoader loader;
    private GroovyShell shell;

    public ScriptEngine(ClassLoader classLoader)
    {
        this.loader = classLoader;
        shell = new GroovyShell(loader);
    }

    public ClassLoader getClassLoader()
    {
        return this.loader;
    }

    @Nullable
    public Script getScript(String script)
    {
        if (script == null)
        {
            return null;
        }

        if (script.isEmpty())
        {
            return null;
        }

        Script result = generalScripts.get(script);

        if (result == null)
        {
            if (script.trim().length() > 0)
            {
                result = shell.parse(script);
            }
            else
            {
                result = NO_SCRIPT;
            }
            generalScripts.put(script, result);
        }

        if (result.equals(NO_SCRIPT))
        {
            return null;
        }

        return result;
    }

    public Script getThreadBindedScript(String script)
    {

        if (script == null)
        {

            return null;
        }

        HashMap<String, Script> scripts = threadBindedScripts.get();
        if (scripts == null)
        {
            scripts = new HashMap<>();
            threadBindedScripts.set(scripts);
        }

        Script result = scripts.get(script);

        if (result == null)
        {
            result = shell.parse(script);
            result.setBinding(new Binding());
            scripts.put(script, result);
        }

        return result;
    }
}
