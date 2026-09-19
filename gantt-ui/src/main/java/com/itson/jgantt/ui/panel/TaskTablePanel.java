package com.itson.jgantt.ui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.ui.model.OutlineTaskTableModel;
import com.itson.jgantt.ui.model.TaskEditListener;
import com.itson.jgantt.ui.util.TaskOutline;

public final class TaskTablePanel extends JPanel {

	public static final int ROW_HEIGHT = 28;

	private static final Color ZEBRA_COLOR = new Color(0xF5F7FB);
	private static final Color PROGRESS_TRACK_COLOR = new Color(0xE3E7EC);
	private static final Color PROGRESS_COLOR = new Color(0x3B82F6);
	private static final Color TEXT_COLOR = new Color(0x333333);

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
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 0));
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.getTableHeader().setReorderingAllowed(false);
		table.setAutoCreateRowSorter(false);
		table.setDefaultRenderer(Object.class, new ZebraRenderer());

		configureColumn(OutlineTaskTableModel.COL_NAME, 240, new NameRenderer(), new DefaultCellEditor(new JTextField()));
		configureColumn(OutlineTaskTableModel.COL_START, 95, null, new DateCellEditor());
		configureColumn(OutlineTaskTableModel.COL_END, 95, null, new DateCellEditor());
		configureColumn(OutlineTaskTableModel.COL_LENGTH, 60, new LeftAlignedRenderer(), null);
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

	private void configureColumn(int columnIndex, int width, TableCellRenderer renderer,
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
			paintZebra(this, table, row, isSelected);
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

	private static final class LeftAlignedRenderer extends DefaultTableCellRenderer {

		private LeftAlignedRenderer() {
			setHorizontalAlignment(JLabel.LEFT);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
			Component cell = super.getTableCellRendererComponent(
				table, value, isSelected, hasFocus, row, column);
			paintZebra(this, table, row, isSelected);
			return cell;
		}

	}

	private static final class ProgressRenderer extends JPanel implements TableCellRenderer {

		private float progress;
		private Color background;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
			progress = value instanceof Float f ? f : 0f;
			background = isSelected ? table.getSelectionBackground()
				: (row % 2 == 0 ? Color.WHITE : ZEBRA_COLOR);
			return this;
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (background != null) {
				g.setColor(background);
				g.fillRect(0, 0, getWidth(), getHeight());
			}
			int barHeight = 10;
			int barWidth = getWidth() - 14;
			int x = 7;
			int y = (getHeight() - barHeight) / 2;
			if (barWidth <= 0) {
				return;
			}
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(PROGRESS_TRACK_COLOR);
			g2.fillRoundRect(x, y, barWidth, barHeight, 5, 5);
			int fill = Math.round(barWidth * progress);
			if (fill > 0) {
				g2.setColor(PROGRESS_COLOR);
				g2.fillRoundRect(x, y, fill, barHeight, 5, 5);
			}
			String text = Math.round(progress * 100) + "%";
			g2.setColor(TEXT_COLOR);
			FontMetrics fm = g2.getFontMetrics();
			g2.drawString(text, x + Math.max(2, (barWidth - fm.stringWidth(text)) / 2),
				y + (barHeight + fm.getAscent()) / 2 - 1);
			g2.dispose();
		}

	}

	private static void paintZebra(DefaultTableCellRenderer renderer, JTable table, int row,
		boolean isSelected) {
		if (!isSelected) {
			renderer.setBackground(row % 2 == 0 ? Color.WHITE : ZEBRA_COLOR);
		}
	}

	private static final class ZebraRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
			Component cell = super.getTableCellRendererComponent(
				table, value, isSelected, hasFocus, row, column);
			paintZebra(this, table, row, isSelected);
			return cell;
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
