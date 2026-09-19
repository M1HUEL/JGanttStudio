# JGanttStudio — Documentation

A Gantt chart editor built with **Java Swing, Maven, and SQLite**, following a layered (onion-style) architecture split into Maven modules.

## Tech Stack

- **Language:** Java 21 (LTS)
- **Build:** Maven (multi-module)
- **UI:** Java Swing (pure, no extra UI libraries)
- **Persistence:** SQLite via JDBC (`org.xerial:sqlite-jdbc`)
- **Testing:** JUnit 5
- **DI:** None — manual composition at bootstrap

## Architecture Overview

```
JGanttStudio (parent pom: packaging=pom)
├── gantt-domain        Domain layer (pure Java, NO external dependencies)
├── gantt-app           Application layer (use cases / services; depends only on domain)
├── gantt-infra         Infrastructure layer (SQLite, JDBC repositories) depends on domain+app
├── gantt-ui            Presentation layer (Swing) depends on app
└── gantt-bootstrap     Composition root: main() wiring everything at runtime
```

**Dependency rule:** layers only depend downward (`ui → app → domain`, `infra → app+domain`). The domain knows nothing external. Persistence is behind **interfaces (ports)** declared in domain/app and implemented in infra.

## Module Details

### `gantt-domain`

- Entities: `Project` (root aggregate), `Task`, `TaskLink` (dependency), `TaskType` (Task/Milestone).
- Hierarchy: `TaskId`, `ProjectId`, dates with `java.time.LocalDate`.
- Value objects: `DateRange`, `DependencyType` (FS, SS, FF, SF), `Lag`.
- Ports (interfaces): `ProjectRepository`.
- Domain rules: a child task must stay within its parent's range, no cyclic dependencies, `start <= end`.

### `gantt-app`

- Use cases: create/edit/delete tasks, reorder hierarchy, create/delete dependencies, save/load project.
- Scheduling rules: when a task moves, recompute dates of dependent tasks (FS cascade), like GanttProject.
- DTOs for input/output (domain entities are not exposed to the UI).
- Validation and per-use-case transactional logic.

### `gantt-infra`

- SQLite schema: `projects`, `tasks`, `task_links` (FK to projects), simple storage.
- JDBC implementations of the ports (`ProjectRepositoryImpl`), entity-to-row mapping.
- Connection opened/closed per operation (embedded, lightweight usage).

### `gantt-ui` (Swing)

- `MainFrame`: `JSplitPane` — task table on the left, chart on the right.
- **`TaskTablePanel`**: outline/hierarchy (indentation, expand/collapse, edit name/dates/duration).
- **`GanttChartPanel`**: custom component (`JComponent` + `paintComponent`) painting the date header, grid, task bars, dependency arrows, and a "today" marker.
- `TimeScale` (util): `date → pixel` mapping with zoom (day/week/month); keeps table and canvas scroll in sync.
- Thin controllers delegating to `gantt-app`.

### `gantt-bootstrap`

- `Main.main()`: creates `SQLiteProjectRepository` (JDBC) → application services → builds `MainFrame`. Manual injection (no framework).

## Implementation Phases

| Phase | Deliverable | Verification |
|-------|-------------|--------------|
| 0 | Multi-module skeleton, Java 21 toolchain | `mvn verify` compiles |
| 1 | Domain: entities + ports | JUnit unit tests |
| 2 | App: services + scheduling | JUnit unit tests |
| 3 | Infra: SQLite schema + repositories | Integration tests (temporary DB) |
| 4 | UI: frame, table, canvas, controllers | Build + manual smoke test |
| 5 | Visual dependencies, zoom, milestones, polish | Final build + packaging |

## Cross-Cutting Standards

- **Language:** code and this documentation in English.
- **Naming:** English for identifiers; Java standard conventions (camelCase, PascalCase).
- **Git:** branch per phase, atomic commits.
- **No** DI framework or extra UI libraries in the MVP (pure Swing).
- Brief JavaDoc on public APIs; no filler comments.