package com.itson.jgantt.app.dto;

import java.time.LocalDate;

import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

public record TaskDto(
        TaskId id,
        String name,
        LocalDate start,
        LocalDate end,
        TaskType type,
        float progress,
        TaskId parentId,
        int outlineLevel,
        boolean milestone) {
}