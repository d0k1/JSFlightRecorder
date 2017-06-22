package com.focusit.jsflight.script;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dkirpichenkov on 01.04.16.
 */
public class ScriptsClassLoaderFactory
{
    private static final Logger LOG = LoggerFactory.getLogger(ScriptsClassLoaderFactory.class);

    private static final Map<Class<?>, Class<?>> TO_PRIMITIVES = new HashMap<>();

    static
    {
        TO_PRIMITIVES.put(Integer.class, Integer.TYPE);
        TO_PRIMITIVES.put(Byte.class, Byte.TYPE);
        TO_PRIMITIVES.put(Short.class, Short.TYPE);
        TO_PRIMITIVES.put(Long.class, Long.TYPE);
        TO_PRIMITIVES.put(Boolean.class, Boolean.TYPE);
        TO_PRIMITIVES.put(Float.class, Float.TYPE);
        TO_PRIMITIVES.put(Double.class, Double.TYPE);
        TO_PRIMITIVES.put(Character.class, Character.TYPE);
    }

    private static Method getMethod(Class<?> clazz, String name, Class... args) throws NoSuchMethodException
    {
        Class<?> startingClass = clazz;
        while (clazz != null)
        {
            Method method = getMethodUsingObjectClasses(clazz, name, args);
            if (method == null)
            {
                method = getMethodUsingPrimitiveClasses(clazz, name, args);
            }
            if (method != null)
            {
                return method;
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchMethodException(String.format(
                "There is no method %s(%s) neither in %s nor in it's superclasses", name, StringUtils.join(args, ", "),
                startingClass));
    }

    private static Method getMethodUsingPrimitiveClasses(Class<?> clazz, String name, Class[] args)
    {
        args = convertToPrimitiveClasses(args);

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Searching for method {}({}) in {} using primitives", name, StringUtils.join(args, ", "), clazz);
        }
        try
        {
            return clazz.getDeclaredMethod(name, args);
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    private static Class<?>[] convertToPrimitiveClasses(Class[] args)
    {
        return Arrays.stream(args).map(arg -> TO_PRIMITIVES.getOrDefault(arg, arg)).toArray(Class<?>[]::new);
    }

    private static Method getMethodUsingObjectClasses(Class<?> clazz, String name, Class[] args)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("Searching for method {}({}) in {}", name, StringUtils.join(args, ", "), clazz);
        }
        try
        {
            return clazz.getDeclaredMethod(name, args);
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }

    public static ClassLoader createScriptsClassLoader(ClassLoader parent, URL... urls)
    {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(ClassLoader.class);

        URLClassLoader urlClassLoader = new URLClassLoader(Arrays.stream(urls)
                .filter(url -> !url.getPath().contains("groovy")).toArray(URL[]::new), null);

        enhancer.setCallback((MethodInterceptor)(proxy, method, args, fastMethod) -> {
            LOG.debug("Calling {} with args: {}", method, args);

            Class[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);

            try
            {
                try
                {
                    return invokeUrlClassLoaderMethod(urlClassLoader, method.getName(), args, argTypes);
                }
                catch (InvocationTargetException e)
                {
                    return invokeParentClassLoaderMethod(parent, method.getName(), args, argTypes);
                }
            }
            catch (InvocationTargetException e)
            {
                throw e.getCause();
            }
        });
        return (ClassLoader)enhancer.create(new Class[] { ClassLoader.class }, new Object[] { null });
    }

    private static Object invokeUrlClassLoaderMethod(URLClassLoader urlClassLoader, String methodName, Object[] args,
            Class[] argTypes) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException
    {
        Method urlMethod = getMethod(urlClassLoader.getClass(), methodName, argTypes);
        boolean isUrlMethodAccessible = urlMethod.isAccessible();
        urlMethod.setAccessible(true);

        try
        {
            Object result = urlMethod.invoke(urlClassLoader, args);
            LOG.debug("Done: calling {} with result: {}", urlMethod, result);
            if (result == null)
            {
                throw new InvocationTargetException(new Exception(
                        "UrlClassLoader returned null. Trying with parent ClassLoader"));
            }
            return result;
        }
        finally
        {
            urlMethod.setAccessible(isUrlMethodAccessible);
        }
    }

    private static Object invokeParentClassLoaderMethod(ClassLoader parent, String methodName, Object[] args,
            Class[] argTypes) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException
    {
        Method parentMethod = getMethod(parent.getClass(), methodName, argTypes);
        boolean isParentMethodAccessible = parentMethod.isAccessible();
        parentMethod.setAccessible(true);

        try
        {
            Object result = parentMethod.invoke(parent, args);
            LOG.debug("Done: calling {} with result: {}", parentMethod, result);
            return result;
        }
        finally
        {
            parentMethod.setAccessible(isParentMethodAccessible);
        }
    }

}
