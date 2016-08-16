package com.focusit.scenario;

import org.bson.types.ObjectId;
import org.json.JSONObject;

import com.focusit.jsflight.player.config.Configuration;
import com.focusit.jsflight.player.scenario.UserScenario;
import com.focusit.model.Event;
import com.focusit.model.Experiment;
import com.focusit.repository.EventRepositoryCustom;
import com.focusit.repository.ExperimentRepository;

/**
 * This class wraps an experiment in user scenario.
 * <p>
 * Created by doki on 12.05.16.
 */
public class MongoDbScenario extends UserScenario
{
    private final Experiment experiment;
    private EventRepositoryCustom repository;
    private ExperimentRepository experimentRepository;

    public MongoDbScenario(Experiment experiment, EventRepositoryCustom repository,
            ExperimentRepository experimentRepository)
    {
        this.experiment = experiment;
        this.repository = repository;
        this.experimentRepository = experimentRepository;
    }

    @Override
    public Configuration getConfiguration()
    {
        return experiment.getConfiguration();
    }

    @Override
    public int getPosition()
    {
        return experiment.getPosition();
    }

    @Override
    public void setPosition(int position)
    {
        experiment.setPosition(position);
        experimentRepository.save(experiment);
    }

    @Override
    public int getStepsCount()
    {
        return experiment.getSteps();
    }

    @Override
    public JSONObject getStepAt(int position) throws IllegalArgumentException
    {
        Event event = repository.getEventToReplay(new ObjectId(experiment.getRecordingId()), position);
        if (event == null)
        {
            throw new IllegalArgumentException("No event found at position " + position);
        }
        return new JSONObject(event);
    }

    @Override
    public void moveToNextStep()
    {
        setPosition(Math.min(getPosition() + 1, getStepsCount()));
    }

    @Override
    public String getScenarioFilename()
    {
        return getRecordingName();
    }

    public String getRecordingId()
    {
        return experiment.getRecordingId();
    }

    public String getRecordingName()
    {
        return experiment.getRecordingName();
    }

    public String getTag()
    {
        return experiment.getTag();
    }

    public String getTagHash()
    {
        return experiment.getTagHash();
    }

    public String getExperimentId()
    {
        return experiment.getId();
    }

    public int getFirstStep()
    {
        return experiment.getPosition();
    }

    public int getMaxStep()
    {
        return experiment.getLimit();
    }
}
