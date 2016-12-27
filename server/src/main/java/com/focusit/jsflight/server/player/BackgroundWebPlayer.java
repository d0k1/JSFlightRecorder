package com.focusit.jsflight.server.player;

import com.focusit.jsflight.player.cli.config.IConfig;
import com.focusit.jsflight.player.cli.config.PropertiesConfig;
import com.focusit.jsflight.player.webdriver.SeleniumDriver;
import com.focusit.jsflight.script.ScriptEngine;
import com.focusit.jsflight.server.model.Experiment;
import com.focusit.jsflight.server.model.Recording;
import com.focusit.jsflight.server.player.exceptions.ErrorInBrowserPlaybackException;
import com.focusit.jsflight.server.player.exceptions.PausePlaybackException;
import com.focusit.jsflight.server.player.exceptions.TerminatePlaybackException;
import com.focusit.jsflight.server.repository.EventRepository;
import com.focusit.jsflight.server.repository.ExperimentRepository;
import com.focusit.jsflight.server.repository.RecordingRepository;
import com.focusit.jsflight.server.scenario.MongoDbScenario;
import com.focusit.jsflight.server.scenario.MongoDbScenarioProcessor;
import com.focusit.jsflight.server.service.EmailNotificationService;
import com.focusit.jsflight.server.service.ExperimentFactory;
import com.focusit.jsflight.server.service.JMeterRecorderService;
import com.focusit.jsflight.server.service.MongoDbStorageService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Component that plays a scenario in background
 * 
 * Created by dkirpichenkov on 05.05.16.
 */
@Service
public class BackgroundWebPlayer
{
    private static final Logger LOG = LoggerFactory.getLogger(BackgroundWebPlayer.class);

    private MongoDbStorageService storageService;
    private EmailNotificationService notificationService;
    private JMeterRecorderService recorderService;

    private RecordingRepository recordingRepository;
    private EventRepository eventRepository;
    private ExperimentRepository experimentRepository;

    private Map<String, Map<String, String>> experimentLastUrls = new ConcurrentHashMap<>();
    private Map<String, CompletableFuture> playingFutures = new ConcurrentHashMap<>();
    private Map<String, SeleniumDriver> experimentDriver = new ConcurrentHashMap<>();

    private IConfig config;

    @Inject
    public BackgroundWebPlayer(MongoDbStorageService screenshotsService, RecordingRepository recordingRepository,
            EventRepository eventRepository, ExperimentRepository experimentRepository,
            EmailNotificationService notificationService, JMeterRecorderService recorderService)
    {
        this.storageService = screenshotsService;
        this.recordingRepository = recordingRepository;
        this.eventRepository = eventRepository;
        this.experimentRepository = experimentRepository;
        this.notificationService = notificationService;
        this.recorderService = recorderService;

        config = getConfig();
    }

    @Nullable
    private PropertiesConfig getConfig()
    {
        if (System.getProperty("configFile") != null)
        {
            LOG.info("Loading properties from '{}' file", System.getProperty("configFile"));
            return new PropertiesConfig(System.getProperty("configFile"));
        }

        return new PropertiesConfig();
    }

    public Experiment start(String recordingId, boolean withScreenshots, boolean paused) throws Exception
    {
        Recording rec = recordingRepository.findOne(new ObjectId(recordingId));
        if (rec == null)
        {
            throw new IllegalArgumentException("no recording found for id " + recordingId);
        }

        Experiment experiment = new ExperimentFactory().get();
        experiment.setCreated(new Date());
        experiment.setRecordingName(rec.getName());
        experiment.setRecordingId(rec.getId());
        experiment.setScreenshots(withScreenshots);
        experiment.setSteps((int)eventRepository.countByRecordingId(new ObjectId(recordingId)));
        experiment.setPosition(config.getStartStep());
        experiment.setLimit(config.getFinishStep());

        experimentRepository.save(experiment);

        if (!paused)
        {
            resume(experiment.getId());
        }

        return experiment;

    }

    private void startJMeter(MongoDbScenario scenario) throws Exception
    {
        recorderService.startJMeter(scenario);
    }

    private void stopJMeter(MongoDbScenario scenario) throws Exception
    {
        recorderService.stopJMeter(scenario);
    }

    public void resume(String experimentId) throws Exception
    {
        Experiment experiment = experimentRepository.findOne(new ObjectId(experimentId));
        if (experiment == null)
        {
            throw new IllegalArgumentException("No experiment found by given id " + experimentId);
        }

        experiment.setPlaying(true);
        int stepCount = (int)eventRepository.countByRecordingId(new ObjectId(experiment.getRecordingId()));
        experiment.setSteps(stepCount);
        experimentRepository.save(experiment);

        MongoDbScenario scenario = new MongoDbScenario(experiment, eventRepository, experimentRepository);
        scenario.initFromConfig(config);
        MongoDbScenarioProcessor processor = new MongoDbScenarioProcessor(storageService);

        ScriptEngine.init(scenario.getConfiguration().getCommonConfiguration().getScriptClassloader());
        if (config.shouldEnableRecording())
        {
            startJMeter(scenario);
        }
        experimentRepository.save(experiment);

        Map<String, String> lastUrls = experimentLastUrls.getOrDefault(experimentId, new ConcurrentHashMap<>());
        experimentLastUrls.put(experimentId, lastUrls);

        SeleniumDriver driver = experimentDriver.getOrDefault(experimentId,
                new SeleniumDriver(scenario, config.getXvfbDisplayLowerBound(), config.getXvfbDisplayUpperBound()))
                .setLastUrls(lastUrls);
        experimentDriver.put(experimentId, driver);

        playingFutures
                .put(experimentId,
                        CompletableFuture
                                .runAsync(
                                        () -> processor.play(scenario, driver, scenario.getFirstStep(),
                                                scenario.getMaxStep()))
                                .whenCompleteAsync(
                                        (aVoid, throwable) -> {
                                            playingFutures.remove(experimentId);

                                            boolean finished = true;
                                            boolean error = false;

                                            EmailNotificationService.EventType eventType;
                                            if (throwable == null)
                                            {
                                                experimentLastUrls.remove(experimentId);
                                                experimentDriver.remove(experimentId);

                                                eventType = EmailNotificationService.EventType.DONE;
                                            }
                                            else
                                            {
                                                LOG.error(throwable.getMessage(), throwable);
                                                finished = false;

                                                if (throwable instanceof PausePlaybackException)
                                                {
                                                    eventType = EmailNotificationService.EventType.PUASED;
                                                }
                                                else if (throwable instanceof ErrorInBrowserPlaybackException)
                                                {
                                                    eventType = EmailNotificationService.EventType.ERROR_IN_BROWSER;
                                                }
                                                else if (throwable instanceof TerminatePlaybackException)
                                                {
                                                    experimentLastUrls.remove(experimentId);
                                                    experimentDriver.remove(experimentId);

                                                    finished = true;
                                                    eventType = EmailNotificationService.EventType.TERMINATED;
                                                }
                                                else
                                                {
                                                    experiment.setErrorMessage(throwable.toString());

                                                    error = true;
                                                    eventType = EmailNotificationService.EventType.UNKNOWN_ERROR;
                                                }
                                            }
                                            try
                                            {
                                                if (config.shouldEnableRecording())
                                                {
                                                    stopJMeter(scenario);
                                                }
                                            }
                                            catch (Exception e)
                                            {
                                                LOG.error(e.toString(), e);
                                            }
                                            experiment.setPlaying(false);
                                            experiment.setFinished(finished);
                                            experiment.setError(error);
                                            experimentRepository.save(experiment);

                                            if (finished)
                                            {
                                                LOG.info("Playing experiment with ID: '{}' finished", experimentId);
                                            }
                                            else if (error)
                                            {
                                                LOG.info("While playing experiment with ID: '{}' an error occurred",
                                                        experimentId);
                                            }
                                            else
                                            {
                                                LOG.info(
                                                        "Playing experiment with ID: '{}' paused, ot error in browser occurred",
                                                        experimentId);
                                            }

                                            notificationService.notifySubscribers(scenario, throwable, eventType);
                                        }));
    }

    public void pause(String experimentId)
    {
        CompletableFuture future = playingFutures.get(experimentId);
        if (future == null)
        {
            throw new IllegalArgumentException("Experiment " + experimentId + " is not playing now");
        }
        future.completeExceptionally(new PausePlaybackException());
    }

    public void cancel(String experimentId)
    {
        CompletableFuture future = playingFutures.get(experimentId);
        if (future == null)
        {
            throw new IllegalArgumentException("Experiment " + experimentId + " is not playing now");
        }
        future.completeExceptionally(new TerminatePlaybackException());
    }

    public Experiment status(String experimentId)
    {
        Experiment experiment = experimentRepository.findOne(new ObjectId(experimentId));

        if (experiment == null)
        {
            throw new IllegalArgumentException("no experiment found for id " + experimentId);
        }

        return experiment;
    }

    public InputStream getScreenshot(String experimentId, int step)
    {
        Experiment experiment = experimentRepository.findOne(new ObjectId(experimentId));
        return storageService.getScreenshot(experiment.getRecordingName(), experimentId, step);
    }

    public InputStream getErrorScreenshot(String experimentId, int step)
    {
        Experiment experiment = experimentRepository.findOne(new ObjectId(experimentId));
        return storageService.getErrorScreenShot(experiment.getRecordingName(), experimentId, step);
    }

    public void move(String experimentId, int step)
    {
        Experiment experiment = experimentRepository.findOne(new ObjectId(experimentId));
        experiment.setPosition(step);
        experimentRepository.save(experiment);
    }

    public void terminable()
    {

    }

    public List<Experiment> getAllExperiments()
    {
        ArrayList<Experiment> result = new ArrayList<>();
        experimentRepository.findAll().forEach(result::add);
        return result;
    }

    public InputStream getJMX(String experimentId)
    {
        Experiment experiment = experimentRepository.findOne(new ObjectId(experimentId));
        return storageService.getJMeterScenario(experiment.getRecordingName(), experimentId);
    }
}
