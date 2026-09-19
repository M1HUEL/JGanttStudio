package com.itson.jgantt.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

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

    private final ProjectController controller;
    private final TaskTablePanel tablePanel;
    private final GanttChartPanel chartPanel;
    private final JTextField projectNameField = new JTextField(20);
    private final JLabel statusLabel = new JLabel(" ");

    private JButton addSubtaskButton;
    private JButton deleteButton;
    private JButton linkButton;
    private JButton unlinkButton;

    public MainFrame(ProjectController controller) {
        super("JGanttStudio");
        this.controller = controller;
        this.tablePanel = new TaskTablePanel(editListener(), this::updateChart);
        this.chartPanel = new GanttChartPanel();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        tablePanel.getTable().getSelectionModel().addListSelectionListener(e -> updateButtons());

        setSize(new Dimension(1280, 760));
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
        split.setDividerLocation(480);
        split.setResizeWeight(0.25);
        return split;
    }

    private JButton button(String text, ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
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
        int selected = tablePanel.getSelectedTaskIds().size();
        addSubtaskButton.setEnabled(selected >= 1);
        deleteButton.setEnabled(selected >= 1);
        linkButton.setEnabled(selected >= 2);
        unlinkButton.setEnabled(selected >= 2);
    }

    private void runSafely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}