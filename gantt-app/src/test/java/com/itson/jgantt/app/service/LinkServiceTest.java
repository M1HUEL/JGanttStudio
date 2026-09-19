package com.itson.jgantt.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.app.dto.request.AddTaskRequest;
import com.itson.jgantt.app.testutil.InMemoryProjectRepository;
import com.itson.jgantt.domain.entity.TaskLink;
import com.itson.jgantt.domain.exception.GanttDomainException;
import com.itson.jgantt.domain.valueobject.DependencyType;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

class LinkServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    private final InMemoryProjectRepository repository = new InMemoryProjectRepository();
    private final TaskScheduler scheduler = new TaskScheduler();
    private final ProjectService projectService = new ProjectService(repository);
    private final TaskService taskService = new TaskService(repository, scheduler);
    private final LinkService linkService = new LinkService(repository, scheduler);

    @Test
    void addLinkPersistsAndReschedulesSuccessor() {
        ProjectId projectId = projectService.create("Build").id();
        TaskId a = addTask(projectId, "A", START, START.plusDays(4));
        TaskId b = addTask(projectId, "B", START, START.plusDays(1));

        ProjectDto after = linkService.addLink(projectId, a, b,
                DependencyType.FINISH_TO_START, Lag.ZERO);

        assertTrue(after.links().stream().anyMatch(link -> link.predecessorId().equals(a)
                && link.successorId().equals(b)));
        TaskDto bAfter = taskOf(after, b);
        assertEquals(START.plusDays(4), bAfter.start());
    }

    @Test
    void addLinkRejectsCycle() {
        ProjectId projectId = projectService.create("Build").id();
        TaskId a = addTask(projectId, "A", START, START);
        TaskId b = addTask(projectId, "B", START, START);
        TaskId c = addTask(projectId, "C", START, START);
        linkService.addLink(projectId, a, b, DependencyType.FINISH_TO_START, Lag.ZERO);
        linkService.addLink(projectId, b, c, DependencyType.FINISH_TO_START, Lag.ZERO);

        assertThrows(GanttDomainException.class, () ->
                linkService.addLink(projectId, c, a, DependencyType.FINISH_TO_START, Lag.ZERO));
    }

    @Test
    void removeLinkDeletesDependency() {
        ProjectId projectId = projectService.create("Build").id();
        TaskId a = addTask(projectId, "A", START, START.plusDays(4));
        TaskId b = addTask(projectId, "B", START, START.plusDays(1));
        linkService.addLink(projectId, a, b, DependencyType.FINISH_TO_START, Lag.ZERO);

        ProjectDto after = linkService.removeLink(projectId, a, b);

        assertTrue(after.links().isEmpty());
    }

    private TaskId addTask(ProjectId projectId, String name, LocalDate start, LocalDate end) {
        return taskService.addTask(projectId,
                new AddTaskRequest(name, start, end, TaskType.TASK, null))
                .tasks().stream()
                .filter(t -> t.name().equals(name))
                .findFirst().orElseThrow().id();
    }

    private TaskDto taskOf(ProjectDto dto, TaskId id) {
        return dto.tasks().stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow();
    }
}