package com.itson.jgantt.app.testutil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.itson.jgantt.domain.entity.Project;
import com.itson.jgantt.domain.port.ProjectInfo;
import com.itson.jgantt.domain.port.ProjectRepository;
import com.itson.jgantt.domain.valueobject.ProjectId;

public final class InMemoryProjectRepository implements ProjectRepository {

    private final Map<ProjectId, Project> store = new HashMap<>();

    @Override
    public Optional<Project> findById(ProjectId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public ProjectId save(Project project) {
        store.put(project.id(), project);
        return project.id();
    }

    @Override
    public void delete(ProjectId id) {
        store.remove(id);
    }

    @Override
    public List<ProjectInfo> findAll() {
        return store.values().stream()
                .map(project -> new ProjectInfo(project.id(), project.name()))
                .toList();
    }
}