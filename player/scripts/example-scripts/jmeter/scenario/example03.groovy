import com.focusit.jsflight.player.constants.EventConstants
import com.focusit.script.jmeter.JMeterJSFlightBridge

boolean accesKeyFound = false;

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


def getRPCRequestClassName(String request) {
    def provider = new Provider(request, ctx);
    def rpcRequest = com.google.gwt.user.server.rpc.RPC.decodeRequest(request, null as java.lang.Class, provider);
    def test = rpcRequest.getParameters()[0];

    def actions = [];

    if (test.getClass().getName().contains("BatchAction")) {
        test.getActions().each({
            actions.add(it.getClass().getSimpleName());
        })
    } else if (test.getClass().getName().contains("Action")) {
        actions.add(test.getClass().getSimpleName());
    }
    if (actions.size() == 0)
        return null;

    return actions.join(" ").trim();
}

java.lang.Thread.currentThread().setContextClassLoader(classloader);

if (sample.getMethod().toLowerCase().equals('post')) {
    def raw = sample.getPropertyAsString('HTTPsampler.Arguments');

    if (raw != null && raw.length() > 0) {

        String name = null;

        try {
            name = getRPCRequestClassName(raw as String)
        } catch (Exception ex) {
            logger.error(ex.toString() + "\n\n" + raw + "\n\n", ex);
        }
        if (name != null) {
            String counter = sample.getName().split(" ")[0].trim();
            sample.setName("" + counter + " " + name);
            logger.debug(Thread.currentThread().getName() + ":" + 'Request ' + sample.getName() + ' renamed to ' + name + ' hash ' + System.identityHashCode(sample));
        } else {
            logger.debug(Thread.currentThread().getName() + ":" + 'Request ' + sample.getName() + ' is not gwt-prc ' + ' hash ' + System.identityHashCode(sample) + '.' + raw);
        }
    }
}



for (String key : sample.getArguments().getArgumentsAsMap().keySet()) {
    if (key.equalsIgnoreCase("accessKey")) {
        accesKeyFound = true;
        break;
    }
}

if (!accesKeyFound) {
    if (JMeterJSFlightBridge.getInstace().getSourceEvent((org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase) sample) != null) {
        def cookies = new org.apache.jmeter.protocol.http.control.CookieManager();
        cookies.setName("HTTP Cookie Manager");
        cookies.setEnabled(true);
        cookies.setProperty(org.apache.jmeter.testelement.TestElement.GUI_CLASS, "CookiePanel");
        cookies.setProperty(org.apache.jmeter.testelement.TestElement.TEST_CLASS, "CookieManager");
        tree.add(cookies);

        String cooks = "employee=" + JMeterJSFlightBridge.getInstace()
                .getSourceEvent((org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase) sample).getString(EventConstants.TAG);

        String pattern = 'employee=(\\w+)\\$(\\d+)';
        def r = java.util.regex.Pattern.compile(pattern);
        def m = r.matcher(cooks);
        if (m.find()) {
            String name = "jsid_" + m.group(1) + "_" + m.group(2);
            cookies.add(new org.apache.jmeter.protocol.http.control.Cookie("JSESSIONID", '${' + name + '}',
                    sample.getPropertyAsString(org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase.DOMAIN), "/", false, 0L));

            if (!vars.getArgumentsAsMap().containsKey(name)) {
                vars.addArgument(new org.apache.jmeter.config.Argument(name, "empty_session"));
            }
        }
    } else {
        logger.info("No tag found for sampler " + sample.getName());
    }
}

def vars_key = 'variables';

if (ctx.getProperty(vars_key) == null) {
    def items = new HashSet();
    items.addAll(ctx.getSources())
    ctx.addProperty(vars_key, items);
}

Set srcs = []
srcs.addAll(ctx.getProperty(vars_key));

srcs.each({
    def template = ctx.getTemplate(it);

    System.out.println('................................. ' + it + ' : ' + template.getClass().getName());

    // should add regex post processor here
    if (template instanceof String) {
        // add just an user defined variable
        vars.addArgument(new org.apache.jmeter.config.Argument(it, template));

        logger.debug('???????????? Added variable ' + it + ' for ' + sample.getName());
        ctx.getProperty(vars_key).remove(it);
    } else if (template instanceof org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase) {
        logger.debug('++++++++++++++ template ' + template.getName() + ' sample ' + sample.getName());
        if (template.equals(sample)) {
            def ree = new org.apache.jmeter.extractor.RegexExtractor();
            ree.setProperty(org.apache.jmeter.testelement.TestElement.GUI_CLASS, "RegexExtractorGui");
            ree.setProperty(org.apache.jmeter.testelement.TestElement.TEST_CLASS, "RegexExtractor");
            ree.setEnabled(true);
            ree.setRefName(it);
            ree.setName('REE.' + it);

            String pattern = '(\\w+)\\.(\\d+)';
            def r = java.util.regex.Pattern.compile(pattern);
            def m = r.matcher(it);
            m.find();
            ree.setRegex(m.group(1) + '\\$' + '\\d+');
            ree.setTemplate('$0$');
            ree.setDefaultValue('ererrer-1212$233232');
            ree.setMatchNumber(0);
            ree.useHeaders();
            tree.add(ree);

            ctx.getProperty(vars_key).remove(it);

            logger.debug('???????????? Added regex extractor to ' + sample.getName())
        }
    } else {
        logger.debug('Source ' + it + ' template ' + template.getName() + ' sample ' + sample.getName());
    }
})

