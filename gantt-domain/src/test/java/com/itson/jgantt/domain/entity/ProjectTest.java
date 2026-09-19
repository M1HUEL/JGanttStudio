package com.itson.jgantt.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.itson.jgantt.domain.exception.GanttDomainException;
import com.itson.jgantt.domain.valueobject.DependencyType;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

class ProjectTest {

	private static final LocalDate START = LocalDate.of(2026, 1, 1);

	@Test
	void newProjectStartsEmpty() {
		Project project = new Project(ProjectId.random(), "Build");
		assertTrue(project.tasks().isEmpty());
		assertTrue(project.links().isEmpty());
		assertEquals(0, project.countMilestones());
	}

	@Test
	void blankNameIsRejected() {
		assertThrows(GanttDomainException.class, () -> new Project(ProjectId.random(), "   "));
	}

	@Test
	void renameValidates() {
		Project project = new Project(ProjectId.random(), "Build");
		project.rename("Launch");
		assertEquals("Launch", project.name());
		assertThrows(GanttDomainException.class, () -> project.rename(" "));
	}

	@Test
	void addTaskSuccess() {
		Project project = new Project(ProjectId.random(), "Build");
		Task task = task("Analysis");
		project.addTask(task);

		assertEquals(List.of(task), project.tasks());
		assertEquals(0, project.outlineLevel(task.id()));
	}

	@Test
	void addTaskRejectsDuplicate() {
		Project project = new Project(ProjectId.random(), "Build");
		Task task = task("Analysis");
		project.addTask(task);
		assertThrows(GanttDomainException.class, () -> project.addTask(task));
	}

	@Test
	void addTaskRejectsUnknownParent() {
		Project project = new Project(ProjectId.random(), "Build");
		Task orphan = task("Analysis").withParent(TaskId.random());
		assertThrows(GanttDomainException.class, () -> project.addTask(orphan));
	}

	@Test
	void hierarchyAndOutlineLevels() {
		Project project = new Project(ProjectId.random(), "Build");
		Task root = task("Phase 1");
		Task child = task("Task A").withParent(root.id());
		Task grandchild = task("Subtask").withParent(child.id());

		project.addTask(root);
		project.addTask(child);
		project.addTask(grandchild);

		assertEquals(List.of(child), project.childrenOf(root.id()));
		assertEquals(0, project.outlineLevel(root.id()));
		assertEquals(1, project.outlineLevel(child.id()));
		assertEquals(2, project.outlineLevel(grandchild.id()));
	}

	@Test
	void updateTaskCannotBecomeOwnParent() {
		Project project = new Project(ProjectId.random(), "Build");
		Task root = task("Phase 1");
		project.addTask(root);

		assertThrows(GanttDomainException.class,
			() -> project.updateTask(root.withParent(root.id())));
	}

	@Test
	void updateTaskCannotMoveUnderOwnDescendant() {
		Project project = new Project(ProjectId.random(), "Build");
		Task root = task("Phase 1");
		Task child = task("Task A").withParent(root.id());
		Task grandchild = task("Subtask").withParent(child.id());
		project.addTask(root);
		project.addTask(child);
		project.addTask(grandchild);

		assertThrows(GanttDomainException.class,
			() -> project.updateTask(root.withParent(grandchild.id())));
	}

	@Test
	void updateTaskReparentingAllowed() {
		Project project = new Project(ProjectId.random(), "Build");
		Task a = task("A");
		Task b = task("B");
		project.addTask(a);
		project.addTask(b);

		project.updateTask(b.withParent(a.id()));
		assertEquals(1, project.outlineLevel(b.id()));
		assertEquals(List.of(b.id()), project.childrenOf(a.id()).stream().map(Task::id).toList());
	}

	@Test
	void removeTaskCascadesToSubtreeAndLinks() {
		Project project = new Project(ProjectId.random(), "Build");
		Task root = task("Phase 1");
		Task child = task("Task A").withParent(root.id());
		Task grandchild = task("Subtask").withParent(child.id());
		Task other = task("Other");
		project.addTask(root);
		project.addTask(child);
		project.addTask(grandchild);
		project.addTask(other);
		project.addLink(TaskLink.finishToStart(root.id(), other.id()));

		project.removeTask(root.id());

		assertEquals(List.of(other), project.tasks());
		assertTrue(project.links().isEmpty());
		assertTrue(project.find(child.id()).isEmpty());
	}

	@Test
	void removeTaskRejectsUnknownId() {
		Project project = new Project(ProjectId.random(), "Build");
		assertThrows(GanttDomainException.class, () -> project.removeTask(TaskId.random()));
	}

	@Test
	void addLinkValidatesExistence() {
		Project project = new Project(ProjectId.random(), "Build");
		Task a = task("A");
		Task b = task("B");
		project.addTask(a);

		assertThrows(GanttDomainException.class,
			() -> project.addLink(TaskLink.finishToStart(a.id(), b.id())));
	}

	@Test
	void addLinkRejectsDuplicate() {
		Project project = new Project(ProjectId.random(), "Build");
		Task a = task("A");
		Task b = task("B");
		project.addTask(a);
		project.addTask(b);
		TaskLink link = TaskLink.finishToStart(a.id(), b.id());
		project.addLink(link);

		assertThrows(GanttDomainException.class, () -> project.addLink(link));
		assertTrue(project.hasLink(a.id(), b.id()));
	}

	@Test
	void addLinkRejectsCycle() {
		Project project = new Project(ProjectId.random(), "Build");
		Task a = task("A");
		Task b = task("B");
		Task c = task("C");
		project.addTask(a);
		project.addTask(b);
		project.addTask(c);
		project.addLink(TaskLink.finishToStart(a.id(), b.id()));
		project.addLink(TaskLink.finishToStart(b.id(), c.id()));

		assertThrows(GanttDomainException.class,
			() -> project.addLink(TaskLink.finishToStart(c.id(), a.id())));
	}

	@Test
	void addLinkRoundTrip() {
		Project project = new Project(ProjectId.random(), "Build");
		Task a = task("A");
		Task b = task("B");
		project.addTask(a);
		project.addTask(b);

		TaskLink link = new TaskLink(a.id(), b.id(), DependencyType.START_TO_START, new Lag(2));
		project.addLink(link);

		assertEquals(List.of(link), project.links());
	}

	@Test
	void removeLinkRejectsUnknown() {
		Project project = new Project(ProjectId.random(), "Build");
		Task a = task("A");
		Task b = task("B");
		project.addTask(a);
		project.addTask(b);
		TaskLink link = TaskLink.finishToStart(a.id(), b.id());
		project.addLink(link);
		project.removeLink(link);

		assertThrows(GanttDomainException.class, () -> project.removeLink(link));
	}

	@Test
	void restoreConstructorValidatesDuplicates() {
		Task a = task("A");
		Task b = task("B").withId(a.id());
		assertThrows(GanttDomainException.class,
			() -> new Project(ProjectId.random(), "Build", List.of(a, b), List.of()));
	}

	@Test
	void restoreConstructorValidatesParents() {
		Task orphan = task("A").withParent(TaskId.random());
		assertThrows(GanttDomainException.class,
			() -> new Project(ProjectId.random(), "Build", List.of(orphan), List.of()));
	}

	@Test
	void restoreConstructorValidatesLinks() {
		Task a = task("A");
		Task b = task("B");
		TaskLink dangling = TaskLink.finishToStart(a.id(), b.id());
		assertThrows(GanttDomainException.class,
			() -> new Project(ProjectId.random(), "Build", List.of(a), List.of(dangling)));
	}

	@Test
	void restoreConstructorAcceptsValidState() {
		Task root = task("Phase 1");
		Task child = task("Task A").withParent(root.id());
		Task milestone = Task.builder()
			.id(TaskId.random())
			.name("Go live")
			.startsAt(START.plusDays(10))
			.endsAt(START.plusDays(10))
			.type(TaskType.MILESTONE)
			.build();
		TaskLink link = new TaskLink(child.id(), milestone.id(), DependencyType.FINISH_TO_START, Lag.ZERO);

		Project project = new Project(ProjectId.random(), "Build", List.of(root, child, milestone), List.of(link));

		assertEquals(3, project.tasks().size());
		assertEquals(List.of(link), project.links());
		assertEquals(1, project.countMilestones());
		assertFalse(project.countMilestones() > 1);
	}

	private static Task task(String name) {
		return Task.builder()
			.id(TaskId.random())
			.name(name)
			.startsAt(START)
			.endsAt(START.plusDays(4))
			.build();
	}

}
