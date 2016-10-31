package com.focusit.jsflight.server.repository;

import java.util.List;

import org.bson.types.ObjectId;

import com.focusit.jsflight.server.model.Event;

/**
 * Created by dkirpichenkov on 20.05.16.
 */
public interface EventRepository
{
    Event getEventToReplay(ObjectId recordingId, int offset);

    long countByRecordingId(ObjectId recordingId);

    void save(List<Event> lineEvents);
}
