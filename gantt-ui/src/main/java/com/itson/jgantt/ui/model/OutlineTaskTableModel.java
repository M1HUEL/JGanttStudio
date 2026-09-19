package com.itson.jgantt.ui.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.ui.util.Messages;
import com.itson.jgantt.ui.util.TaskOutline;

public final class OutlineTaskTableModel extends AbstractTableModel {

	public static final int COL_NAME = 0;
	public static final int COL_START = 1;
	public static final int COL_END = 2;
	public static final int COL_LENGTH = 3;
	public static final int COL_PROGRESS = 4;

	private static final String[] COLUMNS = {"column.name", "column.start", "column.end", "column.length", "column.progress"};

	private final TaskEditListener listener;
	private List<TaskDto> allTasks = new ArrayList<>();
	private List<TaskDto> visible = new ArrayList<>();
	private final Set<TaskId> expanded = new LinkedHashSet<>();

	public OutlineTaskTableModel(TaskEditListener listener) {
		this.listener = listener;
	}

	public void setTasks(List<TaskDto> tasks) {
		this.allTasks = new ArrayList<>(tasks);
		Set<TaskId> currentIds = new LinkedHashSet<>();
		tasks.forEach(task -> currentIds.add(task.id()));
		expanded.retainAll(currentIds);
		expanded.addAll(currentIds);
		recompute();
	}

	public List<TaskDto> allTasks() {
		return allTasks;
	}

	public List<TaskDto> visibleTasks() {
		return visible;
	}

	public void toggleExpanded(int row) {
		TaskId id = visible.get(row).id();
		if (!expanded.remove(id)) {
			expanded.add(id);
		}
		recompute();
	}

	public boolean isExpanded(int row) {
		return expanded.contains(visible.get(row).id());
	}

	public TaskDto taskAt(int row) {
		return visible.get(row);
	}

	@Override
	public int getRowCount() {
		return visible.size();
	}

	@Override
	public int getColumnCount() {
		return COLUMNS.length;
	}

	@Override
	public String getColumnName(int column) {
		return Messages.get(COLUMNS[column]);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return switch (columnIndex) {
			case COL_NAME ->
				String.class;
			case COL_START, COL_END ->
				LocalDate.class;
			case COL_LENGTH ->
				Long.class;
			case COL_PROGRESS ->
				Float.class;
			default ->
				Object.class;
		};
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == COL_NAME
			|| columnIndex == COL_START
			|| columnIndex == COL_END
			|| columnIndex == COL_PROGRESS;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		TaskDto task = visible.get(rowIndex);
		return switch (columnIndex) {
			case COL_NAME ->
				task.name();
			case COL_START ->
				task.start();
			case COL_END ->
				task.end();
			case COL_LENGTH ->
				ChronoUnit.DAYS.between(task.start(), task.end()) + 1;
			case COL_PROGRESS ->
				task.progress();
			default ->
				"";
		};
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		TaskDto task = visible.get(rowIndex);
		switch (columnIndex) {
			case COL_NAME ->
				listener.onRename(task.id(), String.valueOf(value));
			case COL_START ->
				listener.onStartChange(task.id(), (LocalDate) value);
			case COL_END ->
				listener.onEndChange(task.id(), (LocalDate) value);
			case COL_PROGRESS ->
				listener.onProgressChange(task.id(), (Float) value);
			default -> {
				/* not editable */ }
		}
	}

	private void recompute() {
		visible = TaskOutline.visible(allTasks, expanded);
		fireTableDataChanged();
	}

}
