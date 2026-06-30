package com.tasknotification.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;

public final class DatabaseInitializer {
    private static final Path SCHEMA_PATH = Path.of("database", "schema.sql");

    private DatabaseInitializer() {
    }

    public static void initialize() throws SQLException {
        try {
            Files.createDirectories(DatabaseConfig.DATABASE_PATH.getParent());
            String schema = readSchema();

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

    private static String readSchema() throws IOException {
        try (InputStream inputStream = DatabaseInitializer.class.getResourceAsStream("/database/schema.sql")) {
            if (inputStream != null) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        return Files.readString(SCHEMA_PATH);
    }
}
