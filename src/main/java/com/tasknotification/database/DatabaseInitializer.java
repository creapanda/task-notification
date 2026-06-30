package com.tasknotification.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {
    private static final Path SCHEMA_PATH = Path.of("database", "schema.sql");

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {
        try {
            Files.createDirectories(DatabaseConfig.DATABASE_PATH.getParent());
            String schema = Files.readString(SCHEMA_PATH);

            try (Connection connection = DatabaseConnection.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("PRAGMA foreign_keys = ON");

                for (String sql : schema.split(";")) {
                    String trimmedSql = sql.trim();
                    if (!trimmedSql.isEmpty()) {
                        statement.executeUpdate(trimmedSql);
                    }
                }
            }
        } catch (IOException exception) {
            throw new SQLException("Could not initialize database files", exception);
        }
    }
}
