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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class TaskNotificationApp extends Application {
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final TaskRepository taskRepository = new TaskRepository();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final Label statusLabel = new Label();

    @Override
    public void start(Stage stage) {
        TableView<Task> taskTable = buildTaskTable();
        HBox actions = buildActions(taskTable);

        Label title = new Label("Task Notification");
        title.getStyleClass().add("screen-title");

        statusLabel.getStyleClass().add("status-label");

        VBox header = new VBox(10, title, statusLabel, actions);
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

    private HBox buildActions(TableView<Task> taskTable) {
        Button addButton = new Button("Add Task");
        addButton.setOnAction(event -> showAddTaskDialog());

        Button editButton = new Button("Edit Task");
        editButton.disableProperty().bind(taskTable.getSelectionModel().selectedItemProperty().isNull());
        editButton.setOnAction(event -> showEditTaskDialog(taskTable.getSelectionModel().getSelectedItem()));

        HBox actions = new HBox(8, addButton, editButton);
        actions.getStyleClass().add("actions");
        return actions;
    }

    private void showAddTaskDialog() {
        Optional<TaskFormData> result = showTaskDialog("Add Task", null);
        result.ifPresent(formData -> {
            try {
                taskRepository.add(
                        formData.person(),
                        formData.taskDescription(),
                        formData.deadline(),
                        formData.completed()
                );
                loadTasks();
            } catch (SQLException exception) {
                showError("Could not add the task.");
            }
        });
    }

    private void showEditTaskDialog(Task task) {
        Optional<TaskFormData> result = showTaskDialog("Edit Task", task);
        result.ifPresent(formData -> {
            try {
                taskRepository.update(new Task(
                        task.id(),
                        task.dateCreated(),
                        formData.person(),
                        formData.taskDescription(),
                        formData.deadline(),
                        formData.completed()
                ));
                loadTasks();
            } catch (SQLException exception) {
                showError("Could not update the task.");
            }
        });
    }

    private Optional<TaskFormData> showTaskDialog(String title, Task task) {
        Dialog<TaskFormData> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField personField = new TextField(task == null ? "" : task.person());
        personField.setPromptText("Person");

        TextArea taskDescriptionArea = new TextArea(task == null ? "" : task.taskDescription());
        taskDescriptionArea.setPromptText("Task description");
        taskDescriptionArea.setPrefRowCount(4);
        taskDescriptionArea.setWrapText(true);

        DatePicker deadlinePicker = new DatePicker(task == null ? null : toLocalDate(task.deadline()));

        CheckBox completedCheckBox = new CheckBox("Completed");
        completedCheckBox.setSelected(task != null && task.completed());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(12));
        form.addRow(0, new Label("Person"), personField);
        form.addRow(1, new Label("Task"), taskDescriptionArea);
        form.addRow(2, new Label("Deadline"), deadlinePicker);
        form.addRow(3, new Label(""), completedCheckBox);

        dialog.getDialogPane().setContent(form);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (personField.getText().isBlank() || taskDescriptionArea.getText().isBlank()) {
                showError("Person and task are required.");
                event.consume();
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }

            return new TaskFormData(
                    personField.getText().trim(),
                    taskDescriptionArea.getText().trim(),
                    toStartOfDay(deadlinePicker.getValue()),
                    completedCheckBox.isSelected()
            );
        });

        return dialog.showAndWait();
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

    private LocalDate toLocalDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : LocalDateTime.of(date, LocalTime.MIDNIGHT);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Task Notification");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private record TaskFormData(
            String person,
            String taskDescription,
            LocalDateTime deadline,
            boolean completed
    ) {
    }
}
