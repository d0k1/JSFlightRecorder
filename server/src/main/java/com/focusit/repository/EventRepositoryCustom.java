package com.focusit.repository;

import com.focusit.model.Event;
import org.bson.types.ObjectId;

import java.util.List;

/**
 * Created by dkirpichenkov on 20.05.16.
 */
public interface EventRepositoryCustom {
    Event getEventToReplay(ObjectId recordingId, int offset);

    long countByRecordingId(ObjectId recordingId);

    void save(List<Event> lineEvents);
}
