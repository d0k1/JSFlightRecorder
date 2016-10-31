package com.focusit.jsflight.server.service;

import com.focusit.jsflight.jmeter.JMeterRecorder;
import com.focusit.jsflight.player.configurations.ScriptsConfiguration;
import com.focusit.jsflight.server.scenario.MongoDbScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
        if (scenario.getConfiguration().getCommonConfiguration().getProxyPort() != null)
        {
            return;
        }
        if (!jmeterStartStopLock.tryLock() && !jmeterStartStopLock.tryLock(10, TimeUnit.SECONDS))
        {
            LOG.error("Can't acquire a lock to start JMeter");
            throw new IllegalStateException("Can't acquire a lock to start JMeter");
        }
        try
        {
            if (availablePorts.isEmpty())
            {
                throw new IllegalStateException("No ports left to start JMeter");
            }

            int port = availablePorts.remove(0);
            scenario.getConfiguration().getCommonConfiguration().setProxyPort(port);
            JMeterRecorder recorder = new JMeterRecorder();
            recorder.init();
            recorder.setProxyPort(port);
            ScriptsConfiguration config = scenario.getConfiguration().getScriptsConfiguration();

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
        Integer proxyPort = scenario.getConfiguration().getCommonConfiguration().getProxyPort();
        if (proxyPort == 0)
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

            availablePorts.add(proxyPort);
            scenario.getConfiguration().getCommonConfiguration().setProxyPort(0);

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
