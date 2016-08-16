import com.esotericsoftware.kryo.Kryo
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'httpclient', version = '4.5.2'),
        @Grab(group = 'com.esotericsoftware', module = 'kryo', version = '4.0.0')
])
import com.esotericsoftware.kryo.io.FastInput
import com.esotericsoftware.kryo.serializers.MapSerializer
import com.focusit.jsflight.recorder.internalevent.InternalEventRecorder
import com.focusit.jsflight.recorder.internalevent.httprequest.HttpRecordInformation

class RestoredRequest {
    Map<String, String[]> parameters;
    Long contentLength;
    String contentType;
    String uri;
    String method;
    String contextPath;
    HashMap<String, String> cookies;
    byte[] payload;
    HashMap additional;

    @Override
    public String toString() {
        return "RestoredRequest{" +
                "parameters=" + parameters +
                ", contentLength=" + contentLength +
                ", contentType='" + contentType + '\'' +
                ", uri='" + uri + '\'' +
                ", method='" + method + '\'' +
                ", contextPath='" + contextPath + '\'' +
                ", cookies=" + cookies +
                ", payload=" + Arrays.toString(payload) +
                ", additional=" + additional +
                '}';
    }
}

def requests = [];

FastInput input = new FastInput(new FileInputStream(args[0]))

Kryo kryo = new Kryo();
kryo.register(HashMap.class, new MapSerializer());

while (input.available() > 0) {
    InternalEventRecorder.InternalEventRecord record = kryo.readObject(input, InternalEventRecorder.InternalEventRecord.class);
    HttpRecordInformation information = record.data;

    String tag = new String(record.tag);

    if (tag.equalsIgnoreCase(HttpRecorder.HTTP_RECORDER_TAG)) {
        RestoredRequest request = new RestoredRequest();

        if (information.params != null && information.params.length > 0) {
            FastInput paramsInput = new FastInput(new ByteArrayInputStream(information.params));

            request.parameters = kryo.readObject(paramsInput, HashMap.class);
            request.contentLength = kryo.readObjectOrNull(paramsInput, Long.class);
            request.contentType = kryo.readObjectOrNull(paramsInput, String.class);
            request.uri = kryo.readObjectOrNull(paramsInput, String.class);
            request.method = kryo.readObjectOrNull(paramsInput, String.class);
            request.contextPath = kryo.readObjectOrNull(paramsInput, String.class);
            request.cookies = kryo.readObject(paramsInput, HashMap.class);
        }
        if (information.payload != null) {
            request.payload = information.payload;
        }
        request.additional = information.additional;
        requests << request;

        requests.each({
            println it;
        });
    }
}
