package com.focusit.specs.integration

import com.focusit.jsflight.server.ServerApplication
import com.focusit.jsflight.server.model.Event
import com.focusit.jsflight.server.model.Experiment
import com.focusit.jsflight.server.repository.EventRepository
import com.focusit.jsflight.server.repository.ExperimentRepository
import com.focusit.jsflight.server.scenario.MongoDbScenario
import org.bson.types.ObjectId
import org.json.JSONObject
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

import javax.inject.Inject
/**
 * Created by dkirpichenkov on 23.05.16.
 */
@EnableAutoConfiguration(exclude = [EmbeddedMongoAutoConfiguration.class])
@ContextConfiguration(loader = SpringApplicationContextLoader.class, classes = [ServerApplication.class])
@WebIntegrationTest
@TestPropertySource("classpath:app.integration.test.properties")
public class MongoDbScenarioSpec extends Specification {
    @Inject
    EventRepository eventRepositoryCustom;
    @Inject
    ExperimentRepository experimentRepository;

    def "getNextStepAt returns an event at given position from event list filtered by recordingId and ordered by timestamp asc"() {
        given:
        String recordingId1 = new ObjectId();
        String recordingId4 = new ObjectId();

        Event e1 = new Event();
        e1.uuid = UUID.randomUUID().toString();
        e1.timestamp = System.currentTimeMillis();
        e1.recordingId = recordingId1;

        Thread.sleep(10);

        Event e2 = new Event();
        e2.uuid = UUID.randomUUID().toString();
        e2.timestamp = System.currentTimeMillis();
        e2.recordingId = recordingId1;

        Thread.sleep(10);

        Event e3 = new Event();
        e3.uuid = UUID.randomUUID().toString();
        e3.timestamp = System.currentTimeMillis();
        e3.recordingId = recordingId1;

        Thread.sleep(10);

        Event e4 = new Event();
        e4.uuid = UUID.randomUUID().toString();
        e4.recordingId = recordingId4;
        e4.timestamp = System.currentTimeMillis();

        Experiment experiment = new Experiment();
        experiment.recordingId = recordingId1;

        MongoDbScenario scenario = new MongoDbScenario(experiment, eventRepositoryCustom, experimentRepository);
        eventRepositoryCustom.save([e1, e2, e3, e4]);
        experimentRepository.save(experiment);

        when:
        JSONObject event0 = scenario.getStepAt(0);
        JSONObject event2 = scenario.getStepAt(2);

        long time0 = event0.getLong("timestamp");
        long time2 = event2.getLong("timestamp");

        then:
        event0.get("uuid").toString().equals(e1.uuid.toString());
        event2.get("uuid").toString().equals(e3.uuid.toString());

        time0 == e1.timestamp;
        time2 == e3.timestamp;

        time0 < time2

        when:
        scenario.getStepAt(3);
        then:
        thrown IllegalArgumentException
    }

    def "getStepAt with wrong position throws an IllegalStateException"() {
        given:
        String recordingId1 = new ObjectId();

        Event e1 = new Event();
        e1.uuid = UUID.randomUUID().toString();
        e1.timestamp = System.currentTimeMillis();
        e1.recordingId = recordingId1;

        Experiment experiment = new Experiment();
        experiment.recordingId = recordingId1;

        MongoDbScenario scenario = new MongoDbScenario(experiment, eventRepositoryCustom, experimentRepository);
        eventRepositoryCustom.save([e1]);
        experimentRepository.save(experiment);

        when:
        JSONObject event0 = scenario.getStepAt(0);
        then:
        event0 != null

        when:
        scenario.getStepAt(2);
        then:
        thrown IllegalArgumentException
    }

    def "next increments position in experiment and persists it"() {
        given:
        String recordingId1 = new ObjectId();
        int position = 0;
        Experiment experiment = new Experiment();
        experiment.position = position;
        experiment.recordingId = recordingId1;
        experiment.setSteps(2);
        experimentRepository.save(experiment);

        MongoDbScenario scenario = new MongoDbScenario(experiment, eventRepositoryCustom, experimentRepository);
        when:
        scenario.moveToNextStep();
        then:
        experimentRepository.findOne(experiment.getId()).position == position + 1;
    }

    def "next doesn't change the position tin experiment after last step has passed"() {
        given:
        String recordingId1 = new ObjectId();
        int position = 10;
        Experiment experiment = new Experiment();
        experiment.recordingId = recordingId1;
        experiment.position = position;
        experiment.setSteps(position);
        experimentRepository.save(experiment);

        MongoDbScenario scenario = new MongoDbScenario(experiment, eventRepositoryCustom, experimentRepository);
        when:
        scenario.moveToNextStep();
        then:
        experimentRepository.findOne(new ObjectId(experiment.getId())).position == position;
    }
}
