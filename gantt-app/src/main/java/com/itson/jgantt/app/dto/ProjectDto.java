package com.itson.jgantt.app.dto;

import java.util.List;

import com.itson.jgantt.domain.valueobject.ProjectId;

public record ProjectDto(
        ProjectId id,
        String name,
        List<TaskDto> tasks,
        List<TaskLinkDto> links) {
}