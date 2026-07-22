# API Documentation

Developer-facing API reference for the Task Notification desktop application.

## Overview

The app is organized into small Java packages:

- `com.tasknotification.app`: JavaFX application entry points and UI coordination.
- `com.tasknotification.database`: SQLite database path, connection, and schema setup.
- `com.tasknotification.model`: Task data model.
- `com.tasknotification.repository`: Database read/write operations.
- `com.tasknotification.notification`: Desktop tray and deadline notification service.
- `com.tasknotification.service`: Export services.
- `com.tasknotification.startup`: Windows startup and packaged-app uninstall helpers.

## Data Model

### `Task`

Package: `com.tasknotification.model`

Immutable record representing one row from the `tasks` table.

```java
public record Task(
        long id,
        LocalDateTime dateCreated,
        String person,
        String taskDescription,
        LocalDateTime deadline,
        boolean completed
)
```

Fields:

- `id`: SQLite primary key.
- `dateCreated`: Date and time when the task was created.
- `person`: Person assigned to the task.
- `taskDescription`: Task details.
- `deadline`: Optional task deadline.
- `completed`: Whether the task is finished.

## Database API

### `DatabaseConfig`

Package: `com.tasknotification.database`

Provides the shared SQLite database path and JDBC URL.

Public fields:

- `DATABASE_PATH`: Path to `tasks.db`.
- `JDBC_URL`: SQLite JDBC connection string.

Behavior:

- In development, the database is stored at `database/tasks.db`.
- In a packaged app, the database is stored inside the packaged app folder at `database/tasks.db`.

### `DatabaseConnection`

Package: `com.tasknotification.database`

```java
public static Connection getConnection() throws SQLException
```

Returns a JDBC connection to the configured SQLite database.

### `DatabaseInitializer`

Package: `com.tasknotification.database`

```java
public static void initialize() throws SQLException
```

Creates the database directory and applies `schema.sql` if needed.

Call this before repository operations when starting the app or loading task data.

## Repository API

### `ConnectionFactory`

Package: `com.tasknotification.repository`

Functional interface used to inject database connections, mainly for tests.

```java
Connection getConnection() throws SQLException
```

### `TaskRepository`

Package: `com.tasknotification.repository`

Handles all SQLite task operations.

Constructors:

```java
public TaskRepository()
public TaskRepository(ConnectionFactory connectionFactory)
```

Methods:

```java
public List<Task> findAll() throws SQLException
```

Returns all tasks, sorted with unfinished tasks first, then nearest deadlines.

```java
public List<Task> findClosestUnfinished(int limit) throws SQLException
```

Returns the nearest unfinished tasks up to `limit`.

```java
public List<Task> findCompleted() throws SQLException
```

Returns completed tasks only. This is used by Excel export.

```java
public List<Task> findUnfinishedDueWithin(LocalDateTime now, Duration duration) throws SQLException
```

Returns unfinished tasks with deadlines after `now` and within the supplied duration.

```java
public Task add(String person, String taskDescription, LocalDateTime deadline, boolean completed) throws SQLException
```

Creates a task and returns the created `Task` with its generated ID.

```java
public void update(Task task) throws SQLException
```

Updates person, description, deadline, and completed status for an existing task.

```java
public void updateCompleted(long taskId, boolean completed) throws SQLException
```

Updates only the completed flag.

```java
public void delete(long taskId) throws SQLException
```

Deletes one task by ID.

## Notification API

### `DeadlineNotificationService`

Package: `com.tasknotification.notification`

Checks deadlines and manages the Windows system tray menu.

Constructor:

```java
public DeadlineNotificationService(TaskRepository taskRepository)
```

Lifecycle:

```java
public void start()
public void stop()
```

`start()` initializes the tray icon, checks deadlines immediately, then checks every 5 minutes.

Tray actions:

```java
public void setOpenAction(Runnable openAction)
public void setExitAction(Runnable exitAction)
public void setToggleStartupAction(Runnable toggleStartupAction)
public void setUninstallAction(Runnable uninstallAction)
```

These callbacks are used by the tray menu items.

Startup menu label:

```java
public void setStartupEnabledSupplier(BooleanSupplier startupEnabledSupplier)
public void updateStartupMenuLabel()
```

The tray menu shows `Turn Off Startup` or `Turn On Startup` based on the supplied startup state.

Status and checks:

```java
public boolean isTrayAvailable()
public void checkNow()
public void showStatusNotification(String title, String message)
```

`checkNow()` sends notifications for unfinished tasks due within 12 hours or 24 hours.

## Export API

### `TaskExcelExporter`

Package: `com.tasknotification.service`

```java
public void export(List<Task> tasks, Path outputPath) throws IOException
```

Writes tasks to an `.xlsx` workbook.

Exported columns:

- ID
- Date Created
- Person
- Task Description
- Deadline
- Completed

The UI currently passes completed tasks only.

## Startup And Uninstall API

### `WindowsStartupRegistration`

Package: `com.tasknotification.startup`

Manages Windows startup registration for the packaged app.

```java
public static void registerPackagedApp()
```

Registers the packaged app to run on Windows sign-in unless startup was disabled.

```java
public static boolean enableStartup()
```

Removes the disabled marker and writes the Windows Startup script.

```java
public static boolean disableStartup()
```

Deletes the Windows Startup script and saves a disabled marker.

```java
public static boolean isStartupEnabled()
```

Returns whether startup is currently enabled for the packaged app.

```java
public static boolean uninstallPackagedApp()
```

Turns off startup, then schedules the packaged app folder for deletion after the app exits.

Important:

- Uninstall only works from the packaged app folder.
- It does not work from Maven development mode.
- If the database is inside the packaged app folder, uninstall deletes that database too.

## App Entry Points

### `TaskNotificationLauncher`

Package: `com.tasknotification.app`

Plain Java launcher used by Maven and the packaged Windows app.

```java
public static void main(String[] args)
```

Supported command-line options:

- `--background`: Start hidden in the system tray.
- `--disable-startup`: Turn off Windows startup and exit.
- `--enable-startup`: Turn on Windows startup and exit.
- `--uninstall-app`: Schedule packaged app folder deletion and exit.

### `TaskNotificationApp`

Package: `com.tasknotification.app`

JavaFX application class.

```java
public void start(Stage stage)
public void stop()
public static void main(String[] args)
```

Responsibilities:

- Shows the compact main window.
- Shows the full task list window.
- Handles add, edit, delete, complete, and export actions.
- Connects tray menu actions to startup toggle and uninstall behavior.
- Starts and stops `DeadlineNotificationService`.

## Database Schema

Table: `tasks`

```sql
CREATE TABLE IF NOT EXISTS tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date_created TEXT NOT NULL DEFAULT (datetime('now')),
    person TEXT NOT NULL,
    task_description TEXT NOT NULL,
    deadline TEXT,
    completed INTEGER NOT NULL DEFAULT 0,
    CHECK (completed IN (0, 1))
);
```

Indexes:

```sql
CREATE INDEX IF NOT EXISTS idx_tasks_completed ON tasks(completed);
CREATE INDEX IF NOT EXISTS idx_tasks_deadline ON tasks(deadline);
```

## Error Handling

Most database APIs throw `SQLException`.

The UI catches these exceptions and shows user-friendly error dialogs. Lower-level repository and database classes leave error handling to the caller.

## Testing

Run all automated tests:

```bash
mvn test
```

Run package build:

```bash
mvn package
```
