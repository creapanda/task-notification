package com.tasknotification.repository;

import com.tasknotification.database.DatabaseConnection;
import com.tasknotification.model.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    private final ConnectionFactory connectionFactory;

    public TaskRepository() {
        this(DatabaseConnection::getConnection);
    }

    public TaskRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

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
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                tasks.add(mapTask(resultSet));
            }
        }
        return tasks;
    }

    public List<Task> findClosestUnfinished(int limit) throws SQLException {
        String sql = """
                SELECT id,
                       date_created,
                       person,
                       task_description,
                       deadline,
                       completed
                FROM tasks
                WHERE completed = 0
                ORDER BY
                    CASE WHEN deadline IS NULL OR deadline = '' THEN 1 ELSE 0 END,
                    deadline ASC,
                    id DESC
                LIMIT ?
                """;

        List<Task> tasks = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tasks.add(mapTask(resultSet));
                }
            }
        }
        return tasks;
    }

    public List<Task> findCompleted() throws SQLException {
        String sql = """
                SELECT id,
                       date_created,
                       person,
                       task_description,
                       deadline,
                       completed
                FROM tasks
                WHERE completed = 1
                ORDER BY
                    CASE WHEN deadline IS NULL OR deadline = '' THEN 1 ELSE 0 END,
                    deadline ASC,
                    id DESC
                """;

        List<Task> tasks = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                tasks.add(mapTask(resultSet));
            }
        }
        return tasks;
    }

    public List<Task> findUnfinishedDueWithin(LocalDateTime now, Duration duration) throws SQLException {
        String sql = """
                SELECT id,
                       date_created,
                       person,
                       task_description,
                       deadline,
                       completed
                FROM tasks
                WHERE completed = 0
                  AND deadline IS NOT NULL
                  AND deadline != ''
                  AND deadline > ?
                  AND deadline <= ?
                ORDER BY deadline ASC, id DESC
                """;

        List<Task> tasks = new ArrayList<>();
        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, formatDateTime(now));
            statement.setString(2, formatDateTime(now.plus(duration)));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tasks.add(mapTask(resultSet));
                }
            }
        }
        return tasks;
    }

    public Task add(String person, String taskDescription, LocalDateTime deadline, boolean completed) throws SQLException {
        String sql = """
                INSERT INTO tasks (date_created, person, task_description, deadline, completed)
                VALUES (?, ?, ?, ?, ?)
                """;
        LocalDateTime dateCreated = LocalDateTime.now().withNano(0);

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, formatDateTime(dateCreated));
            statement.setString(2, person);
            statement.setString(3, taskDescription);
            setNullableDateTime(statement, 4, deadline);
            statement.setInt(5, completed ? 1 : 0);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Task(
                            generatedKeys.getLong(1),
                            dateCreated,
                            person,
                            taskDescription,
                            deadline,
                            completed
                    );
                }
            }
        }

        throw new SQLException("Creating task failed, no ID returned.");
    }

    public void update(Task task) throws SQLException {
        String sql = """
                UPDATE tasks
                SET person = ?,
                    task_description = ?,
                    deadline = ?,
                    completed = ?
                WHERE id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, task.person());
            statement.setString(2, task.taskDescription());
            setNullableDateTime(statement, 3, task.deadline());
            statement.setInt(4, task.completed() ? 1 : 0);
            statement.setLong(5, task.id());
            statement.executeUpdate();
        }
    }

    public void updateCompleted(long taskId, boolean completed) throws SQLException {
        String sql = """
                UPDATE tasks
                SET completed = ?
                WHERE id = ?
                """;

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, completed ? 1 : 0);
            statement.setLong(2, taskId);
            statement.executeUpdate();
        }
        
    }


    public void delete(long taskId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";

        try (Connection connection = connectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, taskId);
            statement.executeUpdate();
        }
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

    private void setNullableDateTime(PreparedStatement statement, int parameterIndex, LocalDateTime value) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, Types.VARCHAR);
            return;
        }

        statement.setString(parameterIndex, formatDateTime(value));
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.withNano(0).toString();
    }
}
