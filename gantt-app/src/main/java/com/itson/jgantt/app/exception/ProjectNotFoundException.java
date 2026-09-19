package com.itson.jgantt.app.exception;

import com.itson.jgantt.domain.valueobject.ProjectId;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(ProjectId id) {
        super("Project not found: " + id);
    }
}