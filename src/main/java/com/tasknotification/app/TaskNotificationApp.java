package com.tasknotification.app;

import com.tasknotification.database.DatabaseInitializer;
import com.tasknotification.model.Task;
import com.tasknotification.notification.DeadlineNotificationService;
import com.tasknotification.repository.TaskRepository;
import com.tasknotification.service.TaskExcelExporter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import javafx.scene.image.Image; 
import javax.swing.JOptionPane;


/**
 * Builds the JavaFX interface and coordinates task operations.
 *
 * <p>The main window shows up to three unfinished tasks. The second window
 * provides the complete task list and task-management actions.</p>
 */
public class TaskNotificationApp extends Application {
    
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final int MAIN_TASK_LIMIT = 3;
    private static final double MAIN_WINDOW_WIDTH = 520;
    private static final double MAIN_WINDOW_BASE_HEIGHT = 128;
    private static final double MAIN_MIN_ROW_HEIGHT = 38;
    private static final double MAIN_TABLE_HEADER_HEIGHT = 32;
    private static final double MAIN_ROW_VERTICAL_PADDING = 18;
    private static final double MAIN_TEXT_LINE_HEIGHT = 18;
    private static final int MAIN_PERSON_CHARS_PER_LINE = 16;
    private static final int MAIN_TASK_CHARS_PER_LINE = 36;

    private final TaskRepository taskRepository = new TaskRepository();
    private final DeadlineNotificationService deadlineNotificationService = new DeadlineNotificationService(taskRepository);
    private final TaskExcelExporter taskExcelExporter = new TaskExcelExporter();
    private final ObservableList<Task> mainTasks = FXCollections.observableArrayList();
    private final ObservableList<Task> allTasks = FXCollections.observableArrayList();
    private final Label allTasksStatusLabel = new Label();
    private TableView<Task> mainTaskTable;
    private Stage mainStage;
    private Stage allTasksStage;

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        mainStage = stage;
        mainTaskTable = buildMainTaskTable();
        HBox actions = buildMainActions();

        Label title = new Label("Main");
        title.getStyleClass().add("screen-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.getChildren().addAll(title, spacer, actions);

        VBox header = new VBox(0);
        header.setPadding(new Insets(8, 12, 6, 12));
        header.getChildren().add(topRow);

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(mainTaskTable);
        root.getStyleClass().add("main-root");

        Scene scene = new Scene(root, MAIN_WINDOW_WIDTH, MAIN_WINDOW_BASE_HEIGHT + MAIN_MIN_ROW_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/com/tasknotification/styles/main.css").toExternalForm());

        stage.setTitle("Main");
        stage.setMinWidth(480);
        stage.setMinHeight(MAIN_WINDOW_BASE_HEIGHT + MAIN_MIN_ROW_HEIGHT);
        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/app-icon.png")));
        stage.setOnCloseRequest(event -> {
            if (!deadlineNotificationService.isTrayAvailable()) {
                return;
            }
            event.consume();
            hideTaskWindows();
        });

        deadlineNotificationService.setOpenAction(() -> Platform.runLater(this::showMainWindow));
        deadlineNotificationService.setExitAction(() -> Platform.runLater(this::exitApplication));
        loadMainTasks();
        deadlineNotificationService.start();

        if (!isBackgroundStart() || !deadlineNotificationService.isTrayAvailable()) {
            stage.show();
        }
    }

    private HBox buildMainActions() {
        Button seeAllButton = new Button("See All Tasks");
        seeAllButton.setOnAction(event -> showAllTasksWindow());

        HBox actions = new HBox(8, seeAllButton);
        actions.getStyleClass().add("actions");
        return actions;
    }

    @Override
    public void stop() {
        deadlineNotificationService.stop();
    }

    private boolean isBackgroundStart() {
        return getParameters().getRaw().contains("--background");
    }

    private void showMainWindow() {
        loadMainTasks();
        mainStage.show();
        mainStage.toFront();
        mainStage.requestFocus();
    }

    private void hideTaskWindows() {
        if (allTasksStage != null) {
            allTasksStage.close();
        }
        mainStage.hide();
    }

    private void exitApplication() {
        if (allTasksStage != null) {
            allTasksStage.close();
        }
        Platform.exit();
    }

    private TableView<Task> buildMainTaskTable() {
        TableView<Task> table = new TableView<>(mainTasks);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No unfinished tasks found."));
        table.getStyleClass().add("main-table");
        table.setMinHeight(MAIN_TABLE_HEADER_HEIGHT + MAIN_MIN_ROW_HEIGHT);
        
        TableColumn<Task, String> personColumn = new TableColumn<>("Person");
        personColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().person()));
        personColumn.setPrefWidth(140);
        useWrappingTextCells(personColumn);


        TableColumn<Task, String> taskColumn = new TableColumn<>("Task");
        taskColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().taskDescription()));
        taskColumn.setPrefWidth(280);
        useWrappingTextCells(taskColumn);

        TableColumn<Task, String> deadlineColumn = new TableColumn<>("Deadline");
        deadlineColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().deadline())));
        deadlineColumn.setPrefWidth(140);

        table.getColumns().add(personColumn);
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
        useWrappingTextCells(personColumn);

        TableColumn<Task, String> taskColumn = new TableColumn<>("Task");
        taskColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().taskDescription()));
        taskColumn.setPrefWidth(260);
        useWrappingTextCells(taskColumn);

        TableColumn<Task, String> deadlineColumn = new TableColumn<>("Deadline");
        deadlineColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().deadline())));
        deadlineColumn.setPrefWidth(160);

        table.getColumns().add(idColumn);
        table.getColumns().add(createdColumn);
        table.getColumns().add(personColumn);
        table.getColumns().add(taskColumn);
        table.getColumns().add(deadlineColumn);
        table.getColumns().add(buildCompletedColumn());

        return table;
    }

    private void useWrappingTextCells(TableColumn<Task, String> column) {
        column.setCellFactory(ignoredColumn -> new TableCell<>() {
            private final Text wrappedText = new Text();

            {
                setAlignment(Pos.CENTER_LEFT);
                wrappedText.getStyleClass().add("wrapped-cell-text");
                wrappedText.setLineSpacing(2);
                wrappedText.wrappingWidthProperty().bind(widthProperty().subtract(18));
            }

            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                if (empty || text == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                wrappedText.setText(text);
                setText(null);
                setGraphic(wrappedText);
            }
        });
    }

    private TableColumn<Task, Boolean> buildCompletedColumn() {
        TableColumn<Task, Boolean> completedColumn = new TableColumn<>("Completed");
        completedColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().completed()));
        completedColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            private final CheckBox completedCheckBox = new CheckBox();

            {
                // Confirm the change before writing the completed state to SQLite.
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
        completedColumn.setPrefWidth(95);
        return completedColumn;
    }

   

    private HBox buildAllTasksActions(TableView<Task> taskTable) {
        Button addButton = new Button("Add Task");
        addButton.setOnAction(event -> showAddTaskDialog());

        Button editButton = new Button("Edit Task");
        editButton.disableProperty().bind(taskTable.getSelectionModel().selectedItemProperty().isNull());
        editButton.setOnAction(event -> showEditTaskDialog(taskTable.getSelectionModel().getSelectedItem()));

        Button exportButton = new Button("Export Excel");
        exportButton.setOnAction(event -> exportTasksToExcel());
        Button deleteButton = new Button("Delete Task");
        deleteButton.disableProperty().bind(taskTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.setOnAction(event -> deleteSelectedTask(taskTable.getSelectionModel().getSelectedItem()));

        HBox actions = new HBox(8, addButton, editButton, exportButton, deleteButton);
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
        allTasksStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/list-icon.png")));
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
            resizeMainWindow(databaseTasks);
        } catch (SQLException exception) {
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

    private void exportTasksToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Tasks to Excel");
        fileChooser.setInitialFileName("tasks.xlsx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx")
        );

        File selectedFile = fileChooser.showSaveDialog(allTasksStage);
        if (selectedFile == null) {
            return;
        }

        try {
            DatabaseInitializer.initialize();
            List<Task> databaseTasks = taskRepository.findCompleted();
            taskExcelExporter.export(databaseTasks, selectedFile.toPath());
        } catch (SQLException | IOException exception) {
            showError("Could not export tasks to Excel.");
        }
    }

    private void refreshOpenWindows() {
        loadMainTasks();
        if (allTasksStage != null && allTasksStage.isShowing()) {
            loadAllTasks();
        }
    }

    private void resizeMainWindow(List<Task> tasks) {
        if (mainStage == null) {
            return;
        }

        double rowsHeight = estimateMainRowsHeight(tasks);
        double tableHeight = MAIN_TABLE_HEADER_HEIGHT + rowsHeight;
        if (mainTaskTable != null) {
            mainTaskTable.setPrefHeight(tableHeight);
            mainTaskTable.setMaxHeight(tableHeight);
        }

        mainStage.setWidth(MAIN_WINDOW_WIDTH);
        mainStage.setHeight(MAIN_WINDOW_BASE_HEIGHT + rowsHeight);
    }

    private double estimateMainRowsHeight(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return MAIN_MIN_ROW_HEIGHT;
        }

        double rowsHeight = 0;
        for (Task task : tasks) {
            rowsHeight += estimateMainRowHeight(task);
        }
        return rowsHeight;
    }

    private double estimateMainRowHeight(Task task) {
        int personLines = estimateWrappedLines(task.person(), MAIN_PERSON_CHARS_PER_LINE);
        int taskLines = estimateWrappedLines(task.taskDescription(), MAIN_TASK_CHARS_PER_LINE);
        int rowLines = Math.max(personLines, taskLines);
        return Math.max(MAIN_MIN_ROW_HEIGHT, MAIN_ROW_VERTICAL_PADDING + (rowLines * MAIN_TEXT_LINE_HEIGHT));
    }

    private int estimateWrappedLines(String text, int charactersPerLine) {
        if (text == null || text.isBlank()) {
            return 1;
        }

        int lineCount = 0;
        for (String line : text.split("\\R", -1)) {
            lineCount += Math.max(1, (line.length() + charactersPerLine - 1) / charactersPerLine);
        }
        return lineCount;
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
    

    /* thêm nhận dạng nút delete */
    private void deleteSelectedTask(Task task) {
        if (task == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to delete \"" + task.taskDescription() + "\"?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                taskRepository.delete(task.id());
                refreshOpenWindows();
                deadlineNotificationService.checkNow();
            } catch (SQLException exception) {
                showError("Could not delete the task.");
                refreshOpenWindows();
            }
        }
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "" : DISPLAY_DATE_TIME.format(dateTime);
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "" : DISPLAY_DATE.format(dateTime);
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
