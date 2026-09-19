package com.itson.jgantt.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.itson.jgantt.domain.entity.Project;
import com.itson.jgantt.domain.entity.Task;
import com.itson.jgantt.domain.entity.TaskLink;
import com.itson.jgantt.domain.valueobject.DependencyType;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;

class TaskSchedulerTest {

	private static final LocalDate BASE = LocalDate.of(2026, 1, 1);

	private final TaskScheduler scheduler = new TaskScheduler();

	@Test
	void finishToStartShiftsSuccessor() {
		Project project = emptyProject();
		Task predecessor = task("A", 1, 5);
		Task successor = task("B", 3, 3);
		project.addTask(predecessor);
		project.addTask(successor);
		project.addLink(TaskLink.finishToStart(predecessor.id(), successor.id()));

		scheduler.reschedule(project, successor.id());

		assertEquals(BASE.plusDays(4), project.find(successor.id()).orElseThrow().range().start());
		assertEquals(BASE.plusDays(4), project.find(successor.id()).orElseThrow().range().end());
	}

	@Test
	void finishToStartWithLagShiftsSuccessor() {
		Project project = emptyProject();
		Task predecessor = task("A", 1, 5);
		Task successor = task("B", 3, 3);
		project.addTask(predecessor);
		project.addTask(successor);
		project.addLink(new TaskLink(predecessor.id(), successor.id(),
			DependencyType.FINISH_TO_START, new Lag(2)));

		scheduler.reschedule(project, successor.id());

		assertEquals(BASE.plusDays(6), project.find(successor.id()).orElseThrow().range().start());
	}

	@Test
	void finishToStartDoesNotPullEarlier() {
		Project project = emptyProject();
		Task predecessor = task("A", 1, 5);
		Task successor = task("B", 10, 12);
		project.addTask(predecessor);
		project.addTask(successor);
		project.addLink(TaskLink.finishToStart(predecessor.id(), successor.id()));

		scheduler.reschedule(project, successor.id());

		assertEquals(BASE.plusDays(9), project.find(successor.id()).orElseThrow().range().start());
	}

	@Test
	void cascadePropagatesThroughChain() {
		Project project = emptyProject();
		Task a = task("A", 1, 3);
		Task b = task("B", 2, 4);
		Task c = task("C", 3, 5);
		project.addTask(a);
		project.addTask(b);
		project.addTask(c);
		project.addLink(TaskLink.finishToStart(a.id(), b.id()));
		project.addLink(TaskLink.finishToStart(b.id(), c.id()));

		scheduler.reschedule(project, a.id());

		Task bAfter = project.find(b.id()).orElseThrow();
		Task cAfter = project.find(c.id()).orElseThrow();
		assertEquals(BASE.plusDays(2), bAfter.range().start());
		assertEquals(BASE.plusDays(4), bAfter.range().end());
		assertEquals(BASE.plusDays(4), cAfter.range().start());
		assertEquals(BASE.plusDays(6), cAfter.range().end());
	}

	@Test
	void startToStartUsesPredecessorStart() {
		Project project = emptyProject();
		Task predecessor = task("A", 5, 10);
		Task successor = task("B", 1, 1);
		project.addTask(predecessor);
		project.addTask(successor);
		project.addLink(new TaskLink(predecessor.id(), successor.id(),
			DependencyType.START_TO_START, Lag.ZERO));

		scheduler.reschedule(project, successor.id());

		assertEquals(BASE.plusDays(4), project.find(successor.id()).orElseThrow().range().start());
	}

	@Test
	void finishToFinishUsesPredecessorEndAndKeepsDuration() {
		Project project = emptyProject();
		Task predecessor = task("A", 1, 10);
		Task successor = task("B", 3, 5);
		project.addTask(predecessor);
		project.addTask(successor);
		project.addLink(new TaskLink(predecessor.id(), successor.id(),
			DependencyType.FINISH_TO_FINISH, Lag.ZERO));

		scheduler.reschedule(project, successor.id());

		Task after = project.find(successor.id()).orElseThrow();
		assertEquals(BASE.plusDays(7), after.range().start());
		assertEquals(BASE.plusDays(9), after.range().end());
	}

	@Test
	void startToFinishUsesPredecessorStartAndKeepsDuration() {
		Project project = emptyProject();
		Task predecessor = task("A", 5, 10);
		Task successor = task("B", 2, 4);
		project.addTask(predecessor);
		project.addTask(successor);
		project.addLink(new TaskLink(predecessor.id(), successor.id(),
			DependencyType.START_TO_FINISH, Lag.ZERO));

		scheduler.reschedule(project, successor.id());

		Task after = project.find(successor.id()).orElseThrow();
		assertEquals(BASE.plusDays(2), after.range().start());
		assertEquals(BASE.plusDays(4), after.range().end());
	}

	private static Project emptyProject() {
		return new Project(ProjectId.random(), "Build");
	}

	private static Task task(String name, int startDay, int endDay) {
		return Task.builder()
			.id(TaskId.random())
			.name(name)
			.startsAt(BASE.plusDays(startDay - 1))
			.endsAt(BASE.plusDays(endDay - 1))
			.build();
	}

}
