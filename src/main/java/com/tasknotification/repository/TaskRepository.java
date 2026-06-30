package com.tasknotification.repository;

import com.tasknotification.database.DatabaseConnection;
import com.tasknotification.model.Task;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    public List<Task> findAll() throws SQLException {
        String sql = """
                SELECT id,
                       date_created,
                       person,
                       task_description,
                       deadline,
                       completed
                FROM tasks
                ORDER BY
                    completed ASC,
                    CASE WHEN deadline IS NULL OR deadline = '' THEN 1 ELSE 0 END,
                    deadline ASC,
                    id DESC
                """;

        List<Task> tasks = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                tasks.add(mapTask(resultSet));
            }
        }
        return tasks;
    }

    private Task mapTask(ResultSet resultSet) throws SQLException {
        return new Task(
                resultSet.getLong("id"),
                parseDateTime(resultSet.getString("date_created")),
                resultSet.getString("person"),
                resultSet.getString("task_description"),
                parseDateTime(resultSet.getString("deadline")),
                resultSet.getInt("completed") == 1
        );
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = value.trim().replace(" ", "T");
        try {
            return LocalDateTime.parse(normalizedValue);
        } catch (DateTimeParseException exception) {
            try {
                return LocalDate.parse(value.trim()).atStartOfDay();
            } catch (DateTimeParseException ignoredException) {
                return null;
            }
        }
    }
}
