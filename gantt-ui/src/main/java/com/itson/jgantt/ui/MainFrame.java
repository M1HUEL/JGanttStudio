package com.itson.jgantt.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.ProjectSummaryDto;
import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.ui.controller.ProjectController;
import com.itson.jgantt.ui.dialog.NonWorkingDaysDialog;
import com.itson.jgantt.ui.dialog.OpenProjectDialog;
import com.itson.jgantt.ui.export.GanttExcelExporter;
import com.itson.jgantt.ui.export.GanttPdfExporter;
import com.itson.jgantt.ui.model.TaskEditListener;
import com.itson.jgantt.ui.panel.GanttChartPanel;
import com.itson.jgantt.ui.panel.TaskTablePanel;
import com.itson.jgantt.ui.panel.WelcomePanel;
import com.itson.jgantt.ui.util.Messages;

public final class MainFrame extends JFrame {

	private static final Color BUTTON_BG = new Color(0xFFFFFF);
	private static final Color BUTTON_HOVER_BG = new Color(0xE8EEF7);

	private final ProjectController controller;
	private final TaskTablePanel tablePanel;
	private final GanttChartPanel chartPanel;
	private final JTextField projectNameField = new JTextField(20);
	private final JLabel statusLabel = new JLabel(" ");
	private final CardLayout centerCards = new CardLayout();
	private final JPanel centerPanel = new JPanel(centerCards);
	private final WelcomePanel welcomePanel;

	private JToolBar toolbar;
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

		welcomePanel = new WelcomePanel(this::newProject, this::openProject);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		setJMenuBar(buildMenuBar());
		toolbar = buildToolbar();
		add(toolbar, BorderLayout.NORTH);

		centerPanel.add(buildSplitPane(), "main");
		centerPanel.add(welcomePanel, "welcome");
		add(centerPanel, BorderLayout.CENTER);
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
			centerCards.show(centerPanel, "welcome");
		} else {
			controller.open(projects.getFirst().id());
		}
	}

	private JMenuBar buildMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu(Messages.get("menu.file"));
		fileMenu.add(menuItem(Messages.get("action.newProject"),
			KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), e -> newProject()));
		fileMenu.add(menuItem(Messages.get("action.openProject"),
			KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), e -> openProject()));
		fileMenu.add(menuItem(Messages.get("action.saveProject"),
			KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
			e -> runSafely(() -> controller.renameProject(projectNameField.getText()))));
		fileMenu.addSeparator();
		fileMenu.add(menuItem(Messages.get("action.exit"), e -> System.exit(0)));
		menuBar.add(fileMenu);

		JMenu editMenu = new JMenu(Messages.get("menu.edit"));
		editMenu.add(menuItem(Messages.get("action.addTask"),
			KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), e -> addTask()));
		addSubtaskItem = menuItem(Messages.get("action.addSubtask"),
			KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
			e -> addSubtask());
		editMenu.add(addSubtaskItem);
		editMenu.add(menuItem(Messages.get("action.addMilestone"),
			KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
			e -> addMilestone()));
		editMenu.addSeparator();
		deleteItem = menuItem(Messages.get("action.deleteTask"),
			KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), e -> deleteSelection());
		editMenu.add(deleteItem);
		editMenu.addSeparator();
		editMenu.add(menuItem(Messages.get("action.nonWorkingDays"), e -> configureNonWorkingDays()));
		menuBar.add(editMenu);

		JMenu linkMenu = new JMenu(Messages.get("menu.links"));
		linkItem = menuItem(Messages.get("action.link"),
			KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), e -> linkSelection());
		linkMenu.add(linkItem);
		unlinkItem = menuItem(Messages.get("action.unlink"),
			KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
			e -> unlinkSelection());
		linkMenu.add(unlinkItem);
		menuBar.add(linkMenu);

		JMenu viewMenu = new JMenu(Messages.get("menu.view"));
		viewMenu.add(menuItem(Messages.get("action.zoomIn"),
			KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
			e -> chartPanel.setDayWidth(Math.min(80, chartPanel.dayWidth() + 4))));
		viewMenu.add(menuItem(Messages.get("action.zoomOut"),
			KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK),
			e -> chartPanel.setDayWidth(Math.max(6, chartPanel.dayWidth() - 4))));
		viewMenu.add(menuItem(Messages.get("action.resetZoom"),
			KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), e -> chartPanel.setDayWidth(18)));
		menuBar.add(viewMenu);

		JMenu exportMenu = new JMenu(Messages.get("menu.export"));
		exportMenu.add(menuItem(Messages.get("action.exportPdf"), e -> exportProjectPdf()));
		exportMenu.add(menuItem(Messages.get("action.exportExcel"), e -> exportProjectExcel()));
		menuBar.add(exportMenu);

		JMenu languageMenu = new JMenu(Messages.get("menu.language"));
		ButtonGroup group = new ButtonGroup();
		JRadioButtonMenuItem englishItem = new JRadioButtonMenuItem(Messages.get("language.english"));
		JRadioButtonMenuItem spanishItem = new JRadioButtonMenuItem(Messages.get("language.spanish"));
		boolean spanish = Messages.locale().getLanguage().startsWith("es");
		englishItem.setSelected(!spanish);
		spanishItem.setSelected(spanish);
		englishItem.addActionListener(e -> setLanguage(Locale.ENGLISH));
		spanishItem.addActionListener(e -> setLanguage(new Locale("es")));
		group.add(englishItem);
		group.add(spanishItem);
		languageMenu.add(englishItem);
		languageMenu.add(spanishItem);
		menuBar.add(languageMenu);

		return menuBar;
	}

	private void setLanguage(Locale newLocale) {
		if (Messages.locale().equals(newLocale)) {
			return;
		}
		Messages.setLocale(newLocale);
		applyLocale();
	}

	private void applyLocale() {
		setJMenuBar(buildMenuBar());
		JToolBar newToolbar = buildToolbar();
		getContentPane().remove(toolbar);
		add(newToolbar, BorderLayout.NORTH);
		toolbar = newToolbar;
		tablePanel.refreshColumnHeaders();
		welcomePanel.refreshTexts();
		chartPanel.repaint();
		updateStatus();
		validate();
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

		toolbar.add(new JLabel(Messages.get("toolbar.project") + " "));
		projectNameField.addActionListener(e -> runSafely(() -> controller.renameProject(projectNameField.getText())));
		toolbar.add(projectNameField);

		toolbar.addSeparator();
		toolbar.add(button(Messages.get("toolbar.new"), e -> newProject()));
		toolbar.add(button(Messages.get("toolbar.open"), e -> openProject()));
		toolbar.add(button(Messages.get("toolbar.save"),
			e -> runSafely(() -> controller.renameProject(projectNameField.getText()))));

		toolbar.addSeparator();
		toolbar.add(button(Messages.get("toolbar.addTask"), e -> addTask()));
		addSubtaskButton = button(Messages.get("toolbar.addSubtask"), e -> addSubtask());
		toolbar.add(addSubtaskButton);
		toolbar.add(button(Messages.get("toolbar.addMilestone"), e -> addMilestone()));
		deleteButton = button(Messages.get("toolbar.delete"), e -> deleteSelection());
		toolbar.add(deleteButton);

		toolbar.addSeparator();
		linkButton = button(Messages.get("toolbar.link"), e -> linkSelection());
		unlinkButton = button(Messages.get("toolbar.unlink"), e -> unlinkSelection());
		toolbar.add(linkButton);
		toolbar.add(unlinkButton);

		toolbar.addSeparator();
		toolbar.add(button("Zoom out", e -> chartPanel.setDayWidth(Math.max(6, chartPanel.dayWidth() - 4))));
		toolbar.add(button("Zoom in", e -> chartPanel.setDayWidth(Math.min(80, chartPanel.dayWidth() + 4))));

		return toolbar;
	}

	private JSplitPane buildSplitPane() {
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
		String name = JOptionPane.showInputDialog(this,
			Messages.get("dialog.newProject.message"),
			Messages.get("dialog.newProject.title"), JOptionPane.QUESTION_MESSAGE);
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

	private void configureNonWorkingDays() {
		ProjectDto dto = controller.current();
		if (dto == null) {
			return;
		}
		NonWorkingDaysDialog.show(this, dto.nonWorkingDays())
			.ifPresent(days -> runSafely(() -> controller.updateNonWorkingDays(days)));
	}

	private void exportProjectPdf() {
		ProjectDto dto = controller.current();
		if (dto == null) {
			return;
		}
		Path target = chooseExportFile(dto, ".pdf", Messages.get("export.filter.pdf"));
		if (target == null) {
			return;
		}
		BufferedImage image = chartPanel.toImage();
		runSafely(() -> {
			try {
				GanttPdfExporter.export(dto, image, target);
				statusLabel.setText(String.format(Messages.get("export.status"), target));
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	private void exportProjectExcel() {
		ProjectDto dto = controller.current();
		if (dto == null) {
			return;
		}
		Path target = chooseExportFile(dto, ".xlsx", Messages.get("export.filter.excel"));
		if (target == null) {
			return;
		}
		runSafely(() -> {
			try {
				GanttExcelExporter.export(dto, target);
				statusLabel.setText(String.format(Messages.get("export.status"), target));
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	private Path chooseExportFile(ProjectDto dto, String extension, String description) {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(Messages.get("menu.export"));
		chooser.setFileFilter(new FileNameExtensionFilter(description, extension.substring(1)));
		String safeName = dto.name().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
		if (safeName.isBlank()) {
			safeName = "project";
		}
		chooser.setSelectedFile(new File(safeName + extension));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		Path target = chooser.getSelectedFile().toPath();
		if (!target.getFileName().toString().toLowerCase().endsWith(extension)) {
			target = target.resolveSibling(target.getFileName() + extension);
		}
		return target;
	}

	private void addTask() {
		String name = JOptionPane.showInputDialog(this,
			Messages.get("dialog.newTask.message"),
			Messages.get("dialog.newTask.title"), JOptionPane.QUESTION_MESSAGE);
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
		String name = JOptionPane.showInputDialog(this,
			Messages.get("dialog.milestone.message"),
			Messages.get("dialog.milestone.title"), JOptionPane.QUESTION_MESSAGE);
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
		centerCards.show(centerPanel, "main");
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
		chartPanel.setData(dto.tasks(), tablePanel.visibleTasks(), dto.links(),
			Set.copyOf(dto.nonWorkingDays()));
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
		statusLabel.setText(String.format(Messages.get("status.summary"),
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
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), Messages.get("dialog.error"), JOptionPane.ERROR_MESSAGE);
		}
	}

}
