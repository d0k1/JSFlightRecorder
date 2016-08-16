package com.focusit.repository;

import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;

import com.focusit.model.Settings;

/**
 * Created by doki on 30.04.16.
 */
public interface SettingsRepository extends CrudRepository<Settings, ObjectId>
{

    @Override
    Settings findOne(ObjectId objectId);

}
