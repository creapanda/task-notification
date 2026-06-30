package com.tasknotification.app;

import com.tasknotification.database.DatabaseInitializer;
import com.tasknotification.model.Task;
import com.tasknotification.repository.TaskRepository;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TaskNotificationApp extends Application {
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final TaskRepository taskRepository = new TaskRepository();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final Label statusLabel = new Label();

    @Override
    public void start(Stage stage) {
        TableView<Task> taskTable = buildTaskTable();

        Label title = new Label("Task Notification");
        title.getStyleClass().add("screen-title");

        statusLabel.getStyleClass().add("status-label");

        VBox header = new VBox(6, title, statusLabel);
        header.setPadding(new Insets(16, 16, 10, 16));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(taskTable);
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 900, 560);
        scene.getStylesheets().add(getClass().getResource("/com/tasknotification/styles/app.css").toExternalForm());

        stage.setTitle("Task Notification");
        stage.setMinWidth(760);
        stage.setMinHeight(420);
        stage.setScene(scene);
        stage.show();

        loadTasks();
    }

    private TableView<Task> buildTaskTable() {
        TableView<Task> table = new TableView<>(tasks);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No tasks found in the database."));

        TableColumn<Task, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        idColumn.setMaxWidth(80);

        TableColumn<Task, String> createdColumn = new TableColumn<>("Created");
        createdColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDateTime(cell.getValue().dateCreated())));
        createdColumn.setPrefWidth(150);

        TableColumn<Task, String> personColumn = new TableColumn<>("Person");
        personColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().person()));
        personColumn.setPrefWidth(140);

        TableColumn<Task, String> taskColumn = new TableColumn<>("Task");
        taskColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().taskDescription()));
        taskColumn.setPrefWidth(260);

        TableColumn<Task, String> deadlineColumn = new TableColumn<>("Deadline");
        deadlineColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDateTime(cell.getValue().deadline())));
        deadlineColumn.setPrefWidth(160);

        TableColumn<Task, String> completedColumn = new TableColumn<>("Completed");
        completedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().completed() ? "Yes" : "No"));
        completedColumn.setPrefWidth(110);

        table.getColumns().add(idColumn);
        table.getColumns().add(createdColumn);
        table.getColumns().add(personColumn);
        table.getColumns().add(taskColumn);
        table.getColumns().add(deadlineColumn);
        table.getColumns().add(completedColumn);

        return table;
    }

    private void loadTasks() {
        try {
            DatabaseInitializer.initialize();
            List<Task> databaseTasks = taskRepository.findAll();
            tasks.setAll(databaseTasks);
            statusLabel.setText(databaseTasks.isEmpty()
                    ? "Database is ready. There are no tasks yet."
                    : "Showing " + databaseTasks.size() + " task(s) from the database.");
        } catch (SQLException exception) {
            statusLabel.setText("Could not load tasks from the database.");
            throw new IllegalStateException("Failed to load tasks", exception);
        }
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "" : DISPLAY_DATE_TIME.format(dateTime);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
