# Project Structure

```text
task-notification/
  pom.xml
  README.md
  database/
    schema.sql
  docs/
    ROADMAP.md
    STRUCTURE.md
  src/
    main/
      java/
        com/tasknotification/
          app/
          controller/
          database/
          model/
          repository/
          service/
          util/
      resources/
        com/tasknotification/
          fxml/
          styles/
        images/
    test/
      java/
        com/tasknotification/
```

## Package Purpose

- `app`: application startup and JavaFX bootstrapping
- `controller`: JavaFX screen controllers
- `database`: database connection and schema setup
- `model`: task entity
- `repository`: persistence layer for tasks
- `service`: application business logic
- `util`: shared helpers
