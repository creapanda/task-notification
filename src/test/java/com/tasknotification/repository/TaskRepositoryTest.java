package com.tasknotification.repository;

import com.tasknotification.model.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
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
}
