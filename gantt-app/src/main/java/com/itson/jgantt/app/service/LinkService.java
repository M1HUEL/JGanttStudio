package com.itson.jgantt.app.service;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.exception.ProjectNotFoundException;
import com.itson.jgantt.app.exception.TaskLinkNotFoundException;
import com.itson.jgantt.app.mapper.ProjectMapper;
import com.itson.jgantt.domain.entity.Project;
import com.itson.jgantt.domain.entity.TaskLink;
import com.itson.jgantt.domain.port.ProjectRepository;
import com.itson.jgantt.domain.valueobject.DependencyType;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;

public final class LinkService {

	private final ProjectRepository repository;
	private final TaskScheduler scheduler;

	public LinkService(ProjectRepository repository, TaskScheduler scheduler) {
		this.repository = repository;
		this.scheduler = scheduler;
	}

	public ProjectDto addLink(ProjectId projectId, TaskId predecessorId, TaskId successorId,
		DependencyType type, Lag lag) {
		Project project = load(projectId);
		TaskLink link = new TaskLink(predecessorId, successorId, type, lag);
		project.addLink(link);
		scheduler.reschedule(project, successorId);
		repository.save(project);
		return ProjectMapper.toDto(project);
	}

	public ProjectDto removeLink(ProjectId projectId, TaskId predecessorId, TaskId successorId) {
		Project project = load(projectId);
		TaskLink existing = project.links().stream()
			.filter(link -> link.predecessorId().equals(predecessorId)
			&& link.successorId().equals(successorId))
			.findFirst()
			.orElseThrow(() -> new TaskLinkNotFoundException(predecessorId, successorId));
		project.removeLink(existing);
		repository.save(project);
		return ProjectMapper.toDto(project);
	}

	private Project load(ProjectId id) {
		return repository.findById(id)
			.orElseThrow(() -> new ProjectNotFoundException(id));
	}

}
