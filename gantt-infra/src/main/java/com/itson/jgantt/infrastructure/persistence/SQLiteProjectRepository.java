package com.itson.jgantt.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.itson.jgantt.domain.entity.Project;
import com.itson.jgantt.domain.entity.Task;
import com.itson.jgantt.domain.entity.TaskLink;
import com.itson.jgantt.domain.port.ProjectInfo;
import com.itson.jgantt.domain.port.ProjectRepository;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;
import com.itson.jgantt.infrastructure.exception.InfrastructureException;

public final class SQLiteProjectRepository implements ProjectRepository {

	private static final String DDL_PROJECTS = """
            CREATE TABLE IF NOT EXISTS projects (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL
            )""";

	private static final String DDL_TASKS = """
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY,
                project_id TEXT NOT NULL,
                name TEXT NOT NULL,
                start_date TEXT NOT NULL,
                end_date TEXT NOT NULL,
                type TEXT NOT NULL,
                progress REAL NOT NULL,
                parent_id TEXT,
                position INTEGER NOT NULL,
                FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                FOREIGN KEY (parent_id) REFERENCES tasks(id) ON DELETE SET NULL
            )""";

	private static final String DDL_LINKS = """
            CREATE TABLE IF NOT EXISTS task_links (
                predecessor_id TEXT NOT NULL,
                successor_id TEXT NOT NULL,
                type TEXT NOT NULL,
                lag_days INTEGER NOT NULL,
                PRIMARY KEY (predecessor_id, successor_id),
                FOREIGN KEY (predecessor_id) REFERENCES tasks(id) ON DELETE CASCADE,
                FOREIGN KEY (successor_id) REFERENCES tasks(id) ON DELETE CASCADE
            )""";

	private static final String DDL_NON_WORKING_DAYS = """
            CREATE TABLE IF NOT EXISTS non_working_days (
                project_id TEXT NOT NULL,
                date TEXT NOT NULL,
                PRIMARY KEY (project_id, date),
                FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
            )""";

	private final String url;

	public SQLiteProjectRepository(String databasePath) {
		this.url = "jdbc:sqlite:" + databasePath;
		initialize();
	}

	@Override
	public Optional<Project> findById(ProjectId id) {
		try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(
			"SELECT id, name FROM projects WHERE id = ?")) {
			ps.setString(1, id.value().toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return Optional.empty();
				}
				String projectName = rs.getString("name");
				List<Task> tasks = loadTasks(connection, id);
				List<TaskLink> links = loadLinks(connection, id);
				Set<DayOfWeek> nonWorkingDays = loadNonWorkingDays(connection, id);
				return Optional.of(new Project(id, projectName, tasks, links, nonWorkingDays));
			}
		} catch (SQLException ex) {
			throw new InfrastructureException("Failed to load project " + id, ex);
		}
	}

	@Override
	public ProjectId save(Project project) {
		try (Connection connection = connect()) {
			connection.setAutoCommit(false);
			try {
				upsertProject(connection, project);
				replaceTasks(connection, project);
				replaceLinks(connection, project);
				replaceNonWorkingDays(connection, project);
				connection.commit();
			} catch (SQLException ex) {
				connection.rollback();
				throw ex;
			}
			return project.id();
		} catch (SQLException ex) {
			throw new InfrastructureException("Failed to save project " + project.id(), ex);
		}
	}

	@Override
	public void delete(ProjectId id) {
		try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement("DELETE FROM projects WHERE id = ?")) {
			ps.setString(1, id.value().toString());
			ps.executeUpdate();
		} catch (SQLException ex) {
			throw new InfrastructureException("Failed to delete project " + id, ex);
		}
	}

	@Override
	public List<ProjectInfo> findAll() {
		try (Connection connection = connect(); PreparedStatement ps = connection.prepareStatement(
			"SELECT id, name FROM projects ORDER BY name"); ResultSet rs = ps.executeQuery()) {
			List<ProjectInfo> projects = new ArrayList<>();
			while (rs.next()) {
				projects.add(new ProjectInfo(
					new ProjectId(UUID.fromString(rs.getString("id"))),
					rs.getString("name")));
			}
			return projects;
		} catch (SQLException ex) {
			throw new InfrastructureException("Failed to list projects", ex);
		}
	}

	private void initialize() {
		try (Connection connection = connect(); Statement statement = connection.createStatement()) {
			statement.execute(DDL_PROJECTS);
			statement.execute(DDL_TASKS);
			statement.execute(DDL_LINKS);
			statement.execute(DDL_NON_WORKING_DAYS);
		} catch (SQLException ex) {
			throw new InfrastructureException("Failed to initialize database at " + url, ex);
		}
	}

	private Connection connect() throws SQLException {
		Connection connection = DriverManager.getConnection(url);
		try (Statement statement = connection.createStatement()) {
			statement.execute("PRAGMA foreign_keys = ON");
		}
		return connection;
	}

	private void upsertProject(Connection connection, Project project) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(
			"INSERT OR REPLACE INTO projects (id, name) VALUES (?, ?)")) {
			ps.setString(1, project.id().value().toString());
			ps.setString(2, project.name());
			ps.executeUpdate();
		}
	}

	private void replaceTasks(Connection connection, Project project) throws SQLException {
		try (PreparedStatement delete = connection.prepareStatement(
			"DELETE FROM tasks WHERE project_id = ?")) {
			delete.setString(1, project.id().value().toString());
			delete.executeUpdate();
		}

		try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO tasks (id, project_id, name, start_date, end_date, type, progress, parent_id, position)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""")) {
			int position = 0;
			for (Task task : project.tasks()) {
				insert.setString(1, task.id().value().toString());
				insert.setString(2, project.id().value().toString());
				insert.setString(3, task.name());
				insert.setString(4, task.range().start().toString());
				insert.setString(5, task.range().end().toString());
				insert.setString(6, task.type().name());
				insert.setFloat(7, task.progress());
				if (task.parentId() == null) {
					insert.setNull(8, java.sql.Types.VARCHAR);
				} else {
					insert.setString(8, task.parentId().value().toString());
				}
				insert.setInt(9, position++);
				insert.addBatch();
			}
			insert.executeBatch();
		}
	}

	private void replaceLinks(Connection connection, Project project) throws SQLException {
		try (PreparedStatement delete = connection.prepareStatement(
			"DELETE FROM task_links WHERE predecessor_id IN (SELECT id FROM tasks WHERE project_id = ?)")) {
			delete.setString(1, project.id().value().toString());
			delete.executeUpdate();
		}

		try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO task_links (predecessor_id, successor_id, type, lag_days)
                VALUES (?, ?, ?, ?)""")) {
			for (TaskLink link : project.links()) {
				insert.setString(1, link.predecessorId().value().toString());
				insert.setString(2, link.successorId().value().toString());
				insert.setString(3, link.type().name());
				insert.setInt(4, link.lag().days());
				insert.addBatch();
			}
			insert.executeBatch();
		}
	}

	private void replaceNonWorkingDays(Connection connection, Project project) throws SQLException {
		try (PreparedStatement delete = connection.prepareStatement(
			"DELETE FROM non_working_days WHERE project_id = ?")) {
			delete.setString(1, project.id().value().toString());
			delete.executeUpdate();
		}

		try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO non_working_days (project_id, date)
                VALUES (?, ?)""")) {
			for (DayOfWeek day : project.nonWorkingDays()) {
				insert.setString(1, project.id().value().toString());
				insert.setString(2, day.name());
				insert.addBatch();
			}
			insert.executeBatch();
		}
	}

	private Set<DayOfWeek> loadNonWorkingDays(Connection connection, ProjectId projectId) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("""
                SELECT date FROM non_working_days WHERE project_id = ?
                ORDER BY date""")) {
			ps.setString(1, projectId.value().toString());
			try (ResultSet rs = ps.executeQuery()) {
				Set<DayOfWeek> days = new TreeSet<>();
				while (rs.next()) {
					days.add(parseNonWorkingDay(rs.getString("date")));
				}
				return days;
			}
		}
	}

	private static DayOfWeek parseNonWorkingDay(String raw) {
		try {
			return LocalDate.parse(raw).getDayOfWeek();
		} catch (DateTimeParseException legacy) {
			return DayOfWeek.valueOf(raw);
		}
	}

	private List<Task> loadTasks(Connection connection, ProjectId projectId) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("""
                SELECT id, name, start_date, end_date, type, progress, parent_id
                FROM tasks WHERE project_id = ? ORDER BY position""")) {
			ps.setString(1, projectId.value().toString());
			try (ResultSet rs = ps.executeQuery()) {
				List<Task> tasks = new ArrayList<>();
				while (rs.next()) {
					TaskId parentId = rs.getString("parent_id") == null
						? null
						: new TaskId(UUID.fromString(rs.getString("parent_id")));
					Task task = Task.builder()
						.id(new TaskId(UUID.fromString(rs.getString("id"))))
						.name(rs.getString("name"))
						.startsAt(LocalDate.parse(rs.getString("start_date")))
						.endsAt(LocalDate.parse(rs.getString("end_date")))
						.type(ofType(rs.getString("type")))
						.progress(rs.getFloat("progress"))
						.parent(parentId)
						.build();
					tasks.add(task);
				}
				return tasks;
			}
		}
	}

	private List<TaskLink> loadLinks(Connection connection, ProjectId projectId) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("""
                SELECT l.predecessor_id, l.successor_id, l.type, l.lag_days
                FROM task_links l
                JOIN tasks t ON t.id = l.predecessor_id
                WHERE t.project_id = ?
                ORDER BY l.predecessor_id, l.successor_id""")) {
			ps.setString(1, projectId.value().toString());
			try (ResultSet rs = ps.executeQuery()) {
				List<TaskLink> links = new ArrayList<>();
				while (rs.next()) {
					links.add(new TaskLink(
						new TaskId(UUID.fromString(rs.getString("predecessor_id"))),
						new TaskId(UUID.fromString(rs.getString("successor_id"))),
						com.itson.jgantt.domain.valueobject.DependencyType.valueOf(rs.getString("type")),
						new Lag(rs.getInt("lag_days"))));
				}
				return links;
			}
		}
	}

	private TaskType ofType(String name) {
		return name == null ? TaskType.TASK : TaskType.valueOf(name);
	}

}
