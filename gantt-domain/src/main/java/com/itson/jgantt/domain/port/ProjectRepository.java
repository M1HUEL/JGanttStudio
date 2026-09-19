package com.itson.jgantt.domain.port;

import java.util.List;
import java.util.Optional;

import com.itson.jgantt.domain.entity.Project;
import com.itson.jgantt.domain.valueobject.ProjectId;

public interface ProjectRepository {

    Optional<Project> findById(ProjectId id);

    ProjectId save(Project project);

    void delete(ProjectId id);

    List<ProjectInfo> findAll();
}