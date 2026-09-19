package com.itson.jgantt.app.service;

import java.time.LocalDate;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.request.AddTaskRequest;
import com.itson.jgantt.app.exception.ProjectNotFoundException;
import com.itson.jgantt.app.exception.TaskNotFoundException;
import com.itson.jgantt.app.mapper.ProjectMapper;
import com.itson.jgantt.domain.entity.Project;
import com.itson.jgantt.domain.entity.Task;
import com.itson.jgantt.domain.port.ProjectRepository;
import com.itson.jgantt.domain.valueobject.DateRange;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

public final class TaskService {

	private final ProjectRepository repository;
	private final TaskScheduler scheduler;

	public TaskService(ProjectRepository repository, TaskScheduler scheduler) {
		this.repository = repository;
		this.scheduler = scheduler;
	}

	public ProjectDto addTask(ProjectId projectId, AddTaskRequest request) {
		Project project = load(projectId);
		Task task = Task.builder()
			.id(TaskId.random())
			.name(request.name())
			.startsAt(request.start())
			.endsAt(request.end())
			.type(request.type())
			.parent(request.parentId())
			.build();
		project.addTask(task);
		return save(project);
	}

	public ProjectDto renameTask(ProjectId projectId, TaskId taskId, String newName) {
		Project project = load(projectId);
		project.updateTask(findOrThrow(project, taskId).withName(newName));
		return save(project);
	}

	public ProjectDto changeDates(ProjectId projectId, TaskId taskId, LocalDate newStart, LocalDate newEnd) {
		Project project = load(projectId);
		project.updateTask(findOrThrow(project, taskId).withRange(new DateRange(newStart, newEnd)));
		scheduler.reschedule(project, taskId);
		return save(project);
	}

	public ProjectDto setProgress(ProjectId projectId, TaskId taskId, float progress) {
		Project project = load(projectId);
		project.updateTask(findOrThrow(project, taskId).withProgress(progress));
		return save(project);
	}

	public ProjectDto changeType(ProjectId projectId, TaskId taskId, TaskType type) {
		Project project = load(projectId);
		project.updateTask(findOrThrow(project, taskId).withType(type));
		return save(project);
	}

	public ProjectDto reparent(ProjectId projectId, TaskId taskId, TaskId newParentId) {
		Project project = load(projectId);
		project.updateTask(findOrThrow(project, taskId).withParent(newParentId));
		return save(project);
	}

	public ProjectDto removeTask(ProjectId projectId, TaskId taskId) {
		Project project = load(projectId);
		project.removeTask(taskId);
		return save(project);
	}

	private Task findOrThrow(Project project, TaskId taskId) {
		return project.find(taskId)
			.orElseThrow(() -> new TaskNotFoundException(taskId));
	}

	private ProjectDto save(Project project) {
		repository.save(project);
		return ProjectMapper.toDto(project);
	}

	private Project load(ProjectId id) {
		return repository.findById(id)
			.orElseThrow(() -> new ProjectNotFoundException(id));
	}

}
