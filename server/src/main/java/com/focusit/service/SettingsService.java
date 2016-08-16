package com.focusit.service;

import static com.focusit.model.Settings.SETTINGS_ID;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.focusit.model.Settings;
import com.focusit.repository.SettingsRepository;

/**
 * Created by doki on 30.04.16.
 */
@Service
public class SettingsService
{
    private static final Logger LOG = LoggerFactory.getLogger(SettingsService.class);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private SettingsRepository repository;
    private volatile Settings settings;

    @Inject
    public SettingsService(SettingsRepository repository)
    {
        this.repository = repository;
    }

    @PostConstruct
    public void init()
    {
        lock.writeLock().lock();
        try
        {
            Settings settings = repository.findOne(new ObjectId(SETTINGS_ID));
            if (settings == null)
            {
                System.err.println("No settings found. Creating default");
                settings = new Settings();
                repository.save(settings);
            }
            this.settings = settings;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    public Settings getSettings()
    {
        lock.readLock().lock();
        try
        {
            return settings;
        }
        finally
        {
            lock.readLock().unlock();
        }
    }
}
