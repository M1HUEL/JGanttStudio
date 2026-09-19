package com.itson.jgantt.domain.entity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import com.itson.jgantt.domain.exception.GanttDomainException;
import com.itson.jgantt.domain.valueobject.DateRange;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

public final class Project {

	private final ProjectId id;
	private String name;
	private final List<Task> tasks = new ArrayList<>();
	private final List<TaskLink> links = new ArrayList<>();
	private final Set<DayOfWeek> nonWorkingDays = new TreeSet<>();

	public Project(ProjectId id, String name) {
		this(id, name, List.of(), List.of());
	}

	public Project(ProjectId id, String name, List<Task> tasks, List<TaskLink> links) {
		this(id, name, tasks, links, defaultNonWorkingDays());
	}

	public Project(ProjectId id, String name, List<Task> tasks, List<TaskLink> links,
		Set<DayOfWeek> nonWorkingDays) {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(name, "name");
		if (name.isBlank()) {
			throw new GanttDomainException("Project name must not be blank");
		}
		this.id = id;
		this.name = name;

		Map<TaskId, Task> byId = new LinkedHashMap<>();
		for (Task task : tasks) {
			Objects.requireNonNull(task, "task");
			if (byId.put(task.id(), task) != null) {
				throw new GanttDomainException("Duplicate task id: " + task.id());
			}
		}
		for (Task task : tasks) {
			if (task.parentId() != null && !byId.containsKey(task.parentId())) {
				throw new GanttDomainException("Task " + task.id() + " references unknown parent " + task.parentId());
			}
		}
		for (TaskLink link : links) {
			Objects.requireNonNull(link, "link");
			if (!byId.containsKey(link.predecessorId())) {
				throw new GanttDomainException("Link references unknown predecessor " + link.predecessorId());
			}
			if (!byId.containsKey(link.successorId())) {
				throw new GanttDomainException("Link references unknown successor " + link.successorId());
			}
			if (link.predecessorId().equals(link.successorId())) {
				throw new GanttDomainException("A task cannot be linked to itself");
			}
		}
		this.tasks.addAll(tasks);
		this.links.addAll(links);
		for (DayOfWeek day : nonWorkingDays) {
			this.nonWorkingDays.add(Objects.requireNonNull(day, "day"));
		}
	}

	public ProjectId id() {
		return id;
	}

	public String name() {
		return name;
	}

	public void rename(String newName) {
		if (newName == null || newName.isBlank()) {
			throw new GanttDomainException("Project name must not be blank");
		}
		this.name = newName;
	}

	public List<Task> tasks() {
		return Collections.unmodifiableList(tasks);
	}

	public List<TaskLink> links() {
		return Collections.unmodifiableList(links);
	}

	public Set<DayOfWeek> nonWorkingDays() {
		return Collections.unmodifiableSet(nonWorkingDays);
	}

	public void addNonWorkingDay(DayOfWeek day) {
		nonWorkingDays.add(Objects.requireNonNull(day, "day"));
	}

	public void removeNonWorkingDay(DayOfWeek day) {
		nonWorkingDays.remove(Objects.requireNonNull(day, "day"));
	}

	public void replaceNonWorkingDays(Set<DayOfWeek> days) {
		nonWorkingDays.clear();
		for (DayOfWeek day : Objects.requireNonNull(days, "days")) {
			nonWorkingDays.add(Objects.requireNonNull(day, "day"));
		}
	}

	private static Set<DayOfWeek> defaultNonWorkingDays() {
		return EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
	}

	public Optional<Task> find(TaskId taskId) {
		for (Task task : tasks) {
			if (task.id().equals(taskId)) {
				return Optional.of(task);
			}
		}
		return Optional.empty();
	}

	public List<Task> childrenOf(TaskId parentId) {
		List<Task> children = new ArrayList<>();
		for (Task task : tasks) {
			if (parentId.equals(task.parentId())) {
				children.add(task);
			}
		}
		return children;
	}

	public int outlineLevel(TaskId taskId) {
		int level = 0;
		TaskId current = taskId;
		while (current != null) {
			Optional<Task> task = find(current);
			if (task.isEmpty()) {
				throw new GanttDomainException("Unknown task: " + taskId);
			}
			current = task.get().parentId();
			level++;
		}
		return level - 1;
	}

	public int countMilestones() {
		return (int) tasks.stream().filter(Task::isMilestone).count();
	}

	public void syncSummaryRanges() {
		Map<TaskId, Task> byId = new HashMap<>();
		for (Task task : tasks) {
			byId.put(task.id(), task);
		}
		int pass = 0;
		while (pass++ <= tasks.size()) {
			boolean changed = false;
			for (Task task : new ArrayList<>(tasks)) {
				if (task.isMilestone()) {
					continue;
				}
				LocalDate minStart = null;
				LocalDate maxEnd = null;
				for (Task child : tasks) {
					if (task.id().equals(child.parentId()) && !child.isMilestone()) {
						if (minStart == null || child.range().start().isBefore(minStart)) {
							minStart = child.range().start();
						}
						if (maxEnd == null || child.range().end().isAfter(maxEnd)) {
							maxEnd = child.range().end();
						}
					}
				}
				if (minStart == null) {
					continue;
				}
				Task current = byId.get(task.id());
				LocalDate wantedStart = current.range().start().isBefore(minStart) ? current.range().start() : minStart;
				LocalDate wantedEnd = current.range().end().isAfter(maxEnd) ? current.range().end() : maxEnd;
				DateRange wanted = new DateRange(wantedStart, wantedEnd);
				if (!wanted.equals(current.range())) {
					Task updated = current.withRange(wanted);
					replaceTask(updated);
					byId.put(updated.id(), updated);
					changed = true;
				}
			}
			if (!changed) {
				return;
			}
		}
	}

	public void addTask(Task task) {
		Objects.requireNonNull(task, "task");
		if (find(task.id()).isPresent()) {
			throw new GanttDomainException("Task already exists: " + task.id());
		}
		if (task.parentId() != null && find(task.parentId()).isEmpty()) {
			throw new GanttDomainException("Parent does not exist: " + task.parentId());
		}
		tasks.add(task);
	}

	public void updateTask(Task task) {
		Objects.requireNonNull(task, "task");
		if (find(task.id()).isEmpty()) {
			throw new GanttDomainException("Task does not exist: " + task.id());
		}
		if (task.parentId() != null) {
			if (task.parentId().equals(task.id())) {
				throw new GanttDomainException("A task cannot be its own parent");
			}
			if (find(task.parentId()).isEmpty()) {
				throw new GanttDomainException("Parent does not exist: " + task.parentId());
			}
			if (isDescendant(task.parentId(), task.id())) {
				throw new GanttDomainException("Task cannot be moved under its own descendant: " + task.parentId());
			}
		}
		replaceTask(task);
	}

	public void removeTask(TaskId taskId) {
		Objects.requireNonNull(taskId, "taskId");
		if (find(taskId).isEmpty()) {
			throw new GanttDomainException("Task does not exist: " + taskId);
		}
		Set<TaskId> toRemove = new HashSet<>();
		collectSubtree(taskId, toRemove);
		tasks.removeIf(task -> toRemove.contains(task.id()));
		links.removeIf(link -> toRemove.contains(link.predecessorId()) || toRemove.contains(link.successorId()));
	}

	public TaskType taskTypeOf(TaskId taskId) {
		return find(taskId).orElseThrow(() -> new GanttDomainException("Task does not exist: " + taskId)).type();
	}

	public void addLink(TaskLink link) {
		Objects.requireNonNull(link, "link");
		if (find(link.predecessorId()).isEmpty() || find(link.successorId()).isEmpty()) {
			throw new GanttDomainException("Link references a task that does not belong to this project");
		}
		if (hasLink(link.predecessorId(), link.successorId())) {
			throw new GanttDomainException("A link between these tasks already exists");
		}
		if (createsCycle(link)) {
			throw new GanttDomainException("This link would create a cycle");
		}
		links.add(link);
	}

	public void removeLink(TaskLink link) {
		Objects.requireNonNull(link, "link");
		if (!links.remove(link)) {
			throw new GanttDomainException("Link does not exist");
		}
	}

	public boolean hasLink(TaskId predecessorId, TaskId successorId) {
		for (TaskLink link : links) {
			if (link.predecessorId().equals(predecessorId) && link.successorId().equals(successorId)) {
				return true;
			}
		}
		return false;
	}

	private void replaceTask(Task task) {
		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).id().equals(task.id())) {
				tasks.set(i, task);
				return;
			}
		}
	}

	private boolean isDescendant(TaskId candidate, TaskId ancestor) {
		TaskId current = candidate;
		while (current != null) {
			if (current.equals(ancestor)) {
				return true;
			}
			Optional<Task> task = find(current);
			current = task.map(Task::parentId).orElse(null);
		}
		return false;
	}

	private void collectSubtree(TaskId taskId, Set<TaskId> acc) {
		if (!acc.add(taskId)) {
			return;
		}
		for (Task task : tasks) {
			if (taskId.equals(task.parentId())) {
				collectSubtree(task.id(), acc);
			}
		}
	}

	private boolean createsCycle(TaskLink candidate) {
		Deque<TaskId> queue = new ArrayDeque<>();
		Set<TaskId> visited = new HashSet<>();
		queue.add(candidate.successorId());
		while (!queue.isEmpty()) {
			TaskId node = queue.poll();
			if (node.equals(candidate.predecessorId())) {
				return true;
			}
			if (!visited.add(node)) {
				continue;
			}
			for (TaskLink link : links) {
				if (link.predecessorId().equals(node)) {
					queue.add(link.successorId());
				}
			}
		}
		return false;
	}

}
