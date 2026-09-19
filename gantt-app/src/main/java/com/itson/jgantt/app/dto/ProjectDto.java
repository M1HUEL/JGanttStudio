package com.itson.jgantt.app.dto;

import java.time.DayOfWeek;
import java.util.List;

import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;

public record ProjectDto(
	ProjectId id,
	String name,
	List<TaskDto> tasks,
	List<TaskLinkDto> links,
	List<DayOfWeek> nonWorkingDays) {

	public TaskDto taskOf(TaskId taskId) {
		return tasks.stream()
			.filter(task -> task.id().equals(taskId))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown task " + taskId));
	}
}
