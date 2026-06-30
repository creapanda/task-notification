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
          notification/
          repository/
          scheduler/
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
- `database`: database connection and migration setup
- `model`: task entities, enums, and value objects
- `notification`: desktop notification integration
- `repository`: persistence layer for tasks
- `scheduler`: reminder scheduling and background checks
- `service`: application business logic
- `util`: shared helpers
