package com.itson.jgantt.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.app.dto.request.AddTaskRequest;
import com.itson.jgantt.app.exception.ProjectNotFoundException;
import com.itson.jgantt.app.exception.TaskNotFoundException;
import com.itson.jgantt.app.testutil.InMemoryProjectRepository;
import com.itson.jgantt.domain.valueobject.DependencyType;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

class ProjectServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private final InMemoryProjectRepository repository = new InMemoryProjectRepository();
    private final ProjectService projectService = new ProjectService(repository);

    @Test
    void createPersistsProject() {
        ProjectDto project = projectService.create("Build");

        assertTrue(repository.findById(project.id()).isPresent());
        assertEquals("Build", project.name());
    }

    @Test
    void createRejectsBlankName() {
        assertThrows(RuntimeException.class, () -> projectService.create("  "));
    }

    @Test
    void findByIdLoadsProject() {
        ProjectDto created = projectService.create("Build");
        ProjectDto loaded = projectService.findById(created.id());

        assertEquals(created, loaded);
    }

    @Test
    void findByIdThrowsForUnknownProject() {
        assertThrows(ProjectNotFoundException.class, () -> projectService.findById(ProjectId.random()));
    }

    @Test
    void renameUpdatesProject() {
        ProjectDto created = projectService.create("Build");
        ProjectDto renamed = projectService.rename(created.id(), "Launch");

        assertEquals("Launch", renamed.name());
        assertEquals("Launch", repository.findById(created.id()).orElseThrow().name());
    }
}