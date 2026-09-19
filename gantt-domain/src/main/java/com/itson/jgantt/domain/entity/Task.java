package com.itson.jgantt.domain.entity;

import java.time.LocalDate;
import java.util.Objects;

import com.itson.jgantt.domain.exception.GanttDomainException;
import com.itson.jgantt.domain.valueobject.DateRange;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

public final class Task {

	private final TaskId id;
	private final String name;
	private final DateRange range;
	private final TaskType type;
	private final float progress;
	private final TaskId parentId;

	private Task(Builder builder) {
		this.id = builder.id;
		this.name = builder.name;
		this.range = new DateRange(builder.start, builder.end);
		this.type = builder.type;
		this.progress = builder.progress;
		this.parentId = builder.parentId;
	}

	public TaskId id() {
		return id;
	}

	public String name() {
		return name;
	}

	public DateRange range() {
		return range;
	}

	public TaskType type() {
		return type;
	}

	public float progress() {
		return progress;
	}

	public TaskId parentId() {
		return parentId;
	}

	public boolean isRoot() {
		return parentId == null;
	}

	public boolean isMilestone() {
		return type == TaskType.MILESTONE;
	}

	public Task withId(TaskId newId) {
		return new Builder().with(this).id(newId).build();
	}

	public Task withName(String newName) {
		return new Builder().with(this).name(newName).build();
	}

	public Task withRange(DateRange newRange) {
		return new Builder().with(this).range(newRange).build();
	}

	public Task withType(TaskType newType) {
		return new Builder().with(this).type(newType).build();
	}

	public Task withProgress(float newProgress) {
		return new Builder().with(this).progress(newProgress).build();
	}

	public Task withParent(TaskId newParentId) {
		return new Builder().with(this).parent(newParentId).build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private TaskId id;
		private String name;
		private LocalDate start;
		private LocalDate end;
		private TaskType type = TaskType.TASK;
		private float progress = 0f;
		private TaskId parentId;

		private Builder with(Task task) {
			this.id = task.id;
			this.name = task.name;
			this.start = task.range.start();
			this.end = task.range.end();
			this.type = task.type;
			this.progress = task.progress;
			this.parentId = task.parentId;
			return this;
		}

		public Builder id(TaskId id) {
			this.id = id;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder startsAt(LocalDate start) {
			this.start = start;
			return this;
		}

		public Builder endsAt(LocalDate end) {
			this.end = end;
			return this;
		}

		public Builder range(DateRange range) {
			this.start = range.start();
			this.end = range.end();
			return this;
		}

		public Builder type(TaskType type) {
			this.type = type;
			return this;
		}

		public Builder progress(float progress) {
			this.progress = progress;
			return this;
		}

		public Builder parent(TaskId parentId) {
			this.parentId = parentId;
			return this;
		}

		public Task build() {
			Objects.requireNonNull(id, "id");
			if (name == null || name.isBlank()) {
				throw new GanttDomainException("Task name must not be blank");
			}
			if (start == null || end == null) {
				throw new GanttDomainException("Task must have both a start and an end date");
			}
			if (progress < 0f || progress > 1f) {
				throw new GanttDomainException("Progress must be between 0 and 1, was " + progress);
			}
			DateRange range = new DateRange(start, end);
			if (type == TaskType.MILESTONE && !start.equals(end)) {
				throw new GanttDomainException("A milestone must have start == end");
			}
			return new Task(this);
		}

	}

}
