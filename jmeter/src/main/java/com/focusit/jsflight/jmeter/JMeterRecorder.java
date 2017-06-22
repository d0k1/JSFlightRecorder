package com.focusit.jsflight.jmeter;

import com.focusit.jsflight.script.jmeter.JMeterRecorderContext;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.control.RecordingController;
import org.apache.jmeter.protocol.http.control.gui.RecordController;
import org.apache.jmeter.protocol.http.proxy.JMeterProxyControl;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.HashTreeTraverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Interface to control jmeter proxy recorder
 *
 * @author Denis V. Kirpichenkov
 */
public class JMeterRecorder
{
    public static final Logger LOG = LoggerFactory.getLogger(JMeterRecorder.class);
    public static final String DEFAULT_TEMPLATE_PATH = "template.jmx";
    private HashTree mainHashTreeTemplate;
    private JMeterProxyControl jMeterProxyControl;

    private List<RecordingController> recordingControllers;

    private HashTree transactionControllerSubTree;
    private Arguments vars = null;

    private String pathToTemplate = DEFAULT_TEMPLATE_PATH;

    private JMeterRecorderContext context;

    private JMeterScriptProcessor scriptProcessor;

    public JMeterRecorder()
    {
        initializeJMeter();

        context = new JMeterRecorderContext();
        scriptProcessor = new JMeterScriptProcessor();
        recordingControllers = new LinkedList<>();
    }

    public JMeterRecorder(String pathToTemplate)
    {
        this();
        this.pathToTemplate = pathToTemplate;
    }

    private static void initializeJMeter()
    {
        JMeterUtils.setJMeterHome(new File("").getAbsolutePath());
        JMeterUtils.loadJMeterProperties(new File("jmeter.properties").getAbsolutePath());
        JMeterUtils.setProperty("saveservice_properties", File.separator + "saveservice.properties");
        JMeterUtils.setProperty("user_properties", File.separator + "user.properties");
        JMeterUtils.setProperty("upgrade_properties", File.separator + "upgrade.properties");
        JMeterUtils.setProperty("system_properties", File.separator + "system.properties");
        JMeterUtils.setLocale(Locale.ENGLISH);
        JMeterUtils.setProperty("proxy.cert.directory", new File("").getAbsolutePath());
    }

    public JMeterScriptProcessor getScriptProcessor()
    {
        return scriptProcessor;
    }

    public void initialize(Long maxRequestsPerScenario) throws Exception
    {
        jMeterProxyControl = new JMeterProxyControl(this, maxRequestsPerScenario);
        reset();
    }

    public int getRecordingsCount()
    {
        return recordingControllers.size();
    }

    public void saveScenario(OutputStream outStream, int recordingIndex) throws IOException
    {
        LOG.info("Start {} scenario saving", recordingIndex);
        LOG.info("Cloning template tree");
        HashTree hashTree = (HashTree)mainHashTreeTemplate.clone();
        LOG.info("Searching for main nodes and trees");
        findMainNodesAndTrees(hashTree);

        RecordingController recordingController = recordingControllers.get(recordingIndex);
        HashTree recordingControllerSubTree = transactionControllerSubTree.add(recordingController);

        LOG.info("Extracting test elements");
        List<TestElement> samples = extractAppropriateTestElements(recordingController);

        LOG.info("Placing test elements");
        placeAndProcessTestElements(recordingControllerSubTree, samples);
        LOG.info("Saving into out stream");
        SaveService.saveTree(hashTree, outStream);
    }

    private void placeAndProcessTestElements(HashTree hashTree, List<TestElement> samples)
    {
        for (TestElement element : samples)
        {
            List<TestElement> descendants = findAndRemoveHeaderManagers(element);
            HashTree parent = hashTree.add(element);
            descendants.forEach(parent::add);

            if (element instanceof HTTPSamplerBase)
            {
                HTTPSamplerBase http = (HTTPSamplerBase)element;

                LOG.info("Start sampler processing");
                scriptProcessor.processScenario(http, parent, vars, this);
                LOG.info("Stop sampler processing");
            }
        }
    }

    private List<TestElement> findAndRemoveHeaderManagers(TestElement element) {
        List<TestElement> descendants = new ArrayList<>();

        for (PropertyIterator iter = element.propertyIterator(); iter.hasNext();)
        {
            JMeterProperty property = iter.next();
            if (property.getObjectValue() instanceof HeaderManager)
            {
                descendants.add((HeaderManager)property.getObjectValue());
                iter.remove();
            }
        }
        return descendants;
    }

    private List<TestElement> extractAppropriateTestElements(RecordingController recordingController)
    {
        List<TestElement> samples = new ArrayList<>();

        for (TestElement sample; (sample = recordingController.next()) != null;)
        {
            // skip unknown nasty requests
            if (sample instanceof HTTPSamplerBase)
            {
                HTTPSamplerBase http = (HTTPSamplerBase)sample;
                if (http.getArguments().getArgumentCount() > 0
                        && http.getArguments().getArgument(0).getValue().startsWith("0Q0O0M0K0I0"))
                {
                    continue;
                }
            }
            samples.add(sample);
        }

        Collections.sort(samples, (o1, o2) -> {
            String num1 = o1.getName().split(" ")[0];
            String num2 = o2.getName().split(" ")[0];
            return ((Integer)Integer.parseInt(num1)).compareTo(Integer.parseInt(num2));
        });
        return samples;
    }

    private void findMainNodesAndTrees(HashTree hashTree)
    {
        hashTree.traverse(new HashTreeTraverser()
        {
            @Override
            public void addNode(Object node, HashTree subTree)
            {
                System.out.println("Node: " + node.toString());

                if (node instanceof Arguments && ((Arguments)node).getName().equalsIgnoreCase("UDV"))
                {
                    vars = (Arguments)node;
                }

                if (node instanceof TransactionController)
                {
                    transactionControllerSubTree = subTree;
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
    }

    public void saveScenarios(String filename) throws IOException
    {
        for (int i = 0; i < recordingControllers.size(); i++)
        {
            saveScenario(new FileOutputStream(new File(i + '_' + filename)), i);
        }
    }

    public void startRecording() throws IOException
    {
        context.reset();
        jMeterProxyControl.startProxy();
    }

    public void setProxyPort(int proxyPort) throws IOException
    {
        jMeterProxyControl.setPort(proxyPort);
    }

    public void stopRecording()
    {
        jMeterProxyControl.stopProxy();
    }

    public void reset() throws IOException
    {
        mainHashTreeTemplate = SaveService.loadTree(new File(pathToTemplate));

        splitScenario();
    }

    public JMeterRecorderContext getContext()
    {
        return context;
    }

    public void setContext(JMeterRecorderContext context)
    {
        this.context = context;
    }

    public void splitScenario()
    {
        RecordingController recordingController = new RecordingController();
        recordingController.setProperty(TestElement.TEST_CLASS, RecordingController.class.getName());
        recordingController.setProperty(TestElement.GUI_CLASS, RecordController.class.getName());
        recordingController.setName("Recording controller");
        recordingControllers.add(recordingController);

        jMeterProxyControl.setTargetTestElement(recordingController);
    }
}
