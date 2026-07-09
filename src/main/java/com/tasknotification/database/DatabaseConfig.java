package com.tasknotification.database;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stores the shared SQLite database location and JDBC connection URL.
 */
public final class DatabaseConfig {
    public static final Path DATABASE_PATH = resolveDatabasePath();
    public static final String JDBC_URL = "jdbc:sqlite:" + DATABASE_PATH;

    private DatabaseConfig() {
    }

    private static Path resolveDatabasePath() {
        Path javaHome = Path.of(System.getProperty("java.home", ""));
        Path appDirectory = javaHome.getParent();
        if (appDirectory != null
                && Files.isRegularFile(appDirectory.resolve("Task Notification.exe"))) {
            return appDirectory.resolve("database").resolve("tasks.db");
        }
        return Path.of("database", "tasks.db");
    }
}
