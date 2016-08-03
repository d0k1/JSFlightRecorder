import com.google.gwt.user.server.rpc.RPC

java.lang.Thread.currentThread().setContextClassLoader(classloader);

class Provider implements com.google.gwt.user.server.rpc.SerializationPolicyProvider {
    com.google.gwt.user.server.rpc.SerializationPolicy name;
    private com.focusit.script.jmeter.JMeterRecorderContext ctx;

    Provider(request, ctx) {
        this.ctx = ctx;
        this.name = getPolicy(request as String);
    }

    com.google.gwt.user.server.rpc.SerializationPolicy getSerializationPolicy(String moduleBaseURL, String serializationPolicyStrongName) {
        return name;
    }

    def getPolicy(String request) {
        def patternString = ".*/(\\w+)[;/].*?\\|(\\w{32})\\|";
        def r = java.util.regex.Pattern.compile(patternString);
        def m = r.matcher(request);

        String module = null;
        String name = null;

        if (m.find()) {
            module = m.group(1);
            name = m.group(2);
        }

        if (module == null || name == null) {
            return null;
        }

        String policyPath = System.getProperty('policy');

        if (policyPath == null) {
            return null;
        }
        policyPath = policyPath + java.io.File.separatorChar + module + java.io.File.separatorChar + name + '.gwt.rpc';

        def policy;

        if (ctx.getProperty(policyPath) != null) {
            return ctx.getProperty(policyPath);
        }

        def fis = null;
        try {
            fis = new java.io.FileInputStream(new java.io.File(policyPath));
            policy = com.google.gwt.user.server.rpc.SerializationPolicyLoader.loadFromStream(fis);
            ctx.addProperty(policyPath, policy);

            return policy;
        } catch (Exception e) {
            System.err.println(e.toString());
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
}

def isItEditAction(String body) {
    return body.contains('EditObjectAction') || body.contains('EditObjectsAction');
}

def isItDeleteAction(String body) {
    return body.contains('DeleteObjectAction') || body.contains('DeleteObjectsAction');
}

def processBatch(def batch, def clazz) {
    batch.getActions().each({
        if (it.getClass().getName().contains(clazz)) {
            return it;
        }
    })

    return null;
}

def getEditObjectActionResult(String request) {
    def provider = new Provider(request, ctx);
    def rpcRequest = com.google.gwt.user.server.rpc.RPC.decodeRequest(request, null as java.lang.Class, provider);

    def result = [];

    def test = rpcRequest.getParameters()[0];
    if (test.getClass().getName().contains("BatchAction")) {
        test = processBatch(test, "EditObjectAction");
        if (test == null) {
            test = processBatch(test, "EditObjectsAction");
        }
    }

    if (test.getClass().getName().contains("EditObjectAction")) {
        result << test.getUuid();
    }

    if (test.getClass().getName().contains("EditObjectsAction")) {
        result.addAll(test.getUuids() as Set);
    }

    return result;
}

def getDeleteObjectActionResult(String request) {
    def provider = new Provider(request, ctx);
    def rpcRequest = com.google.gwt.user.server.rpc.RPC.decodeRequest(request, null as java.lang.Class, provider);

    def result = [];

    def test = rpcRequest.getParameters()[0];
    if (test.getClass().getName().contains("BatchAction")) {
        test = processBatch(test, "DeleteObjectAction");
        if (test == null) {
            test = processBatch(test, "DeleteObjectsAction");
        }
    }

    if (test.getClass().getName().contains("DeleteObjectAction")) {
        result << test.getUUID();
    }

    if (test.getClass().getName().contains("DeleteObjectsAction")) {
        result.addAll(test.getUuids() as Set);
    }

    return result;
}

def baseurl = System.getProperty("baseurl");

if (baseurl != null && baseurl.trim().length() > 0 && !request.getUrl().toString().contains(baseurl)) {
    logger.info("Skipped " + request.getUrl() + " Response code " + response.getResponseCode() + ' hash ' + System.identityHashCode(request));
    return false;
}

if (request.getUrl().toString().contains('/remote_logging') || request.getUrl().toString().contains('/comet')) {
    logger.info("Skipped " + request.getUrl() + " Response code " + response.getResponseCode() + ' hash ' + System.identityHashCode(request));
    return false;
}

def makeTemplateInRequest(def request) {
    def body = request.getQueryString();
    if (body != null && body.length() > 0) {
        body = URLDecoder.decode(body as String, "UTF-8");
    }

    if (request.getMethod().toLowerCase().equals('post')) {
        body = request.getPropertyAsString('HTTPsampler.Arguments');
    }

    // comet can't be used at the moment
    if (body.contains('WaitCometEventsAction')) {
        return null;
    }

    if ((isItEditAction(body) || isItDeleteAction(body))) {
        logger.info('Edit Or Delete');

        def templates = [];
        if (isItEditAction(body)) {
            templates.addAll(getEditObjectActionResult(body))
        } else {
            templates.addAll(getDeleteObjectActionResult(body));
        }

        templates.each({ template ->
            def src = template.replace('$', '.');

            if (ctx.getTemplate(src) instanceof String || ctx.getTemplate(src) == null) {
                def tt = new String(template);
                tt = 'clone.' + tt;
                if (ctx.getTemplate(src) == null || !ctx.getTemplate(src).equals(tt)) {
                    logger.debug('^^^^^^^^^^^^^^^^^^^^^^^^^ 2. ' + src + ' - ' + tt);
                    ctx.addTemplate(src, tt);
                }
            }
            body = body.replace(template, '${' + src + '}');
        })
    }

    String pattern = '(\\w+)\\$(\\d+)';
    def r = java.util.regex.Pattern.compile(pattern);
    def m = r.matcher(body);

    while (m.find()) {
        def template = m.group(1) + '$' + m.group(2);
        def src = m.group(1) + "." + m.group(2);

        if (ctx.getTemplate(src) == null) {
            // if request has a link to an object. jmeter recorder should add an user-defined-variable with partOne.partTwo equals to partOne$partTwo
            def tt = new String(template);

            logger.debug('**************************** 1. ' + src + ' - ' + tt);
            ctx.addTemplate(src, tt);
        }

        body = body.replace(template, '${' + src + '}');
        m = r.matcher(body);
    }

    if (request.getMethod().toLowerCase().equals('post')) {
        def arg = request.getArguments().getArgument(0);
        if (arg != null) {
            arg.setValue(body);
        }
    }
    if (request.getMethod().toLowerCase().equals('get')) {
        request.getArguments().clear();
        request.parseArguments(body);
        //request.setUrl(body);
    }

    if (request.getMethod().toLowerCase().equals('get')) {
        logger.debug(Thread.currentThread().getName() + ":" + '----------------' + request.getName() + '-' + request.getMethod().toLowerCase() + '-request-' + request.getQueryString());
    } else {
        logger.debug(Thread.currentThread().getName() + ":" + '----------------' + request.getName() + '-' + request.getMethod().toLowerCase() + '-request-' + request.getPropertyAsString('HTTPsampler.Arguments'));
    }

    return body;
}

logger.info(Thread.currentThread().getName() + ":" + "Proceeding with hash " + System.identityHashCode(request));

def body = '';

try {
    // request processing
    body = makeTemplateInRequest(request);

    if (body == null) {
        return false;
    }

} catch (Throwable e) {
    logger.error(e.toString(), e);
    //return false;
}

// response processing
// probably it might be better to parse response only in some conditions
def res = new String(response.getResponseData(), "UTF-8");

if (request.getMethod().toLowerCase().equals('post')) {
    logger.debug(Thread.currentThread().getName() + ":" + '-----------------response:' + res);
}

def rr = java.util.regex.Pattern.compile('AddObjectAction\\/\\d+\\|.*Fqn\\/\\d+\\|(\\w+)(\\$\\w+)?\\|');
def mm = rr.matcher(body);

if (mm.find()) {
    r = java.util.regex.Pattern.compile('(' + mm.group(1) + ')\\$(\\d+)');
    logger.debug('!!!!!!!!!!!!!!!!!!!!!!!!! AddObjectAction');
    logger.debug('!!!!!!!!!!!!!!!!!!!!!!!!! regex:' + r.toString());
    logger.debug('!!!!!!!!!!!!!!!!!!!!!!!!! res:' + res);
    m = r.matcher(res);

    if (m.find()) {
        def template = m.group(1) + '$' + m.group(2);
        def src = m.group(1) + "." + m.group(2);
        if (ctx.getTemplate(src) == null) {
            // if response has a link to an object jmeter recorder should add an user-defined-variable with partOne.partTwo equals current request
            ctx.addTemplate(src, request);
            logger.debug('!!!!!!!!!!!!!!!!!!!!!!!!! register ree ' + src + ' - ' + request.getName());
        } else {
            logger.debug('=============================== Already has ' + src + ' variable!!');
        }
        res = res.replace(template, '${' + src + '}');
    }
}

logger.info(Thread.currentThread().getName() + ":" + "Sample passed " + request.getName() + " Response code " + response.getResponseCode() + ' hash ' + System.identityHashCode(request));

return true;