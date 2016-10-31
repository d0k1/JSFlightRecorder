package com.focusit.jsflight.player.ui;

import com.focusit.jsflight.player.constants.BrowserType;
import com.focusit.jsflight.player.fileconfigholder.*;
import com.focusit.jsflight.player.scenario.UserScenario;
import com.focusit.jsflight.player.input.FileInput;
import com.focusit.jsflight.player.scenario.ScenarioProcessor;
import com.focusit.jsflight.jmeter.JMeterRecorder;
import com.focusit.jsflight.player.webdriver.SeleniumDriver;
import com.focusit.jsflight.script.ScriptEngine;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.apache.commons.io.FileUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rsyntaxtextarea.folding.JsonFoldParser;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class MainFrame
{

    private static final Logger LOG = LoggerFactory.getLogger(MainFrame.class);
    private final ButtonGroup buttonGroup = new ButtonGroup();
    private UserScenario scenario = new UserScenario();
    private AbstractTableModel model;
    private JFrame frmJsflightrecorderPlayer;
    private JTextField filenameField;
    private JTable table;
    private JTextArea contentPane;
    private RSyntaxTextArea eventContent;
    private JLabel statisticsLabel;
    private JTextField proxyHost;
    private JTextField proxyPort;
    private JTextField ffPath;
    private JTextField scriptFilename;
    private JTextField pageReadyTimeoutField;
    private RSyntaxTextArea scriptArea;
    private JRadioButton usePhantomButton;
    private JRadioButton useFirefoxButton;
    private JCheckBox makeShots;
    private JTextField screenDirTextField;
    private JTextField scenarioTextField;
    private JTextField pjsPath;
    private JMeterRecorder jmeter;
    private JTextField lookupFilename;
    private JTextField isAsyncRequestsCompleted;

    private JTextField webDriverTag;
    private RSyntaxTextArea lookupScriptArea;
    private RSyntaxTextArea stepProcessScript;
    private RSyntaxTextArea scenarioProcessScript;
    private RSyntaxTextArea duplicatesScriptArea;
    private JTextField duplicatesFilePath;
    private JCheckBox useRandomCharsBox;
    private JTextField scriptEventHandlerFilePath;

    private RSyntaxTextArea scriptEventHandlerScriptArea;
    private JTextField firefoxDsiplay;
    private JTextField formDialogXpathField;

    private SeleniumDriver seleniumDriver = new SeleniumDriver(scenario);

    /**
     * Create the application.
     *
     * @throws IOException
     */
    public MainFrame() throws Exception
    {
        initialize();

        ScriptEngine.init(scenario.getConfiguration().getCommonConfiguration().getScriptClassloader());
        jmeter = new JMeterRecorder();
        jmeter.init();
    }

    public AbstractTableModel createEventTableModel()
    {
        return new AbstractTableModel()
        {

            private static final long serialVersionUID = 1L;

            private String[] columns = { "*", "#", "eventId", "url", "type", "key", "target", "timestamp", "tag",
                    "Pre", "Post", "comment" };

            @Override
            public int getColumnCount()
            {
                return 11;
            }

            @Override
            public String getColumnName(int column)
            {
                return columns[column];
            }

            @Override
            public int getRowCount()
            {
                return scenario.getStepsCount();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex)
            {
                if (rowIndex == scenario.getPosition() && columnIndex == 0)
                {
                    return "*";
                }

                JSONObject event = scenario.getStepAt(rowIndex);

                switch (columnIndex)
                {
                case 1:
                    return rowIndex;
                case 2:
                    return event.get("eventId");
                case 3:
                    return event.has("hash") ? event.get("hash") : "";
                case 4:
                    String type = event.getString("type");
                    if ("mousedown".equals(type))
                    {
                        return "md";
                    }
                    if ("keypress".equals(type))
                    {
                        return "kp";
                    }
                    if ("keyup".equals(type))
                    {
                        return "ku";
                    }
                    if ("hashchange".equals(type))
                    {
                        return "hc";
                    }

                    return type;
                case 5:
                {
                    if (!event.has("charCode"))
                    {
                        return null;
                    }
                    int code = event.getInt("charCode");
                    char[] key = new char[1];
                    key[0] = (char)code;
                    return String.format("%d(%s)/%s", code, new String(key), event.has("button") ? event.get("button")
                            : "null");
                }
                case 6:
                    return scenario.getTargetForEvent(event);
                case 7:
                    return new Date(event.getBigDecimal("timestamp").longValue());
                case 8:
                    return getTagForEvent(event);
                case 9:
                    return event.has("pre") ? event.getString("pre") : "no";
                case 10:
                    return event.has("post") ? event.getString("post") : "no";
                default:
                    return null;
                }
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if (columnIndex == 6 || columnIndex == 9 || columnIndex == 10)
                {
                    return true;
                }
                return false;
            }
        };
    }

    public JFrame getFrame()
    {
        return frmJsflightrecorderPlayer;
    }

    public String getTagForEvent(JSONObject event)
    {
        String tag = "null";
        if (event.has(webDriverTag.getText()))
        {
            tag = event.getString(webDriverTag.getText());
        }

        return tag;
    }

    public void setColumnWidths()
    {
        table.getColumnModel().getColumn(0).setMaxWidth(10);
        table.getColumnModel().getColumn(1).setMaxWidth(100);
        table.getColumnModel().getColumn(2).setMaxWidth(100);
        table.getColumnModel().getColumn(4).setMaxWidth(1000);
        table.getColumnModel().getColumn(5).setMaxWidth(60);
        table.getColumnModel().getColumn(6).setPreferredWidth(400);
        table.getColumnModel().getColumn(7).setPreferredWidth(180);
        table.getColumnModel().getColumn(7).setMaxWidth(180);
        table.getColumnModel().getColumn(8).setPreferredWidth(140);
        table.getColumnModel().getColumn(8).setMaxWidth(140);
    }

    protected void copyCurrentStep()
    {
        scenario.copyStep(table.getSelectedRow());
        model.fireTableRowsInserted(table.getSelectedRow(), table.getSelectedRow());
    }

    protected void createTableEditor()
    {
        final DefaultCellEditor editor = new DefaultCellEditor(new JTextField());
        editor.addCellEditorListener(new CellEditorListener()
        {
            @Override
            public void editingCanceled(ChangeEvent e)
            {
                ((JTextField)editor.getComponent()).getText();
            }

            @Override
            public void editingStopped(ChangeEvent e)
            {
            }
        });
        table.getColumnModel().getColumn(6).setCellEditor(editor);

        table.getColumnModel().getColumn(9).setCellEditor(new StepScriptEditorDialog(scenario, true));
        table.getColumnModel().getColumn(10).setCellEditor(new StepScriptEditorDialog(scenario, false));
    }

    protected void playTheScenario()
    {
        saveControlersOptions();
        new ScenarioProcessor().play(scenario, seleniumDriver);
        model.fireTableDataChanged();
    }

    protected void resetCurrentEvent()
    {
        int index = table.getSelectedRow();
        if (index >= 0)
        {
            eventContent.setText(scenario.getStepAt(index).toString(3));
            eventContent.setCaretPosition(0);
        }
    }

    protected void saveControlersOptions()
    {
        try
        {
            updateInputController();
            updatePostProcessController();
            updateWebLookupController();
            updateOptionsController();
            updateJMeterController();
            updateDuplicateHandlerController();
            updateScriptEventHandlerController();

            //TODO Store all configurations and/or add necessary configHolders
            InputFileConfigHolder.getInstance().store(IFileConfigHolder.defaultConfig);
            CommonFileConfigHolder.getInstance().store(IFileConfigHolder.defaultConfig);
            PostProcessFileConfigHolder.getInstance().store(IFileConfigHolder.defaultConfig);
        }
        catch (Exception e)
        {
            LOG.error(e.toString(), e);
        }
    }

    protected void updateCurrentEvent()
    {
        scenario.updateStep(table.getSelectedRow(), new JSONObject(eventContent.getText()));
        model.fireTableRowsUpdated(table.getSelectedRow(), table.getSelectedRow());
    }

    private void checkElement(int position)
    {
        scenario.checkStep(position);
        model.fireTableDataChanged();
    }

    private void configureScriptTextArea(RSyntaxTextArea eventContent, RTextScrollPane scrollPane_2, String syntaxStyle)
    {
        eventContent.setSyntaxEditingStyle(syntaxStyle);
        eventContent.getFoldManager().setCodeFoldingEnabled(true);
        eventContent.setFont(new Font("Hack", Font.PLAIN, 16));
        eventContent.setRows(3);
        eventContent.setMarkOccurrences(true);
        eventContent.setLineWrap(true);
        eventContent.setWrapStyleWord(true);

        scrollPane_2.setLineNumbersEnabled(true);
        scrollPane_2.setFoldIndicatorEnabled(true);
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize()
    {
        frmJsflightrecorderPlayer = new JFrame();
        frmJsflightrecorderPlayer.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                saveControlersOptions();
            }

            @Override
            public void windowDeactivated(WindowEvent e)
            {
                saveControlersOptions();
            }
        });
        frmJsflightrecorderPlayer.setTitle("JSFlight player");
        frmJsflightrecorderPlayer.setBounds(100, 100, 1110, 850);
        frmJsflightrecorderPlayer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
        frmJsflightrecorderPlayer.getContentPane().add(tabbedPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        tabbedPane.addTab("Input", null, inputPanel, null);
        GridBagLayout gbl_inputPanel = new GridBagLayout();
        gbl_inputPanel.columnWidths = new int[] { 0, 0, 0 };
        gbl_inputPanel.rowHeights = new int[] { 0, 0, 0, 0 };
        gbl_inputPanel.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
        gbl_inputPanel.rowWeights = new double[] { 0.0, 0.0, 1.0, Double.MIN_VALUE };
        inputPanel.setLayout(gbl_inputPanel);

        JLabel lblFilename = new JLabel("Filename");
        GridBagConstraints gbc_lblFilename = new GridBagConstraints();
        gbc_lblFilename.insets = new Insets(0, 0, 5, 5);
        gbc_lblFilename.anchor = GridBagConstraints.NORTHEAST;
        gbc_lblFilename.gridx = 0;
        gbc_lblFilename.gridy = 0;
        inputPanel.add(lblFilename, gbc_lblFilename);

        filenameField = new JTextField();
        GridBagConstraints gbc_filenameField = new GridBagConstraints();
        gbc_filenameField.insets = new Insets(0, 0, 5, 0);
        gbc_filenameField.fill = GridBagConstraints.HORIZONTAL;
        gbc_filenameField.gridx = 1;
        gbc_filenameField.gridy = 0;
        inputPanel.add(filenameField, gbc_filenameField);
        filenameField.setColumns(10);

        JButton btnLoad = new JButton("Load");
        btnLoad.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    loadScenario();
                }
                catch (IOException e1)
                {
                    throw new IllegalArgumentException(e1);
                }
            }
        });
        GridBagConstraints gbc_btnLoad = new GridBagConstraints();
        gbc_btnLoad.insets = new Insets(0, 0, 5, 5);
        gbc_btnLoad.gridx = 0;
        gbc_btnLoad.gridy = 1;
        inputPanel.add(btnLoad, gbc_btnLoad);

        JButton btnBrowse = new JButton("Browse");
        btnBrowse.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION)
                {
                    String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
                    filenameField.setText(selectedFile);
                }
            }
        });
        GridBagConstraints gbc_btnBrowse = new GridBagConstraints();
        gbc_btnBrowse.insets = new Insets(0, 0, 5, 0);
        gbc_btnBrowse.gridx = 1;
        gbc_btnBrowse.gridy = 1;
        inputPanel.add(btnBrowse, gbc_btnBrowse);

        JLabel lblContent = new JLabel("Content");
        GridBagConstraints gbc_lblContent = new GridBagConstraints();
        gbc_lblContent.anchor = GridBagConstraints.NORTH;
        gbc_lblContent.insets = new Insets(0, 0, 0, 5);
        gbc_lblContent.gridx = 0;
        gbc_lblContent.gridy = 2;
        inputPanel.add(lblContent, gbc_lblContent);

        JScrollPane scrollPane_1 = new JScrollPane();
        GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
        gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
        gbc_scrollPane_1.gridx = 1;
        gbc_scrollPane_1.gridy = 2;
        inputPanel.add(scrollPane_1, gbc_scrollPane_1);

        contentPane = new JTextArea();
        scrollPane_1.setViewportView(contentPane);

        JPanel scenarioPanel = new JPanel();
        tabbedPane.addTab("Scenario", null, scenarioPanel, null);
        scenarioPanel.setLayout(new BorderLayout(0, 0));

        JPanel panel_4 = new JPanel();
        panel_4.setBorder(null);
        panel_4.setLayout(new BorderLayout(0, 0));

        JPanel panel_5 = new JPanel();
        panel_4.add(panel_5, BorderLayout.NORTH);
        GridBagLayout gbl_panel_5 = new GridBagLayout();
        gbl_panel_5.columnWidths = new int[] { 149, 122, 136, 86, 65, 66, 64, 93, 144, 0 };
        gbl_panel_5.rowHeights = new int[] { 25, 0 };
        gbl_panel_5.columnWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
        gbl_panel_5.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        panel_5.setLayout(gbl_panel_5);

        JPanel panel_2 = new JPanel();
        GridBagConstraints gbc_panel_2 = new GridBagConstraints();
        gbc_panel_2.fill = GridBagConstraints.BOTH;
        gbc_panel_2.insets = new Insets(0, 0, 0, 5);
        gbc_panel_2.gridx = 0;
        gbc_panel_2.gridy = 0;
        panel_5.add(panel_2, gbc_panel_2);
        panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.X_AXIS));

        JButton btnParse = new JButton("Parse");
        panel_2.add(btnParse);

        JButton btnSave_1 = new JButton("Save");
        btnSave_1.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    scenario.saveScenario("test.json");
                }
                catch (IOException e1)
                {
                    LOG.error(e1.toString(), e1);
                }
            }
        });
        panel_2.add(btnSave_1);

        JButton btnOpenBrowser = new JButton("Apply step");
        GridBagConstraints gbc_btnOpenBrowser = new GridBagConstraints();
        gbc_btnOpenBrowser.insets = new Insets(0, 0, 0, 5);
        gbc_btnOpenBrowser.gridx = 1;
        gbc_btnOpenBrowser.gridy = 0;
        panel_5.add(btnOpenBrowser, gbc_btnOpenBrowser);

        JButton btnCloseBrowser = new JButton("Close browsers");
        GridBagConstraints gbc_btnCloseBrowser = new GridBagConstraints();
        gbc_btnCloseBrowser.insets = new Insets(0, 0, 0, 5);
        gbc_btnCloseBrowser.gridx = 2;
        gbc_btnCloseBrowser.gridy = 0;
        panel_5.add(btnCloseBrowser, gbc_btnCloseBrowser);

        JButton btnRewind = new JButton("Rewind");
        GridBagConstraints gbc_btnRewind = new GridBagConstraints();
        gbc_btnRewind.insets = new Insets(0, 0, 0, 5);
        gbc_btnRewind.gridx = 3;
        gbc_btnRewind.gridy = 0;
        panel_5.add(btnRewind, gbc_btnRewind);

        JButton btnPrev = new JButton("Prev");
        GridBagConstraints gbc_btnPrev = new GridBagConstraints();
        gbc_btnPrev.insets = new Insets(0, 0, 0, 5);
        gbc_btnPrev.gridx = 4;
        gbc_btnPrev.gridy = 0;
        panel_5.add(btnPrev, gbc_btnPrev);

        JButton btnNext = new JButton("Next");
        GridBagConstraints gbc_btnNext = new GridBagConstraints();
        gbc_btnNext.insets = new Insets(0, 0, 0, 5);
        gbc_btnNext.gridx = 5;
        gbc_btnNext.gridy = 0;
        panel_5.add(btnNext, gbc_btnNext);

        JButton btnSkip = new JButton("Skip");
        GridBagConstraints gbc_btnSkip = new GridBagConstraints();
        gbc_btnSkip.insets = new Insets(0, 0, 0, 5);
        gbc_btnSkip.gridx = 6;
        gbc_btnSkip.gridy = 0;
        panel_5.add(btnSkip, gbc_btnSkip);

        JButton btnDel = new JButton("Del step");
        GridBagConstraints gbc_btnDel = new GridBagConstraints();
        gbc_btnDel.insets = new Insets(0, 0, 0, 5);
        gbc_btnDel.gridx = 7;
        gbc_btnDel.gridy = 0;
        panel_5.add(btnDel, gbc_btnDel);

        JPanel panel = new JPanel();
        GridBagConstraints gbc_panel = new GridBagConstraints();
        gbc_panel.fill = GridBagConstraints.BOTH;
        gbc_panel.gridx = 8;
        gbc_panel.gridy = 0;
        panel_5.add(panel, gbc_panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        JButton CopyStepButton = new JButton("Copy step");
        CopyStepButton.addActionListener(e -> copyCurrentStep());
        panel.add(CopyStepButton);

        JButton btnCheck = new JButton("Check");
        panel.add(btnCheck);
        btnCheck.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                checkElement(table.getSelectedRow());
            }
        });

        JButton btnPlay = new JButton("Play");
        btnPlay.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                playTheScenario();
            }
        });
        panel.add(btnPlay);
        btnDel.addActionListener(e -> {
            for (int row : table.getSelectedRows())
            {
                scenario.deleteStep(row);
            }
            model.fireTableDataChanged();
        });
        btnSkip.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                scenario.skip();
                model.fireTableDataChanged();
            }
        });
        btnNext.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                new ScenarioProcessor().applyStep(scenario, seleniumDriver, scenario.getPosition());
                scenario.moveToNextStep();
                model.fireTableDataChanged();
            }
        });
        btnPrev.addActionListener(e -> scenario.moveToPreviousStep());
        btnRewind.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                scenario.rewind();
                seleniumDriver.resetLastUrls();
                model.fireTableDataChanged();
            }
        });
        btnCloseBrowser.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                seleniumDriver.closeWebDrivers();
            }
        });
        btnOpenBrowser.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                new ScenarioProcessor().applyStep(scenario, seleniumDriver, table.getSelectedRow());
            }
        });
        btnParse.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    scenario.parseNextLine(InputFileConfigHolder.getInstance().getFilename());
                    scenario.setPreProcessScenarioScript(scriptArea.getText());
                    scenario.preProcessScenario();
                    long secs = scenario.getEventsDuration();
                    statisticsLabel.setText(String.format("Events %d, duration %f sec", scenario.getStepsCount(),
                            secs / 1000.0));
                    model = createEventTableModel();
                    table.setModel(model);
                    model.fireTableDataChanged();
                    setColumnWidths();
                    createTableEditor();
                }
                catch (IOException e1)
                {
                    LOG.error(e1.toString(), e1);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane();
        panel_4.add(scrollPane, BorderLayout.CENTER);

        table = new JTable();
        table.setFont(new Font("Hack", Font.PLAIN, 14));
        table.setRowHeight(30);
        scrollPane.setViewportView(table);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {

            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                resetCurrentEvent();
            }
        });

        JSplitPane splitPane = new JSplitPane();
        splitPane.setContinuousLayout(true);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        scenarioPanel.add(splitPane, BorderLayout.CENTER);

        eventContent = new RSyntaxTextArea();
        RTextScrollPane scrollPane_2 = new RTextScrollPane((Component)eventContent);
        configureScriptTextArea(eventContent, scrollPane_2, SyntaxConstants.SYNTAX_STYLE_JSON);

        splitPane.setLeftComponent(panel_4);
        splitPane.setRightComponent(scrollPane_2);

        FoldParserManager.get().addFoldParserMapping(SyntaxConstants.SYNTAX_STYLE_JSON, new JsonFoldParser());

        JPanel panel_6 = new JPanel();
        panel_6.setBorder(null);
        scenarioPanel.add(panel_6, BorderLayout.SOUTH);
        GridBagLayout gbl_panel_6 = new GridBagLayout();
        gbl_panel_6.columnWidths = new int[] { 378, 0 };
        gbl_panel_6.rowHeights = new int[] { 0, 0 };
        gbl_panel_6.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gbl_panel_6.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
        panel_6.setLayout(gbl_panel_6);

        JPanel panel_1 = new JPanel();
        GridBagConstraints gbc_panel_1 = new GridBagConstraints();
        gbc_panel_1.anchor = GridBagConstraints.SOUTH;
        gbc_panel_1.fill = GridBagConstraints.HORIZONTAL;
        gbc_panel_1.gridx = 0;
        gbc_panel_1.gridy = 0;
        panel_6.add(panel_1, gbc_panel_1);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));

        JButton btnUpdate = new JButton("Update");
        btnUpdate.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                updateCurrentEvent();
            }
        });
        panel_1.add(btnUpdate);

        JButton btnReset_1 = new JButton("Reset");
        btnReset_1.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                resetCurrentEvent();
            }
        });
        panel_1.add(btnReset_1);

        JLabel lblEvents = new JLabel("Statistics");
        panel_1.add(lblEvents);

        statisticsLabel = new JLabel("DATA");
        panel_1.add(statisticsLabel);

        JPanel postProcessPanel = new JPanel();
        tabbedPane.addTab("Post process", null, postProcessPanel, null);
        postProcessPanel.setLayout(new BorderLayout(0, 0));

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        postProcessPanel.add(toolBar, BorderLayout.NORTH);

        JLabel lblScript = new JLabel("Script");
        toolBar.add(lblScript);

        scriptFilename = new JTextField();
        toolBar.add(scriptFilename);
        scriptFilename.setColumns(15);

        JButton btnBrowse_1 = new JButton("Browse");
        btnBrowse_1.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION)
                {
                    String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
                    scriptFilename.setText(selectedFile);
                    loadSelectedFile();
                }
            }
        });
        toolBar.add(btnBrowse_1);

        JButton btnLoad_1 = new JButton("Load");
        btnLoad_1.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                loadSelectedFile();
            }
        });
        toolBar.add(btnLoad_1);

        JButton btnSave = new JButton("Save");
        btnSave.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    FileUtils.writeStringToFile(new File("scripts/postprocess.groovy"), scriptArea.getText());
                }
                catch (IOException e1)
                {
                    throw new RuntimeException(e1);
                }
            }
        });
        toolBar.add(btnSave);

        JButton btnReset = new JButton("Reset");
        toolBar.add(btnReset);

        JButton btnRun = new JButton("Run");
        btnRun.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                scenario.runPostProcessor(scriptArea.getText());
            }
        });
        toolBar.add(btnRun);

        RTextScrollPane scrollPane_3 = new RTextScrollPane();
        postProcessPanel.add(scrollPane_3, BorderLayout.CENTER);

        scriptArea = new RSyntaxTextArea();
        scrollPane_3.setViewportView(scriptArea);
        configureScriptTextArea(scriptArea, scrollPane_3, SyntaxConstants.SYNTAX_STYLE_GROOVY);

        JPanel webLookupPanel = new JPanel();
        tabbedPane.addTab("Web lookup", null, webLookupPanel, null);
        webLookupPanel.setLayout(new BorderLayout(0, 0));

        JToolBar toolBar_1 = new JToolBar();
        webLookupPanel.add(toolBar_1, BorderLayout.NORTH);

        JLabel label_1 = new JLabel("Script");
        toolBar_1.add(label_1);

        lookupFilename = new JTextField();
        lookupFilename.setColumns(15);
        toolBar_1.add(lookupFilename);

        JButton button = new JButton("Browse");
        toolBar_1.add(button);

        button.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION)
                {
                    String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
                    lookupFilename.setText(selectedFile);
                }
            }
        });

        JButton button_1 = new JButton("Load");
        toolBar_1.add(button_1);

        button_1.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    lookupScriptArea.setText(FileInput.getContentInString(lookupFilename.getText()));
                }
                catch (IOException e1)
                {
                    throw new RuntimeException(e1);
                }
            }
        });

        JButton button_2 = new JButton("Save");
        toolBar_1.add(button_2);

        button_2.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    FileUtils.writeStringToFile(new File("scripts/weblookup.groovy"), lookupScriptArea.getText());
                }
                catch (IOException e1)
                {
                    throw new RuntimeException(e1);
                }
            }
        });

        JButton button_3 = new JButton("Reset");
        toolBar_1.add(button_3);

        RTextScrollPane scrollPane_4 = new RTextScrollPane();
        webLookupPanel.add(scrollPane_4, BorderLayout.CENTER);

        lookupScriptArea = new RSyntaxTextArea();
        scrollPane_4.setViewportView(lookupScriptArea);
        configureScriptTextArea(lookupScriptArea, scrollPane_4, SyntaxConstants.SYNTAX_STYLE_GROOVY);

        JPanel optionsPanel = new JPanel();
        tabbedPane.addTab("Options", null, optionsPanel, null);
        optionsPanel.setLayout(new FormLayout(new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC, FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), },
                new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, }));

        JLabel lblFirefoxProxyHost = new JLabel("Proxy host");
        optionsPanel.add(lblFirefoxProxyHost, "2, 2, right, default");

        proxyHost = new JTextField();
        optionsPanel.add(proxyHost, "4, 2, fill, default");
        proxyHost.setColumns(10);

        JLabel lblFirefoxProxyPort = new JLabel("Proxy port");
        optionsPanel.add(lblFirefoxProxyPort, "2, 4, right, default");

        proxyPort = new JTextField();
        optionsPanel.add(proxyPort, "4, 4, fill, default");
        proxyPort.setColumns(10);

        JLabel lblFirefoxPath = new JLabel("Firefox path");
        optionsPanel.add(lblFirefoxPath, "2, 6, right, default");

        ffPath = new JTextField();
        ffPath.setText("/usr/bin/firefox");
        optionsPanel.add(ffPath, "4, 6, fill, default");
        ffPath.setColumns(10);

        JLabel lblPhantomJsPath = new JLabel("PhantomJs path");
        optionsPanel.add(lblPhantomJsPath, "2, 8, right, default");

        pjsPath = new JTextField();
        pjsPath.setText("phantomjs/phantomjs");
        pjsPath.setColumns(10);
        optionsPanel.add(pjsPath, "4, 8, fill, default");

        JLabel lblPageReadyTimeout = new JLabel("Page ready timeout, sec");
        optionsPanel.add(lblPageReadyTimeout, "2, 10, right, default");

        pageReadyTimeoutField = new JTextField();
        pageReadyTimeoutField.setText("30");
        optionsPanel.add(pageReadyTimeoutField, "4, 10, fill, default");
        pageReadyTimeoutField.setColumns(10);

        JLabel lblBrowser = new JLabel("Browser");
        optionsPanel.add(lblBrowser, "2, 12, right, default");

        JPanel panel_3 = new JPanel();
        optionsPanel.add(panel_3, "4, 12, fill, fill");
        panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.X_AXIS));

        useFirefoxButton = new JRadioButton("Firefox");
        buttonGroup.add(useFirefoxButton);
        useFirefoxButton.setSelected(true);
        panel_3.add(useFirefoxButton);

        usePhantomButton = new JRadioButton("PhantomJs");
        buttonGroup.add(usePhantomButton);
        panel_3.add(usePhantomButton);

        JLabel label = new JLabel("Make screenshots");
        optionsPanel.add(label, "2, 14, right, default");

        makeShots = new JCheckBox("");
        makeShots.setSelected(true);
        optionsPanel.add(makeShots, "4, 14");

        JLabel lblScreenshotDirectory = new JLabel("Screenshot directory");
        optionsPanel.add(lblScreenshotDirectory, "2, 16, right, default");

        screenDirTextField = new JTextField();
        screenDirTextField.setText("shots");
        screenDirTextField.setColumns(10);
        optionsPanel.add(screenDirTextField, "4, 16, fill, default");

        JLabel lblCheckPageReady = new JLabel("Check page ready");
        optionsPanel.add(lblCheckPageReady, "2, 18, right, default");

        isAsyncRequestsCompleted = new JTextField();
        isAsyncRequestsCompleted.setText("return document.getElementById('state.context').getAttribute('value');");
        isAsyncRequestsCompleted.setColumns(10);
        optionsPanel.add(isAsyncRequestsCompleted, "4, 18, fill, default");

        JLabel lblWebdriverTag = new JLabel("WebDriver tag");
        optionsPanel.add(lblWebdriverTag, "2, 20, right, default");

        webDriverTag = new JTextField();
        webDriverTag.setText("uuid");
        webDriverTag.setColumns(10);
        optionsPanel.add(webDriverTag, "4, 20, fill, default");

        JLabel useRandomCharsLabel = new JLabel("Use random chars instead of recorded chars");
        optionsPanel.add(useRandomCharsLabel, "2, 22, right, fill");

        useRandomCharsBox = new JCheckBox("");
        optionsPanel.add(useRandomCharsBox, "4, 22");

        JLabel lblFirefoxXdispay = new JLabel("Firefox XDispay");
        optionsPanel.add(lblFirefoxXdispay, "2, 24, right, default");

        firefoxDsiplay = new JTextField();
        firefoxDsiplay.setColumns(10);
        optionsPanel.add(firefoxDsiplay, "4, 24, fill, default");

        JLabel lblFromXpath = new JLabel("Form or dialog xpath");
        optionsPanel.add(lblFromXpath, "2, 26, right, default");

        formDialogXpathField = new JTextField();
        optionsPanel.add(formDialogXpathField, "4, 26, fill, default");
        JPanel jmeterPanel = new JPanel();
        tabbedPane.addTab("JMeter", null, jmeterPanel, null);
        jmeterPanel.setLayout(new FormLayout(new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC, FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), },
                new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("default:grow"), FormSpecs.RELATED_GAP_ROWSPEC, RowSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC, FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, }));

        JLabel lblJmeterRecorder_1 = new JLabel("JMeter recorder");
        jmeterPanel.add(lblJmeterRecorder_1, "2, 2, right, default");

        JPanel label_5 = new JPanel();
        jmeterPanel.add(label_5, "4, 2");
        label_5.setLayout(new BoxLayout(label_5, BoxLayout.X_AXIS));

        JButton startProxyButton = new JButton("Start proxy");
        startProxyButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    jmeter.startRecording();
                }
                catch (IOException e1)
                {
                    LOG.error(e1.toString(), e1);
                }
            }
        });

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    jmeter.getScriptProcessor().setScenarioProcessorScript(stepProcessScript.getText());
                    jmeter.getScriptProcessor().setStepProcessorScript(scenarioProcessScript.getText());
                    jmeter.reset();
                }
                catch (IOException e1)
                {
                    LOG.error(e1.toString(), e1);
                }
            }
        });
        resetButton.setSelected(true);
        label_5.add(resetButton);
        startProxyButton.setSelected(true);
        label_5.add(startProxyButton);

        JButton stopProxyButton = new JButton("Stop proxy");
        stopProxyButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                jmeter.stopRecording();
            }
        });
        stopProxyButton.setSelected(true);
        label_5.add(stopProxyButton);

        JButton saveRecordingButton = new JButton("Save recording");
        saveRecordingButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    jmeter.saveScenario(scenarioTextField.getText());
                }
                catch (IOException e1)
                {
                    LOG.error(e1.toString(), e1);
                }
            }
        });
        saveRecordingButton.setSelected(true);
        label_5.add(saveRecordingButton);

        JLabel lblJmeterRecorder = new JLabel("JMeter scenario");
        jmeterPanel.add(lblJmeterRecorder, "2, 4, right, default");

        scenarioTextField = new JTextField();
        scenarioTextField.setText("test.jmx");
        scenarioTextField.setColumns(10);
        jmeterPanel.add(scenarioTextField, "4, 4, fill, default");

        JLabel lblJmeterStepPost = new JLabel("Step processor");
        jmeterPanel.add(lblJmeterStepPost, "2, 6, right, top");

        JPanel panel_7 = new JPanel();
        jmeterPanel.add(panel_7, "4, 6, fill, fill");
        panel_7.setLayout(new BorderLayout(0, 0));

        RTextScrollPane scrollPane_5 = new RTextScrollPane();
        panel_7.add(scrollPane_5, BorderLayout.CENTER);

        stepProcessScript = new RSyntaxTextArea();
        scrollPane_5.setViewportView(stepProcessScript);
        configureScriptTextArea(stepProcessScript, scrollPane_5, SyntaxConstants.SYNTAX_STYLE_GROOVY);

        JLabel lblScenarioProcessor = new JLabel("Scenario processor");
        jmeterPanel.add(lblScenarioProcessor, "2, 8, right, top");

        JPanel panel_8 = new JPanel();
        jmeterPanel.add(panel_8, "4, 8, fill, fill");
        panel_8.setLayout(new BorderLayout(0, 0));

        RTextScrollPane scrollPane_6 = new RTextScrollPane();
        panel_8.add(scrollPane_6, BorderLayout.CENTER);

        scenarioProcessScript = new RSyntaxTextArea();
        scrollPane_6.setViewportView(scenarioProcessScript);
        configureScriptTextArea(scenarioProcessScript, scrollPane_6, SyntaxConstants.SYNTAX_STYLE_GROOVY);

        JPanel duplicatesPanel = new JPanel();
        tabbedPane.addTab("Duplicate handler", null, duplicatesPanel, null);
        duplicatesPanel.setLayout(new BorderLayout(0, 0));

        JPanel panel_9 = new JPanel();
        duplicatesPanel.add(panel_9, BorderLayout.SOUTH);

        JToolBar duplicatesToolBar = new JToolBar();
        duplicatesPanel.add(duplicatesToolBar, BorderLayout.NORTH);

        JLabel duplicatesScriptLabel = new JLabel("Script");
        duplicatesToolBar.add(duplicatesScriptLabel);

        duplicatesFilePath = new JTextField();
        duplicatesToolBar.add(duplicatesFilePath);
        duplicatesFilePath.setColumns(10);

        JButton dupsBrowseBtn = new JButton("Browse");
        duplicatesToolBar.add(dupsBrowseBtn);

        dupsBrowseBtn.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION)
                {
                    String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
                    duplicatesFilePath.setText(selectedFile);
                }
            }
        });

        JButton dupsLoadBtn = new JButton("Load");
        duplicatesToolBar.add(dupsLoadBtn);

        dupsLoadBtn.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    duplicatesScriptArea.setText(FileInput.getContentInString(duplicatesFilePath.getText()));
                }
                catch (IOException e1)
                {
                    throw new RuntimeException(e1);
                }
            }
        });

        JButton dupsSaveBtn = new JButton("Save");
        duplicatesToolBar.add(dupsSaveBtn);

        dupsSaveBtn.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                try
                {
                    FileUtils.writeStringToFile(new File("scripts/duplicateHandler.groovy"),
                            duplicatesScriptArea.getText());
                }
                catch (IOException e1)
                {
                    throw new RuntimeException(e1);
                }
            }
        });

        RTextScrollPane duplicatesScrollPanel = new RTextScrollPane();
        duplicatesPanel.add(duplicatesScrollPanel, BorderLayout.CENTER);

        duplicatesScriptArea = new RSyntaxTextArea();
        duplicatesScrollPanel.setViewportView(duplicatesScriptArea);
        configureScriptTextArea(duplicatesScriptArea, duplicatesScrollPanel, SyntaxConstants.SYNTAX_STYLE_GROOVY);

        JPanel ScriptEventPanel = new JPanel();
        tabbedPane.addTab("Script event handler", null, ScriptEventPanel, null);
        ScriptEventPanel.setLayout(new BorderLayout(0, 0));

        JToolBar scriptEventToolBar = new JToolBar();
        ScriptEventPanel.add(scriptEventToolBar, BorderLayout.NORTH);

        JLabel scriptEventHandlerPathLabel = new JLabel("Path to script");
        scriptEventToolBar.add(scriptEventHandlerPathLabel);

        scriptEventHandlerFilePath = new JTextField();
        scriptEventToolBar.add(scriptEventHandlerFilePath);
        scriptEventHandlerFilePath.setColumns(10);

        JButton scriptEventHandlerBrowse = new JButton("Browse");
        scriptEventToolBar.add(scriptEventHandlerBrowse);

        scriptEventHandlerBrowse.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION)
                {
                    String selectedFile = fileChooser.getSelectedFile().getAbsolutePath();
                    scriptEventHandlerFilePath.setText(selectedFile);
                }

            }
        });

        JButton scriptEventHandlerLoad = new JButton("Load");
        scriptEventToolBar.add(scriptEventHandlerLoad);

        scriptEventHandlerLoad.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    scriptEventHandlerScriptArea.setText(FileInput.getContentInString(scriptEventHandlerFilePath
                            .getText()));
                }
                catch (IOException e1)
                {
                    throw new RuntimeException(e1);
                }
            }
        });

        JButton scriptEventHandlerSave = new JButton("Save");
        scriptEventToolBar.add(scriptEventHandlerSave);

        scriptEventHandlerSave.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    FileUtils.writeStringToFile(new File("scripts/scriptEventHandler.groovy"),
                            scriptEventHandlerScriptArea.getText());
                }
                catch (IOException e1)
                {
                    throw new RuntimeException(e1);
                }

            }
        });

        RTextScrollPane scriptEventHandlerScrollPane = new RTextScrollPane();
        ScriptEventPanel.add(scriptEventHandlerScrollPane, BorderLayout.CENTER);

        scriptEventHandlerScriptArea = new RSyntaxTextArea();
        scriptEventHandlerScrollPane.setViewportView(scriptEventHandlerScriptArea);
        configureScriptTextArea(scriptEventHandlerScriptArea, scriptEventHandlerScrollPane,
                SyntaxConstants.SYNTAX_STYLE_GROOVY);

        initUIFromControllers();
    }

    private void initUIFromControllers()
    {
        initUIFromOptionsController();
        initUIFromJMeterController();
        initUIFromPostProcessorController();
        initUIFromWebLookupController();
        initUIFromInitController();
        initUIFromDuplicateHandlerController();
        initUIFromScriptEventHandlerController();
    }

    private void initUIFromDuplicateHandlerController()
    {
        duplicatesScriptArea.setText(scenario.getConfiguration().getScriptsConfiguration().getDuplicationHandlerScript());
    }

    private void initUIFromInitController()
    {
        filenameField.setText(InputFileConfigHolder.getInstance().getFilename());
    }

    private void initUIFromJMeterController()
    {
//TODO        scenario.getConfiguration().getScriptsConfiguration().syncScripts(jmeter);
        stepProcessScript.setText(scenario.getConfiguration().getScriptsConfiguration().getStepProcessorScript());
        scenarioProcessScript
                .setText(scenario.getConfiguration().getScriptsConfiguration().getScenarioProcessorScript());
    }

    private void initUIFromOptionsController()
    {
        //TODO Init all configurations and/or add necessary configHolders
        CommonFileConfigHolder.getInstance().setConfiguration(scenario.getConfiguration().getCommonConfiguration());
        try
        {
            //TODO Load all configurations and/or add necessary configHolders
            InputFileConfigHolder.getInstance().load(IFileConfigHolder.defaultConfig);
            CommonFileConfigHolder.getInstance().load(IFileConfigHolder.defaultConfig);
            PostProcessFileConfigHolder.getInstance().load(IFileConfigHolder.defaultConfig);
        }
        catch (Exception e)
        {
            LOG.error(e.toString(), e);
        }
        proxyHost.setText(scenario.getConfiguration().getCommonConfiguration().getProxyHost());
        proxyPort.setText(scenario.getConfiguration().getCommonConfiguration().getProxyPort().toString());
        ffPath.setText(scenario.getConfiguration().getCommonConfiguration().getPathToBrowserExecutable());
        pjsPath.setText(scenario.getConfiguration().getCommonConfiguration().getPathToBrowserExecutable());
        useFirefoxButton.setSelected(scenario.getConfiguration().getCommonConfiguration().getBrowserType().equals(BrowserType.FIREFOX));
        pageReadyTimeoutField.setText(scenario.getConfiguration().getCommonConfiguration().getAsyncRequestsCompletedTimeoutInSeconds().toString());
        makeShots.setSelected(scenario.getConfiguration().getCommonConfiguration().getMakeShots());
        screenDirTextField.setText(scenario.getConfiguration().getCommonConfiguration().getScreenshotsDirectory());
        isAsyncRequestsCompleted.setText(scenario.getConfiguration().getScriptsConfiguration().getIsAsyncRequestsCompletedScript());
        webDriverTag.setText(scenario.getConfiguration().getCommonConfiguration().getWebDriverTag());
        useRandomCharsBox.setSelected(scenario.getConfiguration().getCommonConfiguration().isUseRandomChars());
        formDialogXpathField.setText(scenario.getConfiguration().getCommonConfiguration().getFormOrDialogXpath());
    }

    private void initUIFromPostProcessorController()
    {
        scriptFilename.setText(PostProcessFileConfigHolder.getInstance().getFilename());
        scriptArea.setText(PostProcessFileConfigHolder.getInstance().getScript());
    }

    private void initUIFromScriptEventHandlerController()
    {
        scriptEventHandlerScriptArea.setText(scenario.getConfiguration().getScriptsConfiguration().getScriptEventHandlerScript());
    }

    private void initUIFromWebLookupController()
    {
        lookupScriptArea.setText(scenario.getConfiguration().getScriptsConfiguration().getElementLookupScript());
    }

    /**
     * @throws IOException
     */
    private void loadScenario() throws IOException
    {
        String filename = filenameField.getText().trim();
        if (filename.length() == 0)
        {
            return;
        }
        try
        {
            scenario.parse(filename);
            contentPane.setText(FileInput.getContentInString(filename));
        }
        catch (IOException e1)
        {
            LOG.error(e1.toString(), e1);
            throw e1;
        }
    }

    private void loadSelectedFile()
    {
        try
        {
            String content = FileInput.getContentInString(scriptFilename.getText());
            if (content == null)
            {
                content = "File is too big to show it";
            }
            scriptArea.setText(content);
        }
        catch (IOException e1)
        {
            throw new RuntimeException(e1);
        }
    }

    private void updateDuplicateHandlerController()
    {
        scenario.getConfiguration().getScriptsConfiguration().setDuplicationHandlerScript(duplicatesScriptArea.getText());
    }

    private void updateInputController()
    {
        InputFileConfigHolder.getInstance().setFilename(filenameField.getText());
    }

    private void updateJMeterController()
    {
        scenario.getConfiguration().getScriptsConfiguration()
                .setStepProcessorScript(stepProcessScript.getText());
        scenario.getConfiguration().getScriptsConfiguration()
                .setScenarioProcessorScript(scenarioProcessScript.getText());
    }

    private void updateOptionsController()
    {
        scenario.getConfiguration().getCommonConfiguration().setProxyHost(proxyHost.getText());
        scenario.getConfiguration().getCommonConfiguration().setProxyPort(Integer.valueOf(proxyPort.getText()));
        scenario.getConfiguration().getCommonConfiguration().setPathToBrowserExecutable(ffPath.getText());
        scenario.getConfiguration().getCommonConfiguration().setAsyncRequestsCompletedTimeoutInSeconds(Integer.parseInt(pageReadyTimeoutField.getText()));
        scenario.getConfiguration().getCommonConfiguration().setMakeShots(makeShots.isSelected());
        scenario.getConfiguration().getCommonConfiguration().setScreenshotsDirectory(screenDirTextField.getText());
        scenario.getConfiguration().getScriptsConfiguration().setIsAsyncRequestsCompletedScript(isAsyncRequestsCompleted.getText());
        scenario.getConfiguration().getCommonConfiguration().setWebDriverTag(webDriverTag.getText());
        scenario.getConfiguration().getCommonConfiguration().setBrowserType(useFirefoxButton.isSelected()
                ? BrowserType.FIREFOX
                : BrowserType.CHROME);
        scenario.getConfiguration().getCommonConfiguration().setUseRandomChars(useRandomCharsBox.isSelected());
        scenario.getConfiguration().getCommonConfiguration().setFormOrDialogXpath(formDialogXpathField.getText());
    }

    private void updatePostProcessController()
    {
        PostProcessFileConfigHolder.getInstance().setFilename(scriptFilename.getText());
        PostProcessFileConfigHolder.getInstance().setScript(scriptArea.getText());
    }

    private void updateScriptEventHandlerController()
    {
        scenario.getConfiguration().getScriptsConfiguration().setScriptEventHandlerScript(scriptEventHandlerScriptArea.getText());
    }

    private void updateWebLookupController()
    {
        scenario.getConfiguration().getScriptsConfiguration().setElementLookupScript(lookupScriptArea.getText());
    }
}
