package com.tasknotification.database;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConfigTest {

    // Note: Verifies that DATABASE_PATH is initialized and not null.
    @Test
    void databasePathIsNotNull() {
        assertNotNull(DatabaseConfig.DATABASE_PATH);
    }

    // Note: Verifies that DATABASE_PATH points to a file named "tasks.db".
    @Test
    void databasePathEndsWithTasksDb() {
        Path path = DatabaseConfig.DATABASE_PATH;

        assertTrue(path.getFileName().toString().equals("tasks.db"));
    }

    // Note: Verifies that DATABASE_PATH includes a "database" parent directory.
    @Test
    void databasePathParentDirectoryIsDatabase() {
        Path path = DatabaseConfig.DATABASE_PATH;

        assertTrue(path.getParent().getFileName().toString().equals("database"));
    }

    // Note: Verifies that JDBC_URL is initialized and not null.
    @Test
    void jdbcUrlIsNotNull() {
        assertNotNull(DatabaseConfig.JDBC_URL);
    }

    // Note: Verifies that JDBC_URL starts with the "jdbc:sqlite:" prefix.
    @Test
    void jdbcUrlStartsWithSqlitePrefix() {
        assertTrue(DatabaseConfig.JDBC_URL.startsWith("jdbc:sqlite:"));
    }

    // Note: Verifies that JDBC_URL contains the database path string.
    @Test
    void jdbcUrlContainsDatabasePath() {
        assertTrue(DatabaseConfig.JDBC_URL.contains(DatabaseConfig.DATABASE_PATH.toString()));
    }

    // Note: Verifies that JDBC_URL is consistent with "jdbc:sqlite:" concatenated with DATABASE_PATH.
    @Test
    void jdbcUrlMatchesExpectedFormat() {
        String expectedUrl = "jdbc:sqlite:" + DatabaseConfig.DATABASE_PATH;

        assertTrue(DatabaseConfig.JDBC_URL.equals(expectedUrl));
    }

    // Note: Verifies that DATABASE_PATH is a relative path in the development environment (no "Task Notification.exe" next to java.home).
    @Test
    void databasePathIsRelativeInDevelopmentEnvironment() {
        Path path = DatabaseConfig.DATABASE_PATH;

        assertFalse(path.isAbsolute());
    }
}
