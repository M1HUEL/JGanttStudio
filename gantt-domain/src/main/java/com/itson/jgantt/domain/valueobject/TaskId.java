package com.itson.jgantt.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record TaskId(UUID value) {

	public TaskId {
		Objects.requireNonNull(value, "value");
	}

	public static TaskId random() {
		return new TaskId(UUID.randomUUID());
	}
}
