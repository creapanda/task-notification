package com.tasknotification.repository;

import com.tasknotification.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void addCreatesTask() throws Exception {
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

    @Test
    void updateChangesEditableTaskFields() throws Exception {
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

    @Test
    void updateCompletedChangesOnlyCompletedState() throws Exception {
        Task createdTask = taskRepository.add("Alex", "Prepare report", LocalDateTime.of(2026, 7, 1, 0, 0), false);

        taskRepository.updateCompleted(createdTask.id(), true);

        Task savedTask = taskRepository.findAll().getFirst();
        assertEquals(createdTask.id(), savedTask.id());
        assertEquals(createdTask.person(), savedTask.person());
        assertEquals(createdTask.taskDescription(), savedTask.taskDescription());
        assertEquals(createdTask.deadline(), savedTask.deadline());
        assertTrue(savedTask.completed());
    }

    @Test
    void findClosestUnfinishedReturnsNearestIncompleteDeadlinesUpToLimit() throws Exception {
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

    @Test
    void findClosestUnfinishedIgnoresCompletedTasks() throws Exception {
        taskRepository.add("Alex", "Completed task", LocalDateTime.of(2026, 7, 1, 0, 0), true);

        List<Task> tasks = taskRepository.findClosestUnfinished(3);

        assertTrue(tasks.isEmpty());
    }

    @Test
    void findUnfinishedDueWithinReturnsOnlyTasksInsideWindow() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 7, 1, 9, 0);
        taskRepository.add("Alex", "Due in 10 hours", now.plusHours(10), false);
        taskRepository.add("Sam", "Due in 30 hours", now.plusHours(30), false);
        taskRepository.add("Jordan", "Already complete", now.plusHours(8), true);
        taskRepository.add("Taylor", "Already overdue", now.minusHours(1), false);

        List<Task> tasks = taskRepository.findUnfinishedDueWithin(now, Duration.ofHours(24));

        assertEquals(1, tasks.size());
        assertEquals("Due in 10 hours", tasks.getFirst().taskDescription());
    }
}
