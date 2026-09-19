package com.itson.jgantt.ui.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.ui.model.OutlineTaskTableModel;
import com.itson.jgantt.ui.model.TaskEditListener;
import com.itson.jgantt.ui.util.TaskOutline;

public final class TaskTablePanel extends JPanel {

    public static final int ROW_HEIGHT = 28;

    private final OutlineTaskTableModel model;
    private final JTable table;
    private final JScrollPane scrollPane;
    private final Runnable layoutChanged;

    public TaskTablePanel(TaskEditListener listener, Runnable layoutChanged) {
        super(new BorderLayout());
        this.layoutChanged = layoutChanged;
        this.model = new OutlineTaskTableModel(listener);
        this.table = new JTable(model);

        table.setRowHeight(ROW_HEIGHT);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(false);

        configureColumn(OutlineTaskTableModel.COL_NAME, 240, new NameRenderer(), new DefaultCellEditor(new JTextField()));
        configureColumn(OutlineTaskTableModel.COL_START, 95, null, new DateCellEditor());
        configureColumn(OutlineTaskTableModel.COL_END, 95, null, new DateCellEditor());
        configureColumn(OutlineTaskTableModel.COL_LENGTH, 60, null, null);
        configureColumn(OutlineTaskTableModel.COL_PROGRESS, 80, new ProgressRenderer(), new ProgressCellEditor());

        installClickToToggle();
        this.scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    public JTable getTable() {
        return table;
    }

    public JScrollBar getVerticalScrollBar() {
        return scrollPane.getVerticalScrollBar();
    }

    public JScrollBar getHorizontalScrollBar() {
        return scrollPane.getHorizontalScrollBar();
    }

    public List<TaskDto> visibleTasks() {
        return model.visibleTasks();
    }

    public List<TaskId> getSelectedTaskIds() {
        return Arrays.stream(table.getSelectedRows())
                .filter(row -> row >= 0 && row < model.visibleTasks().size())
                .mapToObj(row -> model.visibleTasks().get(row).id())
                .toList();
    }

    public void setTasks(List<TaskDto> tasks) {
        List<TaskId> selection = getSelectedTaskIds();
        model.setTasks(tasks);
        restoreSelection(selection);
    }

    private void restoreSelection(List<TaskId> ids) {
        table.clearSelection();
        for (int row = 0; row < model.getRowCount(); row++) {
            if (ids.contains(model.visibleTasks().get(row).id())) {
                table.addRowSelectionInterval(row, row);
            }
        }
    }

    private void configureColumn(int columnIndex, int width, DefaultTableCellRenderer renderer,
            DefaultCellEditor editor) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setPreferredWidth(width);
        if (renderer != null) {
            column.setCellRenderer(renderer);
        }
        if (editor != null) {
            column.setCellEditor(editor);
        }
    }

    private void installClickToToggle() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() != 1) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                int viewColumn = table.columnAtPoint(e.getPoint());
                if (row < 0 || viewColumn < 0) {
                    return;
                }
                if (table.convertColumnIndexToModel(viewColumn) != OutlineTaskTableModel.COL_NAME) {
                    return;
                }
                TaskDto task = model.visibleTasks().get(row);
                Rectangle cell = table.getCellRect(row, viewColumn, true);
                int xInCell = e.getX() - cell.x;
                int glyphStart = 2 + task.outlineLevel() * 14;
                if (xInCell >= glyphStart && xInCell <= glyphStart + 14) {
                    model.toggleExpanded(row);
                    if (layoutChanged != null) {
                        layoutChanged.run();
                    }
                }
            }
        });
    }

    private final class NameRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            TaskDto task = model.visibleTasks().get(row);
            boolean hasChildren = TaskOutline.hasChildren(model.allTasks(), task.id());
            boolean expanded = model.isExpanded(row);

            String glyph;
            if (hasChildren) {
                glyph = expanded ? "\u25BE " : "\u25B8 ";
            } else if (task.milestone()) {
                glyph = "\u25C6 ";
            } else {
                glyph = "   ";
            }
            label.setText(glyph + task.name());
            label.setBorder(BorderFactory.createEmptyBorder(0, 2 + task.outlineLevel() * 14, 0, 4));
            return label;
        }
    }

    private static final class ProgressRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            float progress = value instanceof Float f ? f : 0f;
            label.setText(Math.round(progress * 100) + "%");
            return label;
        }
    }

    private static final class DateCellEditor extends DefaultCellEditor {

        private DateCellEditor() {
            super(new JTextField());
        }

        @Override
        public boolean stopCellEditing() {
            try {
                LocalDate.parse(((JTextField) getComponent()).getText().trim());
            } catch (DateTimeParseException ex) {
                return false;
            }
            return super.stopCellEditing();
        }

        @Override
        public Object getCellEditorValue() {
            return LocalDate.parse(((JTextField) getComponent()).getText().trim());
        }
    }

    private static final class ProgressCellEditor extends DefaultCellEditor {

        private ProgressCellEditor() {
            super(new JTextField());
        }

        @Override
        public boolean stopCellEditing() {
            String text = ((JTextField) getComponent()).getText().trim().replace("%", "");
            try {
                int percent = Integer.parseInt(text);
                if (percent < 0 || percent > 100) {
                    return false;
                }
            } catch (NumberFormatException ex) {
                return false;
            }
            return super.stopCellEditing();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                int row, int column) {
            float progress = value instanceof Float f ? f : 0f;
            ((JTextField) getComponent()).setText(Math.round(progress * 100) + "%");
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        @Override
        public Object getCellEditorValue() {
            String text = ((JTextField) getComponent()).getText().trim().replace("%", "");
            return Integer.parseInt(text) / 100f;
        }
    }
}