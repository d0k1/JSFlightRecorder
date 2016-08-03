import com.focusit.script.jmeter.JMeterRecorderContext
import spock.lang.Specification

/**
 * Created by dkirpichenkov on 06.05.16.
 */

public class JMeterRecorderContextJMeterRecorderContextSpec extends Specification {
    public static final String PROPERTY = "property"
    String VALUE = "userObject\$123";
    String UDV = "userObject.123";
    JMeterRecorderContext ctx = new JMeterRecorderContext();

    def "can add user defined variable"() {
        when:
        ctx.addTemplate(UDV, VALUE);
        then:
        ctx.getSources().contains(UDV);
        ctx.getTemplate(UDV).equals(VALUE);
    }

    def "reset erases everything"() {
        when:
        ctx.addTemplate("1", "2");
        ctx.addTemplate("3", "4");
        then:
        ctx.getSources().size() == 2;

        when:
        ctx.reset();
        then:
        ctx.getTemplate("1") == null;
        ctx.getSources().size() == 0;
    }

    def "can put and retrieve any property"() {
        when:
        ctx.addProperty(PROPERTY, VALUE);
        then:
        ctx.getProperty(PROPERTY) == VALUE;
        ctx.getProperty(VALUE) == null;
    }
}