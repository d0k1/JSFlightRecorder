package com.focusit.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.focusit.jmeter.JMeterRecorder;
import com.focusit.jsflight.player.config.JMeterConfiguration;
import com.focusit.scenario.MongoDbScenario;

/**
 * Created by dkirpichenkov on 19.05.16.
 */
@Service
public class JMeterRecorderService
{
    private static final Logger LOG = LoggerFactory.getLogger(JMeterRecorderService.class);
    private Map<String, JMeterRecorder> jmeters = new ConcurrentHashMap<>();
    private List<Integer> availablePorts = new ArrayList<>(64356);
    private ReentrantLock jmeterStartStopLock = new ReentrantLock();

    private MongoDbStorageService storageService;

    @Inject
    public JMeterRecorderService(MongoDbStorageService storageService)
    {
        this.storageService = storageService;

        for (int i = 1025; i < 64530; i++)
        {
            availablePorts.add(i);
        }
    }

    public void startJMeter(MongoDbScenario scenario) throws Exception
    {
        if (!scenario.getConfiguration().getCommonConfiguration().getProxyPort().isEmpty())
        {
            return;
        }
        if (jmeterStartStopLock.tryLock() || jmeterStartStopLock.tryLock(10, TimeUnit.SECONDS))
        {
            if (availablePorts.size() < 0)
            {
                throw new IllegalArgumentException("No ports left to start JMeter");
            }
        }
        else
        {
            LOG.error("Can't acquire a lock to start JMeter");
        }
        try
        {
            int port = availablePorts.get(0);
            availablePorts.remove(0);
            scenario.getConfiguration().getCommonConfiguration().setProxyPort("" + port);
            JMeterRecorder recorder = new JMeterRecorder(scenario.getConfiguration().getCommonConfiguration()
                    .getScriptClassloader());
            recorder.init();
            recorder.setProxyPort(port);
            JMeterConfiguration config = scenario.getConfiguration().getjMeterConfiguration();

            config.syncScripts(recorder);

            jmeters.put(scenario.getExperimentId(), recorder);
            recorder.startRecording();
        }
        catch (IOException e)
        {
            LOG.error(e.toString(), e);
            throw e;
        }
        finally
        {
            jmeterStartStopLock.unlock();
        }
    }

    public void stopJMeter(MongoDbScenario scenario) throws Exception
    {
        String proxyPort = scenario.getConfiguration().getCommonConfiguration().getProxyPort();
        if (proxyPort.isEmpty() || proxyPort.equalsIgnoreCase("-1"))
        {
            return;
        }

        if (!jmeterStartStopLock.tryLock() && !jmeterStartStopLock.tryLock(10, TimeUnit.SECONDS))
        {
            LOG.error("Can't acquire a lock to stop JMeter");
            throw new IllegalStateException("Can't acquire a lock to stop JMeter");
        }
        try
        {
            JMeterRecorder recorder = jmeters.get(scenario.getExperimentId());
            if (recorder == null)
            {
                throw new IllegalStateException("Instance of JMeterRecorder not found");
            }

            recorder.stopRecording();

            availablePorts.add(Integer.parseInt(proxyPort));
            scenario.getConfiguration().getCommonConfiguration().setProxyPort("");

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
            {
                recorder.saveScenario(baos);
                try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray()))
                {
                    storageService.storeJMeterScenario(scenario, bais);
                }
            }
        }
        finally
        {
            jmeterStartStopLock.unlock();
        }
    }
}
