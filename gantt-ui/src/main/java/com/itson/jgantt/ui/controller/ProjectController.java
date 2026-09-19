package com.itson.jgantt.ui.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.request.AddTaskRequest;
import com.itson.jgantt.app.service.LinkService;
import com.itson.jgantt.app.service.ProjectService;
import com.itson.jgantt.app.service.TaskService;
import com.itson.jgantt.domain.valueobject.DependencyType;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;
import com.itson.jgantt.app.dto.ProjectSummaryDto;

public final class ProjectController {

	private final ProjectService projectService;
	private final TaskService taskService;
	private final LinkService linkService;
	private final List<Runnable> listeners = new ArrayList<>();
	private ProjectId currentId;
	private ProjectDto current;

	public ProjectController(ProjectService projectService, TaskService taskService, LinkService linkService) {
		this.projectService = projectService;
		this.taskService = taskService;
		this.linkService = linkService;
	}

	public ProjectDto current() {
		return current;
	}

	public boolean hasProject() {
		return currentId != null;
	}

	public void addListener(Runnable listener) {
		listeners.add(listener);
	}

	public List<ProjectSummaryDto> listProjects() {
		return projectService.list();
	}

	public void newProject(String name) {
		current = projectService.create(name);
		currentId = current.id();
		notifyListeners();
	}

	public void open(ProjectId id) {
		current = projectService.findById(id);
		currentId = id;
		notifyListeners();
	}

	public void renameProject(String newName) {
		if (currentId == null) {
			return;
		}
		current = projectService.rename(currentId, newName);
		notifyListeners();
	}

	public void updateNonWorkingDays(Set<LocalDate> nonWorkingDays) {
		if (currentId == null) {
			return;
		}
		current = projectService.updateNonWorkingDays(currentId, nonWorkingDays);
		notifyListeners();
	}

	public void refresh() {
		if (currentId == null) {
			return;
		}
		current = projectService.findById(currentId);
		notifyListeners();
	}

	public void addTask(String name) {
		current = taskService.addTask(currentId, new AddTaskRequest(name, java.time.LocalDate.now(),
			java.time.LocalDate.now().plusDays(1), TaskType.TASK, null));
		notifyListeners();
	}

	public void addSubtask(TaskId parentId) {
		current = taskService.addTask(currentId, new AddTaskRequest("New task", java.time.LocalDate.now(),
			java.time.LocalDate.now().plusDays(1), TaskType.TASK, parentId));
		notifyListeners();
	}

	public void addMilestone(String name, TaskId parentId) {
		LocalDate day = LocalDate.now();
		if (parentId != null) {
			day = current.taskOf(parentId).end();
		}
		current = taskService.addTask(currentId, new AddTaskRequest(name, day, day, TaskType.MILESTONE, parentId));
		notifyListeners();
	}

	public void deleteTask(TaskId taskId) {
		current = taskService.removeTask(currentId, taskId);
		notifyListeners();
	}

	public void renameTask(TaskId taskId, String newName) {
		current = taskService.renameTask(currentId, taskId, newName);
		notifyListeners();
	}

	public void changeStartDate(TaskId taskId, java.time.LocalDate newStart) {
		current = taskService.changeDates(currentId, taskId, newStart, current.taskOf(taskId).end());
		notifyListeners();
	}

	public void changeEndDate(TaskId taskId, java.time.LocalDate newEnd) {
		current = taskService.changeDates(currentId, taskId, current.taskOf(taskId).start(), newEnd);
		notifyListeners();
	}

	public void changeDates(TaskId taskId, java.time.LocalDate newStart, java.time.LocalDate newEnd) {
		current = taskService.changeDates(currentId, taskId, newStart, newEnd);
		notifyListeners();
	}

	public void setProgress(TaskId taskId, float progress) {
		current = taskService.setProgress(currentId, taskId, progress);
		notifyListeners();
	}

	public void linkFinishToStart(TaskId predecessorId, TaskId successorId) {
		current = linkService.addLink(currentId, predecessorId, successorId,
			DependencyType.FINISH_TO_START, Lag.ZERO);
		notifyListeners();
	}

	public void unlink(TaskId predecessorId, TaskId successorId) {
		current = linkService.removeLink(currentId, predecessorId, successorId);
		notifyListeners();
	}

	private void notifyListeners() {
		for (Runnable listener : listeners) {
			listener.run();
		}
	}

}
