package com.itson.jgantt.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.itson.jgantt.app.mapper.ProjectMapper;
import com.itson.jgantt.domain.entity.Project;
import com.itson.jgantt.domain.entity.Task;
import com.itson.jgantt.domain.entity.TaskLink;
import com.itson.jgantt.domain.valueobject.DependencyType;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;

class SQLiteProjectRepositoryTest {

	private static final LocalDate START = LocalDate.of(2026, 1, 1);

	@TempDir
	Path tempDir;

	private SQLiteProjectRepository repository;

	@BeforeEach
	void setUp() {
		repository = new SQLiteProjectRepository(tempDir.resolve("test.db").toString());
	}

	@Test
	void findAllReturnsStoredProjects() {
		repository.save(new Project(ProjectId.random(), "Zeta"));
		repository.save(new Project(ProjectId.random(), "Alpha"));

		List<String> names = repository.findAll().stream()
			.map(info -> info.name())
			.toList();

		assertEquals(List.of("Alpha", "Zeta"), names);
	}

	@Test
	void findByIdReturnsEmptyForMissingProject() {
		assertTrue(repository.findById(ProjectId.random()).isEmpty());
	}

	@Test
	void saveAndLoadEmptyProject() {
		Project project = new Project(ProjectId.random(), "Build");
		ProjectId id = repository.save(project);

		Optional<Project> loaded = repository.findById(id);

		assertTrue(loaded.isPresent());
		assertEquals(ProjectMapper.toDto(project), ProjectMapper.toDto(loaded.orElseThrow()));
	}

	@Test
	void roundTripPreservesHierarchyLinksAndProgress() {
		Project project = sampleProject();
		repository.save(project);

		Project loaded = repository.findById(project.id()).orElseThrow();

		assertEquals(ProjectMapper.toDto(project), ProjectMapper.toDto(loaded));
	}

	@Test
	void saveReplacesExistingProjectData() {
		Project project = sampleProject();
		repository.save(project);

		Project modifiable = repository.findById(project.id()).orElseThrow();
		Task first = modifiable.tasks().getFirst();
		modifiable.updateTask(first.withName("Design"));
		repository.save(modifiable);

		Project reloaded = repository.findById(project.id()).orElseThrow();

		assertEquals(3, reloaded.tasks().size());
		assertEquals("Design", reloaded.tasks().getFirst().name());
		assertEquals(1, reloaded.links().size());
	}

	@Test
	void preservesTaskOrdering() {
		Project project = new Project(ProjectId.random(), "Build");
		Task a = task("A").withRange(new com.itson.jgantt.domain.valueobject.DateRange(START, START.plusDays(1)));
		Task b = task("B").withRange(new com.itson.jgantt.domain.valueobject.DateRange(START, START.plusDays(1)));
		Task c = task("C").withRange(new com.itson.jgantt.domain.valueobject.DateRange(START, START.plusDays(1)));
		project.addTask(a);
		project.addTask(b);
		project.addTask(c);
		repository.save(project);

		Project loaded = repository.findById(project.id()).orElseThrow();

		List<String> names = loaded.tasks().stream().map(Task::name).toList();
		assertEquals(List.of("A", "B", "C"), names);
	}

	@Test
	void deleteRemovesProjectAndAssociatedRows() {
		Project project = sampleProject();
		repository.save(project);

		repository.delete(project.id());

		assertTrue(repository.findById(project.id()).isEmpty());
	}

	@Test
	void roundTripPreservesNonWorkingDays() {
		Project project = new Project(ProjectId.random(), "Launch");
		project.addNonWorkingDay(DayOfWeek.FRIDAY);
		project.addNonWorkingDay(DayOfWeek.THURSDAY);
		repository.save(project);

		Project loaded = repository.findById(project.id()).orElseThrow();

		assertEquals(Set.copyOf(project.nonWorkingDays()), Set.copyOf(loaded.nonWorkingDays()));
	}

	@Test
	void savedProjectCanReplaceNonWorkingDays() {
		Project project = new Project(ProjectId.random(), "Launch");
		project.addNonWorkingDay(DayOfWeek.TUESDAY);
		repository.save(project);

		Project modifiable = repository.findById(project.id()).orElseThrow();
		modifiable.replaceNonWorkingDays(Set.of(DayOfWeek.MONDAY));
		repository.save(modifiable);

		Project reloaded = repository.findById(project.id()).orElseThrow();

		assertEquals(Set.of(DayOfWeek.MONDAY), Set.copyOf(reloaded.nonWorkingDays()));
	}

	@Test
	void legacyDateRowIsMigratedToDayOfWeek() throws Exception {
		Project project = new Project(ProjectId.random(), "Launch");
		ProjectId id = repository.save(project);
		try (var connection = java.sql.DriverManager.getConnection(
			"jdbc:sqlite:" + tempDir.resolve("test.db").toString());
			var statement = connection.createStatement()) {
			statement.execute("INSERT INTO non_working_days (project_id, date) VALUES ('" + id.value() + "', '2026-05-01')");
		}

		Project loaded = repository.findById(id).orElseThrow();

		assertTrue(loaded.nonWorkingDays().contains(LocalDate.of(2026, 5, 1).getDayOfWeek()));
	}

	private Project sampleProject() {
		Project project = new Project(ProjectId.random(), "Launch");
		Task root = task("Phase 1");
		Task child = task("Task A").withRange(
			new com.itson.jgantt.domain.valueobject.DateRange(START, START.plusDays(4))).withParent(root.id());
		Task milestone = Task.builder()
			.id(TaskId.random())
			.name("Go live")
			.startsAt(START.plusDays(10))
			.endsAt(START.plusDays(10))
			.type(TaskType.MILESTONE)
			.progress(0.5f)
			.build();
		project.addTask(root);
		project.addTask(child);
		project.addTask(milestone);
		project.addLink(new TaskLink(child.id(), milestone.id(), DependencyType.START_TO_START, new Lag(1)));
		return project;
	}

	private Task task(String name) {
		return Task.builder()
			.id(TaskId.random())
			.name(name)
			.startsAt(START)
			.endsAt(START.plusDays(9))
			.build();
	}

}
