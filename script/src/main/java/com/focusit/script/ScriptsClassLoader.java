package com.focusit.script;

import java.net.URL;
import java.net.URLClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dkirpichenkov on 01.04.16.
 */
public class ScriptsClassLoader extends URLClassLoader
{
    private static final Logger log = LoggerFactory.getLogger(ScriptsClassLoader.class);

    public ScriptsClassLoader(ClassLoader parent, URL urls[])
    {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        return super.loadClass(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException
    {
        return super.findClass(name);
    }
}
