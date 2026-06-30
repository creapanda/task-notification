package com.tasknotification.database;

import java.nio.file.Path;

public final class DatabaseConfig {
    public static final Path DATABASE_PATH = Path.of("database", "tasks.db");
    public static final String JDBC_URL = "jdbc:sqlite:" + DATABASE_PATH;

    private DatabaseConfig() {
    }
}
