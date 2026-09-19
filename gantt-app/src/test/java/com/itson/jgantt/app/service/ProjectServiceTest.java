package com.itson.jgantt.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.exception.ProjectNotFoundException;
import com.itson.jgantt.app.testutil.InMemoryProjectRepository;
import com.itson.jgantt.domain.valueobject.ProjectId;

class ProjectServiceTest {

	private static final LocalDate START = LocalDate.of(2026, 1, 1);

	private final InMemoryProjectRepository repository = new InMemoryProjectRepository();
	private final ProjectService projectService = new ProjectService(repository);

	@Test
	void createPersistsProject() {
		ProjectDto project = projectService.create("Build");

		assertTrue(repository.findById(project.id()).isPresent());
		assertEquals("Build", project.name());
	}

	@Test
	void createRejectsBlankName() {
		assertThrows(RuntimeException.class, () -> projectService.create("  "));
	}

	@Test
	void findByIdLoadsProject() {
		ProjectDto created = projectService.create("Build");
		ProjectDto loaded = projectService.findById(created.id());

		assertEquals(created, loaded);
	}

	@Test
	void findByIdThrowsForUnknownProject() {
		assertThrows(ProjectNotFoundException.class, () -> projectService.findById(ProjectId.random()));
	}

	@Test
	void listReturnsAllProjects() {
		projectService.create("Alpha");
		projectService.create("Beta");

		assertEquals(2, projectService.list().size());
		assertTrue(projectService.list().stream().anyMatch(p -> p.name().equals("Alpha")));
	}

	@Test
	void renameUpdatesProject() {
		ProjectDto created = projectService.create("Build");
		ProjectDto renamed = projectService.rename(created.id(), "Launch");

		assertEquals("Launch", renamed.name());
		assertEquals("Launch", repository.findById(created.id()).orElseThrow().name());
	}

	@Test
	void updateNonWorkingDaysReplacesProjectDays() {
		ProjectDto created = projectService.create("Build");
		Set<LocalDate> days = new HashSet<>(
			java.util.List.of(LocalDate.of(2026, 12, 24), LocalDate.of(2026, 12, 25)));

		ProjectDto updated = projectService.updateNonWorkingDays(created.id(), days);
		ProjectDto reloaded = projectService.findById(created.id());

		assertEquals(days, Set.copyOf(updated.nonWorkingDays()));
		assertEquals(days, Set.copyOf(reloaded.nonWorkingDays()));
	}

}
