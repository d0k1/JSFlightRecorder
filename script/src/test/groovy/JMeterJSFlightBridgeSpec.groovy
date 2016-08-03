import org.json.JSONObject
import spock.lang.Specification

/**
 * Created by dkirpichenkov on 06.05.16.
 */

class JMeterJSFlightBridgeSpec extends Specification {

    com.focusit.script.jmeter.JMeterJSFlightBridge bridge = new com.focusit.script.jmeter.JMeterJSFlightBridge();

    def "current step after construcor is literally null"() {
        when:
        bridge;
        then:
        bridge.getCurrentScenarioStep() == bridge.NO_SCENARIO_STEP;
        bridge.isCurrentStepEmpty();
    }

    def "can change current step"() {
        def step = new JSONObject();

        when:
        bridge.setCurrentScenarioStep(step);
        then:
        bridge.getCurrentScenarioStep() == step;
    }

    def "can save current step by given sampler"() {
        def step = new JSONObject();
        def sampler = new Object();
        when:
        bridge.setCurrentScenarioStep(step);
        bridge.addSampler(sampler);
        then:
        bridge.getSourceEvent(sampler) == step;
    }
}