package com.tasknotification.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInitializerTest {

    // Note: Verifies that initialize() completes without throwing any exception.
    @Test
    void initializeCompletesWithoutException() {
        assertDoesNotThrow(DatabaseInitializer::initialize);
    }

    // Note: Verifies that the tasks table exists after initialization.
    @Test
    void initializeCreatesTasksTable() throws Exception {
        DatabaseInitializer.initialize();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='tasks'")) {
            assertTrue(resultSet.next());
            assertEquals("tasks", resultSet.getString("name"));
        }
    }

    // Note: Verifies that the tasks table has the expected columns (id, date_created, person, task_description, deadline, completed).
    @Test
    void initializeCreatesTasksTableWithExpectedColumns() throws Exception {
        DatabaseInitializer.initialize();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            // If any column is missing, this query will throw an exception
            statement.executeQuery(
                    "SELECT id, date_created, person, task_description, deadline, completed FROM tasks LIMIT 0");
        }
    }

    // Note: Verifies that the idx_tasks_completed index exists after initialization.
    @Test
    void initializeCreatesCompletedIndex() throws Exception {
        DatabaseInitializer.initialize();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_tasks_completed'")) {
            assertTrue(resultSet.next());
        }
    }

    // Note: Verifies that the idx_tasks_deadline index exists after initialization.
    @Test
    void initializeCreatesDeadlineIndex() throws Exception {
        DatabaseInitializer.initialize();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_tasks_deadline'")) {
            assertTrue(resultSet.next());
        }
    }

    // Note: Verifies that calling initialize() twice does not throw (CREATE IF NOT EXISTS is idempotent).
    @Test
    void initializeIsIdempotent() {
        assertDoesNotThrow(() -> {
            DatabaseInitializer.initialize();
            DatabaseInitializer.initialize();
        });
    }

    // Note: Verifies that data inserted after initialization persists and can be queried back.
    @Test
    void initializeAllowsDataInsertionAfterSetup() throws Exception {
        DatabaseInitializer.initialize();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO tasks (person, task_description, completed) VALUES ('TestUser', 'TestTask', 0)");

            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT person, task_description FROM tasks WHERE person = 'TestUser'")) {
                assertTrue(resultSet.next());
                assertEquals("TestUser", resultSet.getString("person"));
                assertEquals("TestTask", resultSet.getString("task_description"));
            }
        }
    }

    // Note: Verifies that the completed column enforces the CHECK constraint (only 0 or 1 are valid).
    @Test
    void initializeCreatesTableWithCompletedCheckConstraint() throws Exception {
        DatabaseInitializer.initialize();

        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {
            // Enable foreign keys and constraints
            statement.executeUpdate("PRAGMA foreign_keys = ON");

            // Valid values (0 and 1) should succeed
            statement.executeUpdate(
                    "INSERT INTO tasks (person, task_description, completed) VALUES ('A', 'Task0', 0)");
            statement.executeUpdate(
                    "INSERT INTO tasks (person, task_description, completed) VALUES ('B', 'Task1', 1)");

            // Invalid value (2) should fail
            boolean constraintViolated = false;
            try {
                statement.executeUpdate(
                        "INSERT INTO tasks (person, task_description, completed) VALUES ('C', 'Task2', 2)");
            } catch (Exception exception) {
                constraintViolated = true;
            }
            assertTrue(constraintViolated, "CHECK constraint should reject completed = 2");
        }
    }
}
