package com.focusit.player.specs

import com.focusit.jsflight.player.scenario.UserScenario
import com.focusit.jsflight.player.script.PlayerScriptProcessor
import com.focusit.jsflight.script.ScriptEngine
import org.bson.types.ObjectId
import org.json.JSONObject
/**
 * Created by doki on 03.02.17.
 */
class PlayerScriptProcessorSpec extends BaseSpec {
    UserScenario scenario;
    PlayerScriptProcessor proc;

    def setup() {
        ScriptEngine.init(ClassLoader.getSystemClassLoader())
        scenario = new UserScenario();
        proc = new PlayerScriptProcessor(scenario);
    }

    def "every step field could be evaluated by template engine"() {
        given:
        scenario.getContext().put("variable", "ya.ru");

        JSONObject event = getSimpleEvent();
        event.put("url", 'http://${variable}');
        when:
        JSONObject result = proc.runStepTemplating(scenario, event);
        then:
        result.get("url") == "http://ya.ru"
    }

    def "every step must have id/eventId to be processable by template engine"() {
        given:
        scenario.getContext().put("variable", "ya.ru");

        JSONObject eventWithId = getSimpleEvent();
        eventWithId.put("url", 'http://${variable}');

        JSONObject eventWithEventId = getSimpleEvent();
        eventWithEventId.remove("id");
        eventWithEventId.put("eventId", "123");
        eventWithEventId.put("url", 'http://${variable}');

        when:
        JSONObject resultId = proc.runStepTemplating(scenario, eventWithId);
        JSONObject resultEventId = proc.runStepTemplating(scenario, eventWithEventId);
        then:
        resultId.get("url") == "http://ya.ru"
        resultEventId.get("url") == "http://ya.ru"
    }

    def "step templates can contains \$"() {
        given:
        JSONObject event = getSimpleEvent();
        event.put("url", 'http://test.com/#strange$id!123');
        when:
        JSONObject result = proc.runStepTemplating(scenario, event);
        then:
        result.get("url") == "http://test.com/#strange\$id!123";
    }

    def "event's fields without \$ must not be processed by template engine"() {
        given:
        scenario.getContext().put("variable", "ya.ru");

        JSONObject eventWithId = getSimpleEvent();
        String field = new String("http://#variable");
        eventWithId.put("url", field);
        eventWithId.put("url2", "http://\$variable")

        when:
        JSONObject resultId = proc.runStepTemplating(scenario, eventWithId);
        then:
        resultId.get("url").is(field)
        resultId.get("url2") == "http://ya.ru"
    }

    JSONObject getSimpleEvent() {
        JSONObject event = new JSONObject();
        event.put("id", new ObjectId());
        return event;
    }
}