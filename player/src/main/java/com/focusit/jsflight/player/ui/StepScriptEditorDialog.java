package com.focusit.jsflight.player.ui;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.TableCellEditor;

import com.focusit.jsflight.player.scenario.UserScenario;

public class StepScriptEditorDialog extends DefaultCellEditor implements TableCellEditor
{
    private static final long serialVersionUID = 1L;

    private String newInput;
    private String oldValue;
    private ScriptDialog dialog;
    private JButton editorComponent;
    private UserScenario scenario;
    private String stepProperty = "";
    private int position = -1;

    public StepScriptEditorDialog(UserScenario scenario, boolean pre)
    {
        super(new JTextField());
        setClickCountToStart(1);
        this.scenario = scenario;

        if (pre)
        {
            stepProperty = "pre";
        }
        else
        {
            stepProperty = "post";
        }

        editorComponent = new JButton();
        editorComponent.setBackground(Color.white);
        editorComponent.setBorderPainted(false);
        editorComponent.setContentAreaFilled(false);

        // Make sure focus goes back to the table when the dialog is closed
        editorComponent.setFocusable(false);

        dialog = new ScriptDialog();
    }

    public void cancelEdit()
    {
        fireEditingCanceled();
    }

    public void endEdit()
    {
        newInput = dialog.getNewValue();

        if (newInput.trim().length() > 0)
        {
            scenario.getStepAt(position).put(stepProperty, newInput);
        }

        fireEditingStopped();
    }

    @Override
    public Object getCellEditorValue()
    {
        return null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
    {
        oldValue = scenario.getStepAt(row).has(stepProperty) ? oldValue = scenario.getStepAt(row).getString(
                stepProperty) : "";

        newInput = oldValue;

        this.position = row;

        final StepScriptEditorDialog editor = this;
        dialog.setOldValue(oldValue);

        SwingUtilities.invokeLater(() -> {
            dialog.setEditor(editor);
            dialog.show();
        });

        return editorComponent;
    }

}
