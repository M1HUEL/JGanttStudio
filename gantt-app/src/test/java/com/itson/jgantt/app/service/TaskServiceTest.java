package com.itson.jgantt.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.app.dto.request.AddTaskRequest;
import com.itson.jgantt.app.testutil.InMemoryProjectRepository;
import com.itson.jgantt.domain.valueobject.DependencyType;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

class TaskServiceTest {

	private static final LocalDate START = LocalDate.of(2026, 1, 1);

	private final InMemoryProjectRepository repository = new InMemoryProjectRepository();
	private final TaskScheduler scheduler = new TaskScheduler();
	private final ProjectService projectService = new ProjectService(repository);
	private final TaskService taskService = new TaskService(repository, scheduler);
	private final LinkService linkService = new LinkService(repository, scheduler);

	@Test
	void addSubtaskExpandsParentRange() {
		ProjectId projectId = projectService.create("Build").id();
		ProjectDto root = taskService.addTask(projectId,
			new AddTaskRequest("Phase", START.plusDays(8), START.plusDays(10), TaskType.TASK, null));
		TaskId rootId = root.tasks().getFirst().id();

		ProjectDto result = taskService.addTask(projectId,
			new AddTaskRequest("Early", START.plusDays(7), START.plusDays(7), TaskType.TASK, rootId));

		TaskDto parent = result.tasks().stream()
			.filter(t -> t.id().equals(rootId))
			.findFirst().orElseThrow();
		assertEquals(START.plusDays(7), parent.start());
		assertEquals(START.plusDays(11), parent.end());
	}

	@Test
	void changeDatesExpandsParentRange() {
		ProjectId projectId = projectService.create("Build").id();
		ProjectDto root = taskService.addTask(projectId,
			new AddTaskRequest("Phase", START.plusDays(8), START.plusDays(12), TaskType.TASK, null));
		TaskId rootId = root.tasks().getFirst().id();
		ProjectDto added = taskService.addTask(projectId,
			new AddTaskRequest("Child", START.plusDays(9), START.plusDays(10), TaskType.TASK, rootId));
		TaskId childId = added.tasks().stream()
			.filter(t -> t.name().equals("Child"))
			.findFirst().orElseThrow().id();

		ProjectDto result = taskService.changeDates(projectId, childId, START.plusDays(5), START.plusDays(14));

		TaskDto parent = result.tasks().stream()
			.filter(t -> t.id().equals(rootId))
			.findFirst().orElseThrow();
		assertEquals(START.plusDays(5), parent.start());
		assertEquals(START.plusDays(14), parent.end());
	}

	@Test
	void addTaskPersistsTask() {
		ProjectId projectId = projectService.create("Build").id();

		ProjectDto result = taskService.addTask(projectId,
			new AddTaskRequest("Analysis", START, START.plusDays(4), TaskType.TASK, null));

		assertEquals(1, result.tasks().size());
		TaskDto task = result.tasks().getFirst();
		assertEquals("Analysis", task.name());
		assertEquals(START, task.start());
		assertEquals(START.plusDays(4), task.end());
		assertEquals(TaskType.TASK, task.type());
		assertEquals(0, task.outlineLevel());
	}

	@Test
	void changeDatesOnMilestoneForcesPointRange() {
		ProjectId projectId = projectService.create("Build").id();
		ProjectDto added = taskService.addTask(projectId,
			new AddTaskRequest("Go live", START.plusDays(11), START.plusDays(11), TaskType.MILESTONE, null));
		TaskId milestoneId = added.tasks().getFirst().id();

		ProjectDto result = taskService.changeDates(projectId, milestoneId, START.plusDays(14), START.plusDays(11));

		TaskDto milestone = result.tasks().getFirst();
		assertEquals(START.plusDays(14), milestone.start());
		assertEquals(START.plusDays(14), milestone.end());
	}

	@Test
	void addTaskSnapsStartAndEndToWorkingDays() {
		ProjectId projectId = projectService.create("Build").id();

		ProjectDto result = taskService.addTask(projectId,
			new AddTaskRequest("Weekend", START.plusDays(2), START.plusDays(3), TaskType.TASK, null));

		TaskDto task = result.tasks().getFirst();
		assertEquals(START.plusDays(4), task.start());
		assertEquals(START.plusDays(4), task.end());
	}

	@Test
	void addMilestoneSnapsToWorkingDay() {
		ProjectId projectId = projectService.create("Build").id();

		ProjectDto result = taskService.addTask(projectId,
			new AddTaskRequest("Go live", START.plusDays(3), START.plusDays(3), TaskType.MILESTONE, null));

		TaskDto milestone = result.tasks().getFirst();
		assertEquals(START.plusDays(4), milestone.start());
		assertEquals(START.plusDays(4), milestone.end());
	}

	@Test
	void changeDatesSnapsToWorkingDays() {
		ProjectId projectId = projectService.create("Build").id();
		TaskId taskId = addTask(projectId, "A", START, START.plusDays(4));

		ProjectDto result = taskService.changeDates(projectId, taskId, START.plusDays(2), START.plusDays(3));

		TaskDto task = result.tasks().stream().filter(t -> t.id().equals(taskId)).findFirst().orElseThrow();
		assertEquals(START.plusDays(4), task.start());
		assertEquals(START.plusDays(4), task.end());
	}

	@Test
	void addTaskAsChildComputesOutlineLevel() {
		ProjectId projectId = projectService.create("Build").id();
		ProjectDto root = taskService.addTask(projectId,
			new AddTaskRequest("Phase", START, START.plusDays(9), TaskType.TASK, null));
		TaskId rootId = root.tasks().getFirst().id();

		ProjectDto result = taskService.addTask(projectId,
			new AddTaskRequest("Task A", START, START.plusDays(3), TaskType.TASK, rootId));

		TaskDto child = result.tasks().stream()
			.filter(t -> t.name().equals("Task A"))
			.findFirst().orElseThrow();
		assertEquals(rootId, child.parentId());
		assertEquals(1, child.outlineLevel());
	}

	@Test
	void addMilestoneForcesPointRange() {
		ProjectId projectId = projectService.create("Build").id();

		ProjectDto result = taskService.addTask(projectId,
			new AddTaskRequest("Go live", START.plusDays(10), START.plusDays(10), TaskType.MILESTONE, null));

		TaskDto milestone = result.tasks().getFirst();
		assertTrue(milestone.milestone());
		assertEquals(START.plusDays(11), milestone.start());
	}

	@Test
	void renameTaskUpdatesName() {
		ProjectId projectId = projectService.create("Build").id();
		TaskId taskId = taskService.addTask(projectId,
			new AddTaskRequest("Analysis", START, START.plusDays(4), TaskType.TASK, null))
			.tasks().getFirst().id();

		ProjectDto result = taskService.renameTask(projectId, taskId, "Design");

		assertTrue(result.tasks().stream().anyMatch(t -> t.name().equals("Design")));
	}

	@Test
	void changeDatesReschedulesSuccessors() {
		ProjectId projectId = projectService.create("Build").id();
		TaskId a = addTask(projectId, "A", START, START.plusDays(4));
		TaskId b = addTask(projectId, "B", START, START.plusDays(2));
		linkService.addLink(projectId, a, b, DependencyType.FINISH_TO_START, Lag.ZERO);
		ProjectDto afterLink = taskService.changeDates(projectId, a, START.plusDays(10), START.plusDays(14));

		TaskDto bAfter = afterLink.tasks().stream().filter(t -> t.id().equals(b)).findFirst().orElseThrow();
		assertEquals(START.plusDays(14), bAfter.start());
	}

	@Test
	void setProgressUpdatesTask() {
		ProjectId projectId = projectService.create("Build").id();
		TaskId taskId = taskService.addTask(projectId,
			new AddTaskRequest("A", START, START.plusDays(4), TaskType.TASK, null))
			.tasks().getFirst().id();

		ProjectDto result = taskService.setProgress(projectId, taskId, 0.5f);

		assertEquals(0.5f, result.tasks().getFirst().progress());
	}

	@Test
	void reparentMovesTaskUnderNewParent() {
		ProjectId projectId = projectService.create("Build").id();
		TaskId root = addTask(projectId, "Phase", START, START.plusDays(9));
		TaskId task = addTask(projectId, "A", START, START.plusDays(4));

		ProjectDto result = taskService.reparent(projectId, task, root);

		TaskDto reparented = result.tasks().stream().filter(t -> t.id().equals(task)).findFirst().orElseThrow();
		assertEquals(root, reparented.parentId());
	}

	@Test
	void removeTaskRemovesSubtreeAndLinks() {
		ProjectId projectId = projectService.create("Build").id();
		TaskId root = addTask(projectId, "Phase", START, START.plusDays(9));
		TaskId child = addTask(projectId, "A", START, START.plusDays(4));
		taskService.reparent(projectId, child, root);
		linkService.addLink(projectId, root, child, DependencyType.FINISH_TO_START, Lag.ZERO);

		ProjectDto result = taskService.removeTask(projectId, root);

		assertTrue(result.tasks().isEmpty());
		assertTrue(result.links().isEmpty());
	}

	@Test
	void changeDatesOnUnknownTaskThrows() {
		ProjectId projectId = projectService.create("Build").id();
		assertThrowsTaskNotFound(()
			-> taskService.changeDates(projectId, TaskId.random(), START, START.plusDays(4)));
	}

	private TaskId addTask(ProjectId projectId, String name, LocalDate start, LocalDate end) {
		return taskService.addTask(projectId,
			new AddTaskRequest(name, start, end, TaskType.TASK, null))
			.tasks().stream()
			.filter(t -> t.name().equals(name))
			.findFirst().orElseThrow().id();
	}

	private void assertThrowsTaskNotFound(Runnable runnable) {
		try {
			runnable.run();
			throw new AssertionError("Expected TaskNotFoundException");
		} catch (com.itson.jgantt.app.exception.TaskNotFoundException expected) {
			assertFalse(false);
		}
	}

}
