package com.itson.jgantt.app.dto;

import com.itson.jgantt.domain.valueobject.DependencyType;
import com.itson.jgantt.domain.valueobject.TaskId;

public record TaskLinkDto(
        TaskId predecessorId,
        TaskId successorId,
        DependencyType type,
        int lagDays) {
}