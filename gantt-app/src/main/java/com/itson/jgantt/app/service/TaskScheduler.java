package com.itson.jgantt.app.service;

import java.time.LocalDate;
import java.util.Optional;

import com.itson.jgantt.domain.entity.Project;
import com.itson.jgantt.domain.entity.Task;
import com.itson.jgantt.domain.entity.TaskLink;
import com.itson.jgantt.domain.valueobject.DateRange;
import com.itson.jgantt.domain.valueobject.TaskId;

public final class TaskScheduler {

    public void reschedule(Project project, TaskId changedTaskId) {
        recompute(project, changedTaskId);
    }

    private void recompute(Project project, TaskId taskId) {
        Task task = project.find(taskId).orElseThrow();
        earliestStart(project, task).ifPresent(earliest -> {
            if (task.range().start().isBefore(earliest)) {
                LocalDate newStart = earliest;
                LocalDate newEnd = newStart.plusDays(task.range().lengthInDays() - 1);
                project.updateTask(task.withRange(new DateRange(newStart, newEnd)));
            }
        });
        for (TaskLink link : project.links()) {
            if (link.predecessorId().equals(taskId)) {
                recompute(project, link.successorId());
            }
        }
    }

    private Optional<LocalDate> earliestStart(Project project, Task task) {
        LocalDate earliest = null;
        for (TaskLink link : project.links()) {
            if (!link.successorId().equals(task.id())) {
                continue;
            }
            Task predecessor = project.find(link.predecessorId()).orElseThrow();
            long duration = task.range().lengthInDays();
            LocalDate bound = switch (link.type()) {
                case FINISH_TO_START -> predecessor.range().end().plusDays(link.lag().days());
                case START_TO_START -> predecessor.range().start().plusDays(link.lag().days());
                case FINISH_TO_FINISH -> predecessor.range().end().plusDays(link.lag().days()).minusDays(duration - 1);
                case START_TO_FINISH -> predecessor.range().start().plusDays(link.lag().days()).minusDays(duration - 1);
            };
            if (earliest == null || bound.isAfter(earliest)) {
                earliest = bound;
            }
        }
        return Optional.ofNullable(earliest);
    }
}