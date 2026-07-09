package com.tasknotification.database;

import java.nio.file.Path;

/**
 * Stores the shared SQLite database location and JDBC connection URL.
 */
public final class DatabaseConfig {
    public static final Path DATABASE_PATH = Path.of("database", "tasks.db");
    public static final String JDBC_URL = "jdbc:sqlite:" + DATABASE_PATH;

    private DatabaseConfig() {
    }
}
