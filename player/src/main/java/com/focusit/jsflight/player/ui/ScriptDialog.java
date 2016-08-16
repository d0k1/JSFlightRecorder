package com.focusit.jsflight.player.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class ScriptDialog extends JDialog
{
    private static final long serialVersionUID = 1L;

    private final JPanel contentPanel = new JPanel();

    private StepScriptEditorDialog editor;
    private RSyntaxTextArea scriptArea;
    private RTextScrollPane scrollPane;

    /**
     * Create the dialog.
     */
    public ScriptDialog()
    {
        setTitle("Enter script");
        setModalityType(ModalityType.APPLICATION_MODAL);
        setBounds(100, 100, 450, 300);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new BorderLayout(0, 0));
        {
            scrollPane = new RTextScrollPane();
            contentPanel.add(scrollPane, BorderLayout.CENTER);
        }
        {
            scriptArea = new RSyntaxTextArea();
            scriptArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GROOVY);
            scriptArea.setCodeFoldingEnabled(true);
            scrollPane.setViewportView(scriptArea);
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        applyEditor();
                    }
                });
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        cancelEditor();
                    }
                });
                cancelButton.setActionCommand("Cancel");
                buttonPane.add(cancelButton);
            }
        }
    }

    public StepScriptEditorDialog getEditor()
    {
        return editor;
    }

    public void setEditor(StepScriptEditorDialog editor)
    {
        this.editor = editor;
    }

    public String getNewValue()
    {
        return this.scriptArea.getText();
    }

    public void setOldValue(String value)
    {
        this.scriptArea.setText(value);
    }

    private void applyEditor()
    {
        getEditor().endEdit();
        dispose();
    }

    private void cancelEditor()
    {
        getEditor().cancelEdit();
        dispose();
    }
}
