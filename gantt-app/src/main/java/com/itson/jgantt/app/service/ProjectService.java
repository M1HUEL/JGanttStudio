package com.itson.jgantt.app.service;

import java.util.List;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.ProjectSummaryDto;
import com.itson.jgantt.app.exception.ProjectNotFoundException;
import com.itson.jgantt.app.mapper.ProjectMapper;
import com.itson.jgantt.domain.entity.Project;
import com.itson.jgantt.domain.port.ProjectRepository;
import com.itson.jgantt.domain.valueobject.ProjectId;

public final class ProjectService {

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    public ProjectDto create(String name) {
        Project project = new Project(ProjectId.random(), name);
        ProjectId id = repository.save(project);
        return ProjectMapper.toDto(load(id));
    }

    public ProjectDto findById(ProjectId id) {
        return ProjectMapper.toDto(load(id));
    }

    public List<ProjectSummaryDto> list() {
        return repository.findAll().stream()
                .map(info -> new ProjectSummaryDto(info.id(), info.name()))
                .toList();
    }

    public ProjectDto rename(ProjectId id, String newName) {
        Project project = load(id);
        project.rename(newName);
        repository.save(project);
        return ProjectMapper.toDto(project);
    }

    private Project load(ProjectId id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }
}