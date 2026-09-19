package com.itson.jgantt.app.exception;

import com.itson.jgantt.domain.valueobject.TaskId;

public class TaskLinkNotFoundException extends RuntimeException {

	public TaskLinkNotFoundException(TaskId predecessorId, TaskId successorId) {
		super("Link not found: " + predecessorId + " -> " + successorId);
	}

}
