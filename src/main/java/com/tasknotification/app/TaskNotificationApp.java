package com.tasknotification.app;

import com.tasknotification.database.DatabaseInitializer;
import com.tasknotification.model.Task;
import com.tasknotification.notification.DeadlineNotificationService;
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
    private static final int MAIN_TASK_LIMIT = 3;

    private final TaskRepository taskRepository = new TaskRepository();
    private final DeadlineNotificationService deadlineNotificationService = new DeadlineNotificationService(taskRepository);
    private final ObservableList<Task> mainTasks = FXCollections.observableArrayList();
    private final ObservableList<Task> allTasks = FXCollections.observableArrayList();
    private final Label mainStatusLabel = new Label();
    private final Label allTasksStatusLabel = new Label();
    private Stage allTasksStage;

    @Override
    public void start(Stage stage) {
        TableView<Task> taskTable = buildMainTaskTable();
        HBox actions = buildMainActions();

        Label title = new Label("Main");
        title.getStyleClass().add("screen-title");

        mainStatusLabel.getStyleClass().add("status-label");

        VBox header = new VBox(10, title, mainStatusLabel, actions);
        header.setPadding(new Insets(16, 16, 10, 16));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(taskTable);
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 900, 560);
        scene.getStylesheets().add(getClass().getResource("/com/tasknotification/styles/app.css").toExternalForm());

        stage.setTitle("Main");
        stage.setMinWidth(760);
        stage.setMinHeight(420);
        stage.setScene(scene);
        stage.show();

        loadMainTasks();
        deadlineNotificationService.start();
    }

    @Override
    public void stop() {
        deadlineNotificationService.stop();
    }

    private TableView<Task> buildMainTaskTable() {
        TableView<Task> table = new TableView<>(mainTasks);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No unfinished tasks found."));

        TableColumn<Task, String> taskColumn = new TableColumn<>("Task");
        taskColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().taskDescription()));
        taskColumn.setPrefWidth(440);

        TableColumn<Task, String> deadlineColumn = new TableColumn<>("Deadline");
        deadlineColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDateTime(cell.getValue().deadline())));
        deadlineColumn.setPrefWidth(180);

        table.getColumns().add(taskColumn);
        table.getColumns().add(deadlineColumn);
        table.getColumns().add(buildCompletedColumn());

        return table;
    }

    private TableView<Task> buildAllTasksTable() {
        ObservableList<Task> tableTasks = allTasks;
        TableView<Task> table = new TableView<>(tableTasks);
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

        table.getColumns().add(idColumn);
        table.getColumns().add(createdColumn);
        table.getColumns().add(personColumn);
        table.getColumns().add(taskColumn);
        table.getColumns().add(deadlineColumn);
        table.getColumns().add(buildCompletedColumn());

        return table;
    }

    private TableColumn<Task, Boolean> buildCompletedColumn() {
        TableColumn<Task, Boolean> completedColumn = new TableColumn<>("Completed");
        completedColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().completed()));
        completedColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            private final CheckBox completedCheckBox = new CheckBox();

            {
                completedCheckBox.setOnAction(event -> {
                    Task task = getTableRow().getItem();
                    if (task == null) {
                        return;
                    }

                    boolean newCompletedValue = completedCheckBox.isSelected();
                    if (confirmCompletedChange(task, newCompletedValue)) {
                        updateTaskCompleted(task, newCompletedValue);
                    } else {
                        completedCheckBox.setSelected(task.completed());
                    }
                });
            }

            @Override
            protected void updateItem(Boolean completed, boolean empty) {
                super.updateItem(completed, empty);
                if (empty || completed == null) {
                    setGraphic(null);
                    return;
                }

                completedCheckBox.setSelected(completed);
                setGraphic(completedCheckBox);
            }
        });
        completedColumn.setPrefWidth(120);
        return completedColumn;
    }

    private HBox buildMainActions() {
        Button seeAllButton = new Button("See All Tasks");
        seeAllButton.setOnAction(event -> showAllTasksWindow());

        HBox actions = new HBox(8, seeAllButton);
        actions.getStyleClass().add("actions");
        return actions;
    }

    private HBox buildAllTasksActions(TableView<Task> taskTable) {
        Button addButton = new Button("Add Task");
        addButton.setOnAction(event -> showAddTaskDialog());

        Button editButton = new Button("Edit Task");
        editButton.disableProperty().bind(taskTable.getSelectionModel().selectedItemProperty().isNull());
        editButton.setOnAction(event -> showEditTaskDialog(taskTable.getSelectionModel().getSelectedItem()));

        HBox actions = new HBox(8, addButton, editButton);
        actions.getStyleClass().add("actions");
        return actions;
    }

    private void showAllTasksWindow() {
        if (allTasksStage != null && allTasksStage.isShowing()) {
            allTasksStage.toFront();
            return;
        }

        TableView<Task> taskTable = buildAllTasksTable();
        HBox actions = buildAllTasksActions(taskTable);

        Label title = new Label("All Tasks");
        title.getStyleClass().add("screen-title");

        allTasksStatusLabel.getStyleClass().add("status-label");

        VBox header = new VBox(10, title, allTasksStatusLabel, actions);
        header.setPadding(new Insets(16, 16, 10, 16));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(taskTable);
        root.getStyleClass().add("app-root");

        Scene scene = new Scene(root, 900, 560);
        scene.getStylesheets().add(getClass().getResource("/com/tasknotification/styles/app.css").toExternalForm());

        allTasksStage = new Stage();
        allTasksStage.setTitle("All Tasks");
        allTasksStage.setMinWidth(760);
        allTasksStage.setMinHeight(420);
        allTasksStage.setScene(scene);
        allTasksStage.setOnCloseRequest(event -> allTasksStage = null);
        allTasksStage.show();

        loadAllTasks();
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
                refreshOpenWindows();
                deadlineNotificationService.checkNow();
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
                refreshOpenWindows();
                deadlineNotificationService.checkNow();
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

    private void loadMainTasks() {
        try {
            DatabaseInitializer.initialize();
            List<Task> databaseTasks = taskRepository.findClosestUnfinished(MAIN_TASK_LIMIT);
            mainTasks.setAll(databaseTasks);
            mainStatusLabel.setText(databaseTasks.isEmpty()
                    ? "There are no unfinished tasks."
                    : "Showing up to 3 unfinished tasks closest to their deadlines.");
        } catch (SQLException exception) {
            mainStatusLabel.setText("Could not load tasks from the database.");
            throw new IllegalStateException("Failed to load tasks", exception);
        }
    }

    private void loadAllTasks() {
        try {
            DatabaseInitializer.initialize();
            List<Task> databaseTasks = taskRepository.findAll();
            allTasks.setAll(databaseTasks);
            allTasksStatusLabel.setText(databaseTasks.isEmpty()
                    ? "There are no tasks yet."
                    : "Showing " + databaseTasks.size() + " task(s).");
        } catch (SQLException exception) {
            allTasksStatusLabel.setText("Could not load tasks from the database.");
            throw new IllegalStateException("Failed to load tasks", exception);
        }
    }

    private void refreshOpenWindows() {
        loadMainTasks();
        if (allTasksStage != null && allTasksStage.isShowing()) {
            loadAllTasks();
        }
    }

    private boolean confirmCompletedChange(Task task, boolean completed) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Task Update");
        alert.setHeaderText(null);
        alert.setContentText("Mark \"" + task.taskDescription() + "\" as " + (completed ? "completed" : "unfinished") + "?");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void updateTaskCompleted(Task task, boolean completed) {
        try {
            taskRepository.updateCompleted(task.id(), completed);
            refreshOpenWindows();
            deadlineNotificationService.checkNow();
        } catch (SQLException exception) {
            showError("Could not update the task.");
            refreshOpenWindows();
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
