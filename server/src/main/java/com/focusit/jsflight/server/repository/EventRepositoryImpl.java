package com.focusit.jsflight.server.repository;

import com.focusit.jsflight.server.model.Event;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by dkirpichenkov on 20.05.16.
 */
@Repository
public class EventRepositoryImpl implements EventRepository
{
    private MongoTemplate mongoTemplate;

    @Inject
    public EventRepositoryImpl(MongoTemplate mongoTemplate)
    {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Event getEventToReplay(ObjectId recordingId, int offset)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("recordingId").is(recordingId));
        query.with(new Sort(Sort.Direction.ASC, "timestamp"));
        query.skip(offset);
        return mongoTemplate.findOne(query, Event.class);
    }

    @Override
    public long countByRecordingId(ObjectId recordingId)
    {
        Query query = new Query();
        query.addCriteria(Criteria.where("recordingId").is(recordingId));

        return mongoTemplate.count(query, Event.class);
    }

    @Override
    public void save(List<Event> lineEvents)
    {
        mongoTemplate.insertAll(lineEvents);
    }
}
