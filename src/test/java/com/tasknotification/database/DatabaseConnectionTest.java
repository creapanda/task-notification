package com.tasknotification.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConnectionTest {

    // Note: Verifies that getConnection returns a non-null Connection object.
    @Test
    void getConnectionReturnsNonNullConnection() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            assertNotNull(connection);
        }
    }

    // Note: Verifies that the returned connection is open and not closed.
    @Test
    void getConnectionReturnsOpenConnection() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            assertFalse(connection.isClosed());
        }
    }

    // Note: Verifies that two consecutive calls return different Connection instances (not pooled singletons).
    @Test
    void getConnectionReturnsDifferentInstancesPerCall() throws Exception {
        try (Connection first = DatabaseConnection.getConnection();
             Connection second = DatabaseConnection.getConnection()) {
            assertFalse(first == second);
        }
    }

    // Note: Verifies that the connection uses a valid SQLite database by executing a simple query.
    @Test
    void getConnectionCanExecuteSimpleQuery() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            boolean hasResult = connection.createStatement().execute("SELECT 1");

            assertTrue(hasResult);
        }
    }

    // Note: Verifies that closing the returned connection marks it as closed.
    @Test
    void getConnectionCanBeClosed() throws Exception {
        Connection connection = DatabaseConnection.getConnection();

        connection.close();

        assertTrue(connection.isClosed());
    }
}
