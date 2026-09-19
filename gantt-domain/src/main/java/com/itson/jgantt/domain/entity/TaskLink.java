package com.itson.jgantt.domain.entity;

import java.util.Objects;

import com.itson.jgantt.domain.exception.GanttDomainException;
import com.itson.jgantt.domain.valueobject.DependencyType;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.TaskId;

public record TaskLink(TaskId predecessorId, TaskId successorId, DependencyType type, Lag lag) {

	public TaskLink {
		Objects.requireNonNull(predecessorId, "predecessorId");
		Objects.requireNonNull(successorId, "successorId");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(lag, "lag");
		if (predecessorId.equals(successorId)) {
			throw new GanttDomainException("A task cannot be linked to itself");
		}
	}

	public static TaskLink finishToStart(TaskId predecessorId, TaskId successorId) {
		return new TaskLink(predecessorId, successorId, DependencyType.FINISH_TO_START, Lag.ZERO);
	}
}
