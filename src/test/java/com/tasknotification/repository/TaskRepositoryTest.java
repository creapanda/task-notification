package com.tasknotification.repository;

import com.tasknotification.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskRepositoryTest {
    private static final String JDBC_URL = "jdbc:sqlite:file:task_repository_test?mode=memory&cache=shared";

    private Connection connection;
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(JDBC_URL);
        taskRepository = new TaskRepository(() -> DriverManager.getConnection(JDBC_URL));

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS tasks");
            statement.executeUpdate("""
                    CREATE TABLE tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        date_created TEXT NOT NULL DEFAULT (datetime('now')),
                        person TEXT NOT NULL,
                        task_description TEXT NOT NULL,
                        deadline TEXT,
                        completed INTEGER NOT NULL DEFAULT 0,
                        CHECK (completed IN (0, 1))
                    )
                    """);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    // Note: Inserts a task with all fields and verifies the returned object and database state.
    @Test
    void addTest() throws Exception {
        LocalDateTime deadline = LocalDateTime.of(2026, 7, 1, 0, 0);

        Task createdTask = taskRepository.add("Alex", "Prepare report", deadline, false);

        assertTrue(createdTask.id() > 0);
        assertNotNull(createdTask.dateCreated());
        assertEquals("Alex", createdTask.person());
        assertEquals("Prepare report", createdTask.taskDescription());
        assertEquals(deadline, createdTask.deadline());
        assertFalse(createdTask.completed());

        List<Task> tasks = taskRepository.findAll();
        assertEquals(1, tasks.size());
        assertEquals("Prepare report", tasks.getFirst().taskDescription());
    }

    // Note: Updates all editable fields (person, description, deadline, completed) and verifies id and dateCreated remain unchanged.
    @Test
    void updateTest() throws Exception {
        Task createdTask = taskRepository.add("Alex", "Prepare report", null, false);
        Task changedTask = new Task(
                createdTask.id(),
                createdTask.dateCreated(),
                "Sam",
                "Review report",
                LocalDateTime.of(2026, 7, 2, 0, 0),
                true
        );

        taskRepository.update(changedTask);

        List<Task> tasks = taskRepository.findAll();
        assertEquals(1, tasks.size());

        Task savedTask = tasks.getFirst();
        assertEquals(createdTask.id(), savedTask.id());
        assertEquals(createdTask.dateCreated(), savedTask.dateCreated());
        assertEquals("Sam", savedTask.person());
        assertEquals("Review report", savedTask.taskDescription());
        assertEquals(LocalDateTime.of(2026, 7, 2, 0, 0), savedTask.deadline());
        assertTrue(savedTask.completed());
    }

    // Note: Toggles only the completed flag and verifies all other fields remain unchanged.
    @Test
    void updateCompletedTest() throws Exception {
        Task createdTask = taskRepository.add("Alex", "Prepare report", LocalDateTime.of(2026, 7, 1, 0, 0), false);

        taskRepository.updateCompleted(createdTask.id(), true);

        Task savedTask = taskRepository.findAll().getFirst();
        assertEquals(createdTask.id(), savedTask.id());
        assertEquals(createdTask.person(), savedTask.person());
        assertEquals(createdTask.taskDescription(), savedTask.taskDescription());
        assertEquals(createdTask.deadline(), savedTask.deadline());
        assertTrue(savedTask.completed());
    }

    // Note: Inserts 5 tasks (mixed completed/unfinished) and verifies only the 3 nearest-deadline unfinished tasks are returned in order.
    @Test
    void findClosestUnfinishedTest() throws Exception {
        taskRepository.add("Alex", "Later unfinished", LocalDateTime.of(2026, 7, 10, 0, 0), false);
        taskRepository.add("Sam", "Completed earlier", LocalDateTime.of(2026, 7, 1, 0, 0), true);
        taskRepository.add("Jordan", "Closest unfinished", LocalDateTime.of(2026, 7, 2, 0, 0), false);
        taskRepository.add("Taylor", "Middle unfinished", LocalDateTime.of(2026, 7, 5, 0, 0), false);
        taskRepository.add("Riley", "Fourth unfinished", LocalDateTime.of(2026, 7, 20, 0, 0), false);

        List<Task> tasks = taskRepository.findClosestUnfinished(3);

        assertEquals(3, tasks.size());
        assertEquals("Closest unfinished", tasks.getFirst().taskDescription());
        assertEquals("Middle unfinished", tasks.get(1).taskDescription());
        assertEquals("Later unfinished", tasks.get(2).taskDescription());
        assertTrue(tasks.stream().noneMatch(Task::completed));
    }

    // Note: Inserts only a completed task and verifies the result is an empty list.
    @Test
    void findClosestUnfinishedTestIgnoresCompleted() throws Exception {
        taskRepository.add("Alex", "Completed task", LocalDateTime.of(2026, 7, 1, 0, 0), true);

        List<Task> tasks = taskRepository.findClosestUnfinished(3);

        assertTrue(tasks.isEmpty());
    }

    // Note: Inserts one completed and one unfinished task, then verifies only the completed task is returned.
    @Test
    void findCompletedTest() throws Exception {
        taskRepository.add("Alex", "Finished task", LocalDateTime.of(2026, 7, 1, 0, 0), true);
        taskRepository.add("Sam", "Unfinished task", LocalDateTime.of(2026, 7, 2, 0, 0), false);

        List<Task> tasks = taskRepository.findCompleted();

        assertEquals(1, tasks.size());
        assertEquals("Finished task", tasks.getFirst().taskDescription());
        assertTrue(tasks.getFirst().completed());
    }

    // Note: Inserts 4 tasks covering all edge cases (in-window, out-of-window, completed, overdue) and verifies only the in-window unfinished task is returned.
    @Test
    void findUnfinishedDueWithinTest() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 9, 0);
        taskRepository.add("Alex", "Due in 10 hours", now.plusHours(10), false);
        taskRepository.add("Sam", "Due in 30 hours", now.plusHours(30), false);
        taskRepository.add("Jordan", "Already complete", now.plusHours(8), true);
        taskRepository.add("Taylor", "Already overdue", now.minusHours(1), false);

        List<Task> tasks = taskRepository.findUnfinishedDueWithin(now, Duration.ofHours(24));

        assertEquals(1, tasks.size());
        assertEquals("Due in 10 hours", tasks.getFirst().taskDescription());
    }

    // ── delete() tests ──────────────────────────────────────────────────

    // Note: Adds a task, deletes it by id, and verifies the database is empty.
    @Test
    void deleteTest() throws Exception {
        Task task = taskRepository.add("Alex", "Task to delete", LocalDateTime.of(2026, 7, 1, 0, 0), false);

        taskRepository.delete(task.id());

        List<Task> tasks = taskRepository.findAll();
        assertTrue(tasks.isEmpty());
    }

    // Note: Adds two tasks, deletes one, and verifies the other task remains intact.
    @Test
    void deleteTestDoesNotAffectOthers() throws Exception {
        Task taskToDelete = taskRepository.add("Alex", "Delete me", LocalDateTime.of(2026, 7, 1, 0, 0), false);
        Task taskToKeep = taskRepository.add("Sam", "Keep me", LocalDateTime.of(2026, 7, 2, 0, 0), false);

        taskRepository.delete(taskToDelete.id());

        List<Task> tasks = taskRepository.findAll();
        assertEquals(1, tasks.size());
        assertEquals("Keep me", tasks.getFirst().taskDescription());
        assertEquals(taskToKeep.id(), tasks.getFirst().id());
    }

    // Note: Attempts to delete a non-existent id and verifies no exception is thrown and existing data is unaffected.
    @Test
    void deleteTestNonExistentId() throws Exception {
        taskRepository.add("Alex", "Existing task", LocalDateTime.of(2026, 7, 1, 0, 0), false);

        taskRepository.delete(9999);

        List<Task> tasks = taskRepository.findAll();
        assertEquals(1, tasks.size());
    }

    // ── findAll() sort order tests ──────────────────────────────────────

    // Note: Queries an empty database and verifies an empty list is returned.
    @Test
    void findAllTestEmpty() throws Exception {
        List<Task> tasks = taskRepository.findAll();

        assertTrue(tasks.isEmpty());
    }

    // Note: Inserts a completed task first and an unfinished task second, then verifies unfinished appears before completed.
    @Test
    void findAllTestSortByCompleted() throws Exception {
        taskRepository.add("Alex", "Completed task", LocalDateTime.of(2026, 7, 1, 0, 0), true);
        taskRepository.add("Sam", "Unfinished task", LocalDateTime.of(2026, 7, 2, 0, 0), false);

        List<Task> tasks = taskRepository.findAll();

        assertEquals(2, tasks.size());
        assertFalse(tasks.getFirst().completed());
        assertTrue(tasks.get(1).completed());
    }

    // Note: Inserts a null-deadline task first and a deadline task second, then verifies deadline task appears first.
    @Test
    void findAllTestSortByDeadline() throws Exception {
        taskRepository.add("Alex", "No deadline", null, false);
        taskRepository.add("Sam", "Has deadline", LocalDateTime.of(2026, 7, 5, 0, 0), false);

        List<Task> tasks = taskRepository.findAll();

        assertEquals(2, tasks.size());
        assertEquals("Has deadline", tasks.getFirst().taskDescription());
        assertEquals("No deadline", tasks.get(1).taskDescription());
    }

    // ── add() edge case tests ───────────────────────────────────────────

    // Note: Inserts a task with null deadline and verifies both the returned object and the database store null.
    @Test
    void addTestNullDeadline() throws Exception {
        Task task = taskRepository.add("Alex", "No deadline task", null, false);

        assertNotNull(task);
        assertTrue(task.id() > 0);
        assertNull(task.deadline());
        assertEquals("Alex", task.person());

        List<Task> tasks = taskRepository.findAll();
        assertEquals(1, tasks.size());
        assertNull(tasks.getFirst().deadline());
    }

    // ── parseDateTime (indirect) tests ──────────────────────────────────

    // Note: Inserts raw SQL with SQLite space-separated datetime format and verifies parseDateTime converts it correctly.
    @Test
    void parseDateTimeTestSqliteSpaceFormat() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO tasks (date_created, person, task_description, deadline, completed)
                    VALUES ('2026-07-01 09:30:00', 'Alex', 'Space format', '2026-07-10 17:00:00', 0)
                    """);
        }

        List<Task> tasks = taskRepository.findAll();

        assertEquals(1, tasks.size());
        assertEquals(LocalDateTime.of(2026, 7, 1, 9, 30, 0), tasks.getFirst().dateCreated());
        assertEquals(LocalDateTime.of(2026, 7, 10, 17, 0, 0), tasks.getFirst().deadline());
    }

    // Note: Inserts raw SQL with date-only deadline and verifies parseDateTime converts it to start of day (00:00).
    @Test
    void parseDateTimeTestDateOnly() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO tasks (date_created, person, task_description, deadline, completed)
                    VALUES ('2026-07-01 09:30:00', 'Alex', 'Date only', '2026-07-10', 0)
                    """);
        }

        List<Task> tasks = taskRepository.findAll();

        assertEquals(1, tasks.size());
        assertEquals(LocalDateTime.of(2026, 7, 10, 0, 0, 0), tasks.getFirst().deadline());
    }

    // Note: Inserts raw SQL with an empty string deadline and verifies parseDateTime returns null.
    @Test
    void parseDateTimeTestBlank() throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO tasks (date_created, person, task_description, deadline, completed)
                    VALUES ('2026-07-01 09:30:00', 'Alex', 'Blank deadline', '', 0)
                    """);
        }

        List<Task> tasks = taskRepository.findAll();

        assertEquals(1, tasks.size());
        assertNull(tasks.getFirst().deadline());
    }

    // ── findClosestUnfinished() null deadline test ──────────────────────

    // Note: Inserts one task with deadline and one without, then verifies null-deadline task is sorted last.
    @Test
    void findClosestUnfinishedTestNullDeadlineLast() throws Exception {
        taskRepository.add("Alex", "No deadline", null, false);
        taskRepository.add("Sam", "Has deadline", LocalDateTime.of(2026, 7, 5, 0, 0), false);

        List<Task> tasks = taskRepository.findClosestUnfinished(10);

        assertEquals(2, tasks.size());
        assertEquals("Has deadline", tasks.getFirst().taskDescription());
        assertEquals("No deadline", tasks.get(1).taskDescription());
    }

    // ── findUnfinishedDueWithin() edge case tests ───────────────────────

    // Note: Inserts a task with deadline far beyond the time window and verifies an empty list is returned.
    @Test
    void findUnfinishedDueWithinTestEmpty() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 9, 0);
        taskRepository.add("Alex", "Far future task", now.plusHours(48), false);

        List<Task> tasks = taskRepository.findUnfinishedDueWithin(now, Duration.ofHours(24));

        assertTrue(tasks.isEmpty());
    }

    // Note: Inserts a task with null deadline and verifies it is excluded from the time-window query.
    @Test
    void findUnfinishedDueWithinTestNullDeadline() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 9, 0);
        taskRepository.add("Alex", "No deadline", null, false);

        List<Task> tasks = taskRepository.findUnfinishedDueWithin(now, Duration.ofHours(24));

        assertTrue(tasks.isEmpty());
    }

    // ── findCompleted() edge case test ──────────────────────────────────

    // Note: Inserts only an unfinished task and verifies findCompleted returns an empty list.
    @Test
    void findCompletedTestEmpty() throws Exception {
        taskRepository.add("Alex", "Unfinished task", LocalDateTime.of(2026, 7, 1, 0, 0), false);

        List<Task> tasks = taskRepository.findCompleted();

        assertTrue(tasks.isEmpty());
    }

    // ── findUnfinishedDueWithin() window boundary tests ─────────────────

    // Note: Inserts a task whose deadline is exactly `now` and verifies it is excluded, because the
    //       query uses a strict lower bound (deadline > now).
    @Test
    void findUnfinishedDueWithinTestExcludesDeadlineExactlyAtNow() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 9, 0);
        taskRepository.add("Alex", "Due exactly now", now, false);

        List<Task> tasks = taskRepository.findUnfinishedDueWithin(now, Duration.ofHours(24));

        assertTrue(tasks.isEmpty(), "A deadline exactly at now is not inside the future window");
    }

    // Note: Inserts a task whose deadline is exactly at the end of the window and verifies it is
    //       included, because the query uses an inclusive upper bound (deadline <= now + duration).
    @Test
    void findUnfinishedDueWithinTestIncludesDeadlineExactlyAtWindowEnd() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 9, 0);
        taskRepository.add("Alex", "Due at window end", now.plusHours(24), false);

        List<Task> tasks = taskRepository.findUnfinishedDueWithin(now, Duration.ofHours(24));

        assertEquals(1, tasks.size());
        assertEquals("Due at window end", tasks.getFirst().taskDescription());
    }

    // Note: Inserts a task one second past the end of the window and verifies it is excluded.
    @Test
    void findUnfinishedDueWithinTestExcludesDeadlineJustPastWindowEnd() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 9, 0);
        taskRepository.add("Alex", "Just outside", now.plusHours(24).plusSeconds(1), false);

        List<Task> tasks = taskRepository.findUnfinishedDueWithin(now, Duration.ofHours(24));

        assertTrue(tasks.isEmpty());
    }

    // ── findClosestUnfinished() limit tests ─────────────────────────────

    // Note: Inserts two unfinished tasks and verifies a limit of 0 returns an empty list.
    @Test
    void findClosestUnfinishedTestLimitZero() throws Exception {
        taskRepository.add("Alex", "First", LocalDateTime.of(2026, 7, 1, 0, 0), false);
        taskRepository.add("Sam", "Second", LocalDateTime.of(2026, 7, 2, 0, 0), false);

        List<Task> tasks = taskRepository.findClosestUnfinished(0);

        assertTrue(tasks.isEmpty());
    }

    // Note: Inserts one task and verifies a limit larger than the row count returns everything.
    @Test
    void findClosestUnfinishedTestLimitLargerThanRowCount() throws Exception {
        taskRepository.add("Alex", "Only task", LocalDateTime.of(2026, 7, 1, 0, 0), false);

        List<Task> tasks = taskRepository.findClosestUnfinished(100);

        assertEquals(1, tasks.size());
    }

    // ── update() / updateCompleted() / delete() on missing rows ─────────

    // Note: Updates a task id that does not exist and verifies nothing is thrown or inserted.
    @Test
    void updateTestNonExistentId() throws Exception {
        Task missingTask = new Task(9999L, LocalDateTime.of(2026, 7, 1, 9, 0),
                "Ghost", "Ghost task", null, false);

        taskRepository.update(missingTask);

        assertTrue(taskRepository.findAll().isEmpty(), "Updating a missing row must not insert it");
    }

    // Note: Flags a task id that does not exist as completed and verifies nothing is thrown or inserted.
    @Test
    void updateCompletedTestNonExistentId() throws Exception {
        taskRepository.updateCompleted(9999L, true);

        assertTrue(taskRepository.findAll().isEmpty());
    }

    // ── add() failure branch ────────────────────────────────────────────

    // Note: Uses a connection whose generated-keys result is empty to reach the failure branch of
    //       add(), which the normal SQLite path never hits.
    @Test
    void addTestThrowsWhenNoGeneratedKeyIsReturned() {
        TaskRepository repositoryWithoutKeys = new TaskRepository(
                () -> connectionReturningNoGeneratedKeys(DriverManager.getConnection(JDBC_URL)));

        SQLException exception = assertThrows(SQLException.class,
                () -> repositoryWithoutKeys.add("Alex", "No key", null, false));

        assertEquals("Creating task failed, no ID returned.", exception.getMessage());
    }

    // ── Default constructor ─────────────────────────────────────────────

    // Note: Verifies the no-arg constructor can be created. It only stores a method reference, so
    //       this does not open a connection to the real application database.
    @Test
    void defaultConstructorDoesNotOpenConnection() {
        assertNotNull(new TaskRepository());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    // Wraps a real connection so every PreparedStatement it hands out reports no generated keys,
    // reproducing a driver that inserts the row but returns no id.
    private static Connection connectionReturningNoGeneratedKeys(Connection realConnection) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    Object result = invokeOn(realConnection, method, args);
                    return result instanceof PreparedStatement statement
                            ? statementWithoutGeneratedKeys(statement)
                            : result;
                });
    }

    private static PreparedStatement statementWithoutGeneratedKeys(PreparedStatement realStatement) {
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class},
                (proxy, method, args) -> {
                    if ("getGeneratedKeys".equals(method.getName())) {
                        // An always-empty result set; it is closed together with its connection.
                        return realStatement.getConnection()
                                .createStatement()
                                .executeQuery("SELECT 1 WHERE 0");
                    }
                    return invokeOn(realStatement, method, args);
                });
    }

    private static Object invokeOn(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
