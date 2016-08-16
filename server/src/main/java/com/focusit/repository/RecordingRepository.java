package com.focusit.repository;

import org.bson.types.ObjectId;
import org.springframework.data.repository.CrudRepository;

import com.focusit.model.Recording;

/**
 * Created by dkirpichenkov on 04.05.16.
 */
public interface RecordingRepository extends CrudRepository<Recording, ObjectId>
{

    Recording findByName(String name);
}