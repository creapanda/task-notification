package com.tasknotification.repository;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Supplies connections and allows repository tests to use a test database.
 */
@FunctionalInterface
public interface ConnectionFactory {
    Connection getConnection() throws SQLException;
}
