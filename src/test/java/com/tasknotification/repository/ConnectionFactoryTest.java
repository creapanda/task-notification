package com.tasknotification.repository;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link ConnectionFactory} functional interface.
 */
class ConnectionFactoryTest {
    private static final String JDBC_URL = "jdbc:sqlite:file:connection_factory_test?mode=memory&cache=shared";

    // Note: Creates a ConnectionFactory using a lambda and verifies getConnection returns a non-null connection.
    @Test
    void getConnectionTest() throws Exception {
        ConnectionFactory factory = () -> DriverManager.getConnection(JDBC_URL);

        try (Connection connection = factory.getConnection()) {
            assertNotNull(connection);
        }
    }

    // Note: Creates a ConnectionFactory using a method reference and verifies getConnection returns a non-null connection.
    @Test
    void getConnectionTestMethodReference() throws Exception {
        ConnectionFactory factory = ConnectionFactoryTest::createTestConnection;

        try (Connection connection = factory.getConnection()) {
            assertNotNull(connection);
        }
    }

    // Note: Verifies the connection returned by getConnection is open and usable for executing SQL statements.
    @Test
    void getConnectionTestReturnsUsableConnection() throws Exception {
        ConnectionFactory factory = () -> DriverManager.getConnection(JDBC_URL);

        try (Connection connection = factory.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS test_table (id INTEGER PRIMARY KEY)");
            statement.executeUpdate("INSERT INTO test_table (id) VALUES (1)");

            try (ResultSet resultSet = statement.executeQuery("SELECT id FROM test_table WHERE id = 1")) {
                assertTrue(resultSet.next());
                assertEquals(1, resultSet.getInt("id"));
            }

            statement.executeUpdate("DROP TABLE IF EXISTS test_table");
        }
    }

    // Note: Verifies the connection returned by getConnection is not already closed.
    @Test
    void getConnectionTestReturnsOpenConnection() throws Exception {
        ConnectionFactory factory = () -> DriverManager.getConnection(JDBC_URL);

        try (Connection connection = factory.getConnection()) {
            assertFalse(connection.isClosed());
        }
    }

    // Note: Verifies that each call to getConnection returns a new, separate connection instance.
    @Test
    void getConnectionTestReturnsNewConnectionEachCall() throws Exception {
        ConnectionFactory factory = () -> DriverManager.getConnection(JDBC_URL);

        try (Connection connection1 = factory.getConnection();
             Connection connection2 = factory.getConnection()) {
            assertNotNull(connection1);
            assertNotNull(connection2);
            assertNotSame(connection1, connection2);
        }
    }

    // Note: Verifies that getConnection throws SQLException when given an invalid JDBC URL with no matching driver.
    @Test
    void getConnectionTestThrowsSqlException() {
        ConnectionFactory factory = () -> DriverManager.getConnection("jdbc:nonexistent:invalid");

        assertThrows(SQLException.class, factory::getConnection);
    }

    // Note: Verifies the @FunctionalInterface contract by assigning a lambda that wraps an existing static method.
    @Test
    void getConnectionTestFunctionalInterfaceContract() throws Exception {
        ConnectionFactory factory = () -> {
            Connection connection = DriverManager.getConnection(JDBC_URL);
            connection.setAutoCommit(false);
            return connection;
        };

        try (Connection connection = factory.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.getAutoCommit());
        }
    }

    private static Connection createTestConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }
}
