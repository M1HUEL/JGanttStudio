package com.itson.jgantt.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.ProjectSummaryDto;
import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.ui.controller.ProjectController;
import com.itson.jgantt.ui.dialog.OpenProjectDialog;
import com.itson.jgantt.ui.model.TaskEditListener;
import com.itson.jgantt.ui.panel.GanttChartPanel;
import com.itson.jgantt.ui.panel.TaskTablePanel;

public final class MainFrame extends JFrame {

	private static final Color BUTTON_BG = new Color(0xFFFFFF);
	private static final Color BUTTON_HOVER_BG = new Color(0xE8EEF7);

	private final ProjectController controller;
	private final TaskTablePanel tablePanel;
	private final GanttChartPanel chartPanel;
	private final JTextField projectNameField = new JTextField(20);
	private final JLabel statusLabel = new JLabel(" ");

	private JButton addSubtaskButton;
	private JButton deleteButton;
	private JButton linkButton;
	private JButton unlinkButton;

	private JMenuItem addSubtaskItem;
	private JMenuItem deleteItem;
	private JMenuItem linkItem;
	private JMenuItem unlinkItem;

	public MainFrame(ProjectController controller) {
		super("JGanttStudio");
		this.controller = controller;
		this.tablePanel = new TaskTablePanel(editListener(), this::updateChart);
		this.chartPanel = new GanttChartPanel();
		chartPanel.setDragListener((taskId, newStart, newEnd)
			-> runSafely(() -> controller.changeDates(taskId, newStart, newEnd)));

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		setJMenuBar(buildMenuBar());
		add(buildToolbar(), BorderLayout.NORTH);
		add(buildCenter(), BorderLayout.CENTER);
		add(statusLabel, BorderLayout.SOUTH);

		tablePanel.getTable().getSelectionModel().addListSelectionListener(e -> updateButtons());

		setSize(new Dimension(1440, 900));
		setMinimumSize(new Dimension(1100, 700));
		setLocationByPlatform(true);

		controller.addListener(this::onProjectChanged);
	}

	public void initializeProject() {
		List<ProjectSummaryDto> projects = controller.listProjects();
		if (projects.isEmpty()) {
			controller.newProject("Untitled");
		} else {
			controller.open(projects.getFirst().id());
		}
	}

	private JMenuBar buildMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		fileMenu.add(menuItem("New project",
			KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), e -> newProject()));
		fileMenu.add(menuItem("Open project...",
			KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), e -> openProject()));
		fileMenu.add(menuItem("Save project",
			KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
			e -> runSafely(() -> controller.renameProject(projectNameField.getText()))));
		fileMenu.addSeparator();
		fileMenu.add(menuItem("Exit", e -> System.exit(0)));
		menuBar.add(fileMenu);

		JMenu editMenu = new JMenu("Edit");
		editMenu.add(menuItem("Add task",
			KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), e -> addTask()));
		addSubtaskItem = menuItem("Add subtask",
			KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
			e -> addSubtask());
		editMenu.add(addSubtaskItem);
		editMenu.add(menuItem("Add milestone",
			KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
			e -> addMilestone()));
		editMenu.addSeparator();
		deleteItem = menuItem("Delete task",
			KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), e -> deleteSelection());
		editMenu.add(deleteItem);
		menuBar.add(editMenu);

		JMenu linkMenu = new JMenu("Links");
		linkItem = menuItem("Link selected",
			KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), e -> linkSelection());
		linkMenu.add(linkItem);
		unlinkItem = menuItem("Unlink selected",
			KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
			e -> unlinkSelection());
		linkMenu.add(unlinkItem);
		menuBar.add(linkMenu);

		JMenu viewMenu = new JMenu("View");
		viewMenu.add(menuItem("Zoom in",
			KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
			e -> chartPanel.setDayWidth(Math.min(80, chartPanel.dayWidth() + 4))));
		viewMenu.add(menuItem("Zoom out",
			KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK),
			e -> chartPanel.setDayWidth(Math.max(6, chartPanel.dayWidth() - 4))));
		viewMenu.add(menuItem("Reset zoom",
			KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), e -> chartPanel.setDayWidth(18)));
		menuBar.add(viewMenu);

		return menuBar;
	}

	private JMenuItem menuItem(String text, ActionListener action) {
		return menuItem(text, null, action);
	}

	private JMenuItem menuItem(String text, KeyStroke accelerator, ActionListener action) {
		JMenuItem item = new JMenuItem(text);
		if (accelerator != null) {
			item.setAccelerator(accelerator);
		}
		item.addActionListener(action);
		return item;
	}

	private JToolBar buildToolbar() {
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);

		toolbar.add(new JLabel(" Project: "));
		projectNameField.addActionListener(e -> runSafely(() -> controller.renameProject(projectNameField.getText())));
		toolbar.add(projectNameField);

		toolbar.addSeparator();
		toolbar.add(button("New", e -> newProject()));
		toolbar.add(button("Open", e -> openProject()));
		toolbar.add(button("Save", e -> runSafely(() -> controller.renameProject(projectNameField.getText()))));

		toolbar.addSeparator();
		toolbar.add(button("Add task", e -> addTask()));
		addSubtaskButton = button("Add subtask", e -> addSubtask());
		toolbar.add(addSubtaskButton);
		toolbar.add(button("Add milestone", e -> addMilestone()));
		deleteButton = button("Delete", e -> deleteSelection());
		toolbar.add(deleteButton);

		toolbar.addSeparator();
		linkButton = button("Link", e -> linkSelection());
		unlinkButton = button("Unlink", e -> unlinkSelection());
		toolbar.add(linkButton);
		toolbar.add(unlinkButton);

		toolbar.addSeparator();
		toolbar.add(button("Zoom out", e -> chartPanel.setDayWidth(Math.max(6, chartPanel.dayWidth() - 4))));
		toolbar.add(button("Zoom in", e -> chartPanel.setDayWidth(Math.min(80, chartPanel.dayWidth() + 4))));

		return toolbar;
	}

	private JSplitPane buildCenter() {
		JScrollPane chartScroll = new JScrollPane(chartPanel);
		chartScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		tablePanel.getVerticalScrollBar().setModel(chartScroll.getVerticalScrollBar().getModel());

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tablePanel, chartScroll);
		split.setDividerLocation(560);
		split.setResizeWeight(0.28);
		return split;
	}

	private JButton button(String text, ActionListener action) {
		JButton button = new JButton(text);
		button.addActionListener(action);
		button.setFocusable(false);
		button.setMargin(new Insets(5, 12, 5, 12));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.putClientProperty("JButton.buttonType", "square");
		button.setBackground(BUTTON_BG);
		button.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				button.setBackground(BUTTON_HOVER_BG);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				button.setBackground(BUTTON_BG);
			}
		});
		return button;
	}

	private void newProject() {
		String name = JOptionPane.showInputDialog(this, "Project name:", "New project", JOptionPane.QUESTION_MESSAGE);
		if (name == null || name.isBlank()) {
			return;
		}
		runSafely(() -> controller.newProject(name));
	}

	private void openProject() {
		ProjectId id = OpenProjectDialog.show(this, controller.listProjects());
		if (id != null) {
			runSafely(() -> controller.open(id));
		}
	}

	private void addTask() {
		String name = JOptionPane.showInputDialog(this, "Task name:", "New task", JOptionPane.QUESTION_MESSAGE);
		if (name == null || name.isBlank()) {
			return;
		}
		runSafely(() -> controller.addTask(name));
	}

	private void addSubtask() {
		List<TaskId> selected = tablePanel.getSelectedTaskIds();
		if (selected.isEmpty()) {
			return;
		}
		runSafely(() -> controller.addSubtask(selected.getFirst()));
	}

	private void addMilestone() {
		String name = JOptionPane.showInputDialog(this, "Milestone name:", "Milestone", JOptionPane.QUESTION_MESSAGE);
		if (name == null || name.isBlank()) {
			return;
		}
		runSafely(() -> controller.addMilestone(name));
	}

	private void deleteSelection() {
		List<TaskId> selected = tablePanel.getSelectedTaskIds();
		if (selected.isEmpty()) {
			return;
		}
		for (TaskId taskId : selected) {
			runSafely(() -> controller.deleteTask(taskId));
		}
	}

	private void linkSelection() {
		List<TaskId> selected = tablePanel.getSelectedTaskIds();
		if (selected.size() < 2) {
			return;
		}
		runSafely(() -> controller.linkFinishToStart(selected.get(0), selected.get(1)));
	}

	private void unlinkSelection() {
		List<TaskId> selected = tablePanel.getSelectedTaskIds();
		if (selected.size() < 2) {
			return;
		}
		runSafely(() -> controller.unlink(selected.get(0), selected.get(1)));
	}

	private TaskEditListener editListener() {
		return new TaskEditListener() {

			@Override
			public void onRename(TaskId taskId, String newName) {
				runSafely(() -> controller.renameTask(taskId, newName));
			}

			@Override
			public void onStartChange(TaskId taskId, LocalDate newStart) {
				runSafely(() -> controller.changeStartDate(taskId, newStart));
			}

			@Override
			public void onEndChange(TaskId taskId, LocalDate newEnd) {
				runSafely(() -> controller.changeEndDate(taskId, newEnd));
			}

			@Override
			public void onProgressChange(TaskId taskId, float newProgress) {
				runSafely(() -> controller.setProgress(taskId, newProgress));
			}
		};
	}

	private void onProjectChanged() {
		if (!controller.hasProject()) {
			return;
		}
		ProjectDto dto = controller.current();
		tablePanel.setTasks(dto.tasks());
		projectNameField.setText(dto.name());
		updateChart();
		updateStatus();
		updateButtons();
	}

	private void updateChart() {
		if (!controller.hasProject()) {
			return;
		}
		ProjectDto dto = controller.current();
		chartPanel.setData(dto.tasks(), tablePanel.visibleTasks(), dto.links());
	}

	private void updateStatus() {
		if (!controller.hasProject()) {
			return;
		}
		ProjectDto dto = controller.current();
		long milestones = dto.tasks().stream().filter(TaskDto::milestone).count();
		LocalDate min = dto.tasks().stream().map(TaskDto::start)
			.min(LocalDate::compareTo).orElse(LocalDate.now());
		LocalDate max = dto.tasks().stream().map(TaskDto::end)
			.max(LocalDate::compareTo).orElse(LocalDate.now());
		statusLabel.setText(String.format("  %d tasks, %d links, %d milestones   |   %s .. %s",
			dto.tasks().size(), dto.links().size(), milestones, min, max));
	}

	private void updateButtons() {
		List<TaskId> selected = tablePanel.getSelectedTaskIds();
		boolean hasTask = selected.size() >= 1;
		boolean hasPair = selected.size() >= 2;
		addSubtaskButton.setEnabled(hasTask);
		addSubtaskItem.setEnabled(hasTask);
		deleteButton.setEnabled(hasTask);
		deleteItem.setEnabled(hasTask);
		linkButton.setEnabled(hasPair);
		linkItem.setEnabled(hasPair);
		unlinkButton.setEnabled(hasPair);
		unlinkItem.setEnabled(hasPair);
		chartPanel.setSelectedIds(new HashSet<>(selected));
	}

	private void runSafely(Runnable action) {
		try {
			action.run();
		} catch (RuntimeException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

}
