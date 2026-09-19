package com.itson.jgantt.ui.dialog;

import java.awt.Component;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.itson.jgantt.app.dto.ProjectSummaryDto;
import com.itson.jgantt.domain.valueobject.ProjectId;

public final class OpenProjectDialog {

    private OpenProjectDialog() {
    }

    public static ProjectId show(Component parent, List<ProjectSummaryDto> projects) {
        if (projects.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "There are no saved projects yet.\nCreate a new project first.",
                    "Open project", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        DefaultListModel<ProjectSummaryDto> model = new DefaultListModel<>();
        projects.forEach(model::addElement);

        JList<ProjectSummaryDto> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(10);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof ProjectSummaryDto project) {
                    label.setText(project.name());
                }
                return label;
            }
        });

        int option = JOptionPane.showOptionDialog(parent, new JScrollPane(list),
                "Open project", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new Object[] {"Open", "Cancel"}, "Open");
        if (option != JOptionPane.OK_OPTION) {
            return null;
        }
        ProjectSummaryDto selected = list.getSelectedValue();
        return selected == null ? null : selected.id();
    }
}