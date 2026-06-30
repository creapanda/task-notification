# Task Notification

A Java desktop application for viewing tasks stored in a local SQLite database.

## Current Status

The first working screen displays the task list currently stored in the database.

## Planned Stack

- Java
- JavaFX
- Maven
- SQLite

## Documentation

- [Roadmap](docs/ROADMAP.md)
- [Project Structure](docs/STRUCTURE.md)

## Run

```bash
mvn clean javafx:run
```

On first launch, the app creates `database/tasks.db` and displays every row currently stored in the `tasks` table.

## CI/CD

GitHub Actions runs the Maven build on pushes and pull requests to `main`.

```bash
mvn --batch-mode --update-snapshots verify
```

When a tag starting with `v` is pushed, for example `v0.1.0`, the pipeline packages the app and attaches the JAR to a GitHub release.
