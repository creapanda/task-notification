# Task Notification

A Java desktop application for viewing tasks stored in a local SQLite database.

## Current Status

The Main window displays up to 3 unfinished tasks closest to their deadlines.

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

On first launch, the app creates `database/tasks.db`. The Main window displays task description, deadline, and completed status for up to 3 unfinished tasks closest to their deadlines. The See All Tasks button opens a second window with the full task list.

The app checks unfinished task deadlines while it is running and sends a desktop notification when a task is due within 24 hours or 12 hours.

## Test

```bash
mvn test
```

## CI/CD

GitHub Actions runs the Maven build on pushes and pull requests to `main`.

```bash
mvn --batch-mode --update-snapshots verify
```

When a tag starting with `v` is pushed, for example `v0.1.0`, the pipeline packages the app and attaches the JAR to a GitHub release.
