package com.itson.jgantt.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.itson.jgantt.domain.exception.GanttDomainException;
import com.itson.jgantt.domain.valueobject.DateRange;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

class TaskTest {

	private static final LocalDate START = LocalDate.of(2026, 1, 1);
	private static final LocalDate END = LocalDate.of(2026, 1, 10);

	@Test
	void buildsWithDefaults() {
		Task task = Task.builder()
			.id(TaskId.random())
			.name("Analysis")
			.startsAt(START)
			.endsAt(END)
			.build();

		assertEquals("Analysis", task.name());
		assertEquals(TaskType.TASK, task.type());
		assertEquals(0f, task.progress());
		assertNull(task.parentId());
		assertTrue(task.isRoot());
		assertFalse(task.isMilestone());
	}

	@Test
	void blankNameIsRejected() {
		assertThrows(GanttDomainException.class, () -> Task.builder()
			.id(TaskId.random())
			.name("  ")
			.startsAt(START)
			.endsAt(END)
			.build());
	}

	@Test
	void missingDatesAreRejected() {
		assertThrows(GanttDomainException.class, () -> Task.builder()
			.id(TaskId.random())
			.name("Analysis")
			.startsAt(START)
			.build());
	}

	@Test
	void reversedDatesAreRejected() {
		assertThrows(GanttDomainException.class, () -> Task.builder()
			.id(TaskId.random())
			.name("Analysis")
			.startsAt(END)
			.endsAt(START)
			.build());
	}

	@Test
	void milestoneRequiresPointRange() {
		assertThrows(GanttDomainException.class, () -> Task.builder()
			.id(TaskId.random())
			.name("Go live")
			.startsAt(START)
			.endsAt(END)
			.type(TaskType.MILESTONE)
			.build());
	}

	@Test
	void milestoneIsFlagged() {
		Task milestone = Task.builder()
			.id(TaskId.random())
			.name("Go live")
			.startsAt(START)
			.endsAt(START)
			.type(TaskType.MILESTONE)
			.build();

		assertTrue(milestone.isMilestone());
		assertEquals(1, milestone.range().lengthInDays());
	}

	@Test
	void progressMustBeWithinUnitInterval() {
		assertThrows(GanttDomainException.class, () -> Task.builder()
			.id(TaskId.random())
			.name("Analysis")
			.startsAt(START)
			.endsAt(END)
			.progress(-0.1f)
			.build());

		assertThrows(GanttDomainException.class, () -> Task.builder()
			.id(TaskId.random())
			.name("Analysis")
			.startsAt(START)
			.endsAt(END)
			.progress(1.1f)
			.build());
	}

	@Test
	void withMethodsProduceUpdatedCopies() {
		TaskId parent = TaskId.random();
		Task task = Task.builder()
			.id(TaskId.random())
			.name("Analysis")
			.startsAt(START)
			.endsAt(END)
			.build();

		Task renamed = task.withName("Design");
		Task moved = task.withRange(new DateRange(START.plusDays(1), START.plusDays(2)));
		Task linked = task.withParent(parent);

		assertEquals("Analysis", task.name());
		assertEquals("Design", renamed.name());
		assertNotNull(moved.range());
		assertEquals(parent, linked.parentId());
		assertFalse(linked.isRoot());
	}

	@Test
	void withParentCanClearParent() {
		Task root = task("Root");
		Task child = root.withParent(root.id());

		Task unparented = child.withParent(null);
		assertTrue(unparented.isRoot());
	}

	private static Task task(String name) {
		return Task.builder()
			.id(TaskId.random())
			.name(name)
			.startsAt(START)
			.endsAt(END)
			.build();
	}

}
