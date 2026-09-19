package com.itson.jgantt.app.exception;

import com.itson.jgantt.domain.valueobject.TaskId;

public class TaskNotFoundException extends RuntimeException {

	public TaskNotFoundException(TaskId id) {
		super("Task not found: " + id);
	}

}
