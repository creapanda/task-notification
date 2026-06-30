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
