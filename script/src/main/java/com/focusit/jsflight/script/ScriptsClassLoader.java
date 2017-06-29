package com.focusit.jsflight.script;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

/**
 * Created by Gallyam Biktashev on 28.06.17.
 */
public class ScriptsClassLoader extends URLClassLoader
{
    public ScriptsClassLoader(URL[] urls, ClassLoader parent)
    {
        super(Arrays.stream(urls)
                .filter(url -> !url.getPath().contains("groovy"))
                .toArray(URL[]::new), parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        try
        {
            return loadClassInternal(name, resolve);
        }
        catch (ClassNotFoundException ex)
        {
            return super.loadClass(name, resolve);
        }
    }

    private Class<?> loadClassInternal(String name, boolean resolve) throws ClassNotFoundException
    {
        Class<?> c = findLoadedClass(name);
        if (c == null)
        {
            c = findClass(name);
        }

        if (resolve)
        {
            resolveClass(c);
        }
        return c;
    }

}
