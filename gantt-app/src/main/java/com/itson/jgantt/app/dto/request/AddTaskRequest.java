package com.itson.jgantt.app.dto.request;

import java.time.LocalDate;

import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

public record AddTaskRequest(
        String name,
        LocalDate start,
        LocalDate end,
        TaskType type,
        TaskId parentId) {

    public AddTaskRequest {
        if (type == null) {
            type = TaskType.TASK;
        }
    }
}