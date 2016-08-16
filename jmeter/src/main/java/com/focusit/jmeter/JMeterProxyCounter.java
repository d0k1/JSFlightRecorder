package com.focusit.jmeter;

import java.util.concurrent.atomic.AtomicInteger;

public class JMeterProxyCounter
{
    private final static JMeterProxyCounter instance = new JMeterProxyCounter();

    public final AtomicInteger counter = new AtomicInteger(0);

    private JMeterProxyCounter()
    {

    }

    public static JMeterProxyCounter getInstance()
    {
        return instance;
    }
}
