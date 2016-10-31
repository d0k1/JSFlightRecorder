package com.focusit.jsflight.server.service;

import com.focusit.jsflight.player.configurations.Configuration;
import com.focusit.jsflight.server.model.Experiment;

/**
 * Factory to provide an Experiment with default cconfiguration
 * Created by doki on 12.05.16.
 */
public class ExperimentFactory
{

    public Experiment get()
    {
        Experiment result = new Experiment();

        Configuration cfg = new Configuration();

        cfg.loadDefaults();

        result.setConfiguration(cfg);
        return result;
    }
}
