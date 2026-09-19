package com.itson.jgantt.app.mapper;

import java.util.List;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.app.dto.TaskLinkDto;
import com.itson.jgantt.domain.entity.Project;
import com.itson.jgantt.domain.entity.Task;
import com.itson.jgantt.domain.entity.TaskLink;
import com.itson.jgantt.domain.valueobject.DateRange;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectDto toDto(Project project) {
        List<TaskDto> tasks = project.tasks().stream()
                .map(task -> toTaskDto(project, task))
                .toList();
        List<TaskLinkDto> links = project.links().stream()
                .map(ProjectMapper::toTaskLinkDto)
                .toList();
        return new ProjectDto(project.id(), project.name(), tasks, links);
    }

    public static Project toEntity(ProjectDto dto) {
        List<Task> tasks = dto.tasks().stream()
                .map(taskDto -> toTask(taskDto))
                .toList();
        List<TaskLink> links = dto.links().stream()
                .map(linkDto -> new TaskLink(linkDto.predecessorId(), linkDto.successorId(),
                        linkDto.type(), new Lag(linkDto.lagDays())))
                .toList();
        return new Project(dto.id(), dto.name(), tasks, links);
    }

    private static TaskDto toTaskDto(Project project, Task task) {
        return new TaskDto(
                task.id(),
                task.name(),
                task.range().start(),
                task.range().end(),
                task.type(),
                task.progress(),
                task.parentId(),
                project.outlineLevel(task.id()),
                task.isMilestone());
    }

    private static TaskLinkDto toTaskLinkDto(TaskLink link) {
        return new TaskLinkDto(
                link.predecessorId(),
                link.successorId(),
                link.type(),
                link.lag().days());
    }

    private static Task toTask(TaskDto dto) {
        TaskId parentId = dto.parentId();
        TaskType type = dto.type() == null ? TaskType.TASK : dto.type();
        return Task.builder()
                .id(dto.id())
                .name(dto.name())
                .range(new DateRange(dto.start(), dto.end()))
                .type(type)
                .progress(dto.progress())
                .parent(parentId)
                .build();
    }
}