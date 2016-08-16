package com.focusit.jmeter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.RecordingController;
import org.apache.jmeter.protocol.http.proxy.JMeterProxyControl;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestElementTraverser;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;

import com.focusit.script.ScriptsClassLoader;
import com.focusit.script.jmeter.JMeterJSFlightBridge;
import com.focusit.script.jmeter.JMeterRecorderContext;

/**
 * Interface to control jmeter proxy recorder
 *
 * @author Denis V. Kirpichenkov
 */
public class JMeterRecorder
{
    private static final String DEFAULT_TEMPLATE_NAME = "template.jmx";

    private HashTree hashTree;
    private JMeterProxyControl ctrl;
    private RecordingController recCtrl = null;
    private HashTree recCtrlPlace = null;
    private Arguments vars = null;

    private HashTree varsPlace = null;
    private String currentTemplate = DEFAULT_TEMPLATE_NAME;

    private JMeterJSFlightBridge bridge = new JMeterJSFlightBridge();

    private JMeterRecorderContext context;

    private JMeterScriptProcessor scriptProcessor;

    public JMeterRecorder(ScriptsClassLoader classLoader)
    {
        this.context = new JMeterRecorderContext();
        this.scriptProcessor = new JMeterScriptProcessor(this, classLoader);
    }

    public JMeterRecorder(JMeterRecorderContext context, JMeterScriptProcessor scriptProcessor)
    {
        this.context = context;
        this.scriptProcessor = scriptProcessor;
    }

    public JMeterScriptProcessor getScriptProcessor()
    {
        return scriptProcessor;
    }

    public void init() throws Exception
    {
        init(DEFAULT_TEMPLATE_NAME);
    }

    public void init(String pathToTemplate) throws Exception
    {
        this.currentTemplate = pathToTemplate;
        JMeterUtils.setJMeterHome(new File("").getAbsolutePath());
        JMeterUtils.loadJMeterProperties(new File("jmeter.properties").getAbsolutePath());
        JMeterUtils.setProperty("saveservice_properties", File.separator + "saveservice.properties");
        JMeterUtils.setProperty("user_properties", File.separator + "user.properties");
        JMeterUtils.setProperty("upgrade_properties", File.separator + "upgrade.properties");
        JMeterUtils.setProperty("system_properties", File.separator + "system.properties");
        JMeterUtils.setLocale(Locale.ENGLISH);

        JMeterUtils.setProperty("proxy.cert.directory", new File("").getAbsolutePath());
        hashTree = SaveService.loadTree(new File(pathToTemplate));
        ctrl = new JMeterProxyControl(this);

        hashTree.traverse(new HashTreeTraverser()
        {

            @Override
            public void addNode(Object node, HashTree subTree)
            {
                System.out.println("Node: " + node.toString());

                if (node instanceof Arguments)
                {
                    if (((Arguments)node).getName().equalsIgnoreCase("UDV"))
                    {
                        if (varsPlace == null)
                        {
                            varsPlace = subTree;
                            vars = (Arguments)node;
                        }
                    }
                }

                if (node instanceof RecordingController)
                {
                    if (recCtrl == null)
                    {
                        recCtrl = (RecordingController)node;
                        recCtrlPlace = subTree;
                    }
                }
            }

            @Override
            public void processPath()
            {
            }

            @Override
            public void subtractNode()
            {
            }
        });

        ctrl.setTargetTestElement(recCtrl);
    }

    public void saveScenario(OutputStream outStream) throws IOException
    {
        TestElement sample1 = recCtrl.next();

        List<TestElement> samples = new ArrayList<>();

        while (sample1 != null)
        {
            // skip unknown nasty requests
            if (sample1 instanceof HTTPSamplerBase)
            {
                HTTPSamplerBase http = (HTTPSamplerBase)sample1;
                if (http.getArguments().getArgumentCount() > 0
                        && http.getArguments().getArgument(0).getValue().startsWith("0Q0O0M0K0I0"))
                {
                    sample1 = recCtrl.next();
                    continue;
                }
            }
            samples.add(sample1);
            sample1 = recCtrl.next();
        }

        Collections.sort(samples, (o1, o2) -> {
            String num1 = o1.getName().split(" ")[0];
            String num2 = o2.getName().split(" ")[0];
            return ((Integer)Integer.parseInt(num1)).compareTo((Integer)Integer.parseInt(num2));
        });

        for (TestElement sample : samples)
        {
            final List<TestElement> childs = new ArrayList<>();
            final List<JMeterProperty> keys = new ArrayList<>();
            sample.traverse(new TestElementTraverser()
            {

                @Override
                public void endProperty(JMeterProperty key)
                {
                    if (key.getObjectValue() instanceof HeaderManager)
                    {
                        childs.add((HeaderManager)key.getObjectValue());
                        keys.add(key);
                    }
                }

                @Override
                public void endTestElement(TestElement el)
                {
                }

                @Override
                public void startProperty(JMeterProperty key)
                {
                }

                @Override
                public void startTestElement(TestElement el)
                {
                }
            });

            for (JMeterProperty key : keys)
            {
                sample.removeProperty(key.getName());
            }

            HashTree parent = recCtrlPlace.add(sample);

            for (TestElement child : childs)
            {
                parent.add(child);
            }

            // TODO Groovy script should decide whether add cookie manager or not
            if (sample instanceof HTTPSamplerBase)
            {
                HTTPSamplerBase http = (HTTPSamplerBase)sample;

                scriptProcessor.processScenario(http, parent, vars);
            }
        }
        SaveService.saveTree(hashTree, outStream);
    }

    public void saveScenario(String filename) throws IOException
    {
        saveScenario(new FileOutputStream(new File(filename)));
    }

    public void startRecording() throws IOException
    {
        context.reset();
        ctrl.startProxy();
    }

    public void setProxyPort(int proxyPort) throws IOException
    {
        ctrl.setPort(proxyPort);
    }

    public void stopRecording()
    {
        ctrl.stopProxy();
    }

    public void reset() throws IOException
    {
        hashTree = SaveService.loadTree(new File(currentTemplate));

        hashTree.traverse(new HashTreeTraverser()
        {

            @Override
            public void addNode(Object node, HashTree subTree)
            {
                System.out.println("Node: " + node.toString());

                if (node instanceof Arguments)
                {
                    if (((Arguments)node).getName().equalsIgnoreCase("UDV"))
                    {
                        if (varsPlace == null)
                        {
                            varsPlace = subTree;
                            vars = (Arguments)node;
                        }
                    }
                }

                if (node instanceof RecordingController)
                {
                    if (recCtrl == null)
                    {
                        recCtrl = (RecordingController)node;
                        recCtrlPlace = subTree;
                    }
                }
            }

            @Override
            public void processPath()
            {
            }

            @Override
            public void subtractNode()
            {
            }
        });

        ctrl.setTargetTestElement(recCtrl);
    }

    public JMeterRecorderContext getContext()
    {
        return context;
    }

    public void setContext(JMeterRecorderContext context)
    {
        this.context = context;
    }

    public JMeterJSFlightBridge getBridge()
    {
        return bridge;
    }

    public void setBridge(JMeterJSFlightBridge bridge)
    {
        this.bridge = bridge;
    }
}
