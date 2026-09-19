package com.itson.jgantt.app.dto;

import com.itson.jgantt.domain.valueobject.ProjectId;

public record ProjectSummaryDto(ProjectId id, String name) {
}