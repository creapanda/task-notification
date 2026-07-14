package com.tasknotification.service;

import com.tasknotification.model.Task;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskExcelExporterTest {
    @TempDir
    Path temporaryDirectory;

    private final TaskExcelExporter exporter = new TaskExcelExporter();

    // ── export() basic behavior ──────────────────────────────────────

    // Verify that export() creates a valid .xlsx file with correct sheet name, header, and task data
    @Test
    void exportCreatesReadableWorkbookWithTaskData() throws Exception {
        Path outputPath = temporaryDirectory.resolve("completed-tasks.xlsx");
        Task task = new Task(
                7,
                LocalDateTime.of(2026, 7, 9, 9, 30),
                "Alex",
                "Submit report",
                LocalDateTime.of(2026, 7, 10, 17, 0),
                true
        );

        exporter.export(List.of(task), outputPath);

        assertTrue(Files.isRegularFile(outputPath));
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            assertEquals("Tasks", workbook.getSheetAt(0).getSheetName());
            assertEquals("Task Description", workbook.getSheetAt(0).getRow(0).getCell(3).getStringCellValue());
            assertEquals("Submit report", workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());
            assertEquals("Yes", workbook.getSheetAt(0).getRow(1).getCell(5).getStringCellValue());
        }
    }

    // ── writeHeader() ────────────────────────────────────────────────

    // Verify that all six header columns (ID, Date Created, Person, Task Description, Deadline, Completed) are written correctly
    @Test
    void exportWritesAllSixHeaders() throws Exception {
        Path outputPath = temporaryDirectory.resolve("headers.xlsx");

        exporter.export(Collections.emptyList(), outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            Row headerRow = workbook.getSheetAt(0).getRow(0);

            assertEquals("ID", headerRow.getCell(0).getStringCellValue());
            assertEquals("Date Created", headerRow.getCell(1).getStringCellValue());
            assertEquals("Person", headerRow.getCell(2).getStringCellValue());
            assertEquals("Task Description", headerRow.getCell(3).getStringCellValue());
            assertEquals("Deadline", headerRow.getCell(4).getStringCellValue());
            assertEquals("Completed", headerRow.getCell(5).getStringCellValue());
        }
    }

    // Verify that all header cells are styled with a bold font
    @Test
    void exportHeaderCellsUseBoldFont() throws Exception {
        Path outputPath = temporaryDirectory.resolve("bold-headers.xlsx");

        exporter.export(Collections.emptyList(), outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            Row headerRow = workbook.getSheetAt(0).getRow(0);
            for (int col = 0; col < 6; col++) {
                int fontIndex = headerRow.getCell(col).getCellStyle().getFontIndex();
                Font font = workbook.getFontAt(fontIndex);
                assertTrue(font.getBold(), "Header cell at column " + col + " should use a bold font");
            }
        }
    }

    // ── writeTasks() ─────────────────────────────────────────────────

    // Verify that all six data columns (id, dateCreated, person, taskDescription, deadline, completed) are populated for a single task
    @Test
    void exportWritesAllSixColumnsForASingleTask() throws Exception {
        Path outputPath = temporaryDirectory.resolve("all-columns.xlsx");
        Task task = new Task(
                42,
                LocalDateTime.of(2026, 1, 15, 8, 5),
                "Bob",
                "Fix bug #101",
                LocalDateTime.of(2026, 2, 1, 12, 0),
                false
        );

        exporter.export(List.of(task), outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            Row dataRow = workbook.getSheetAt(0).getRow(1);

            assertEquals(42, (long) dataRow.getCell(0).getNumericCellValue());
            assertEquals("15 Jan 2026, 08:05", dataRow.getCell(1).getStringCellValue());
            assertEquals("Bob", dataRow.getCell(2).getStringCellValue());
            assertEquals("Fix bug #101", dataRow.getCell(3).getStringCellValue());
            assertEquals("01 Feb 2026, 12:00", dataRow.getCell(4).getStringCellValue());
            assertEquals("No", dataRow.getCell(5).getStringCellValue());
        }
    }

    // Verify that a completed task writes "Yes" in the Completed column
    @Test
    void exportWritesCompletedAsYesWhenTaskIsCompleted() throws Exception {
        Path outputPath = temporaryDirectory.resolve("completed-yes.xlsx");
        Task task = new Task(1, LocalDateTime.now(), "A", "Task", LocalDateTime.now(), true);

        exporter.export(List.of(task), outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            assertEquals("Yes", workbook.getSheetAt(0).getRow(1).getCell(5).getStringCellValue());
        }
    }

    // Verify that an incomplete task writes "No" in the Completed column
    @Test
    void exportWritesCompletedAsNoWhenTaskIsNotCompleted() throws Exception {
        Path outputPath = temporaryDirectory.resolve("completed-no.xlsx");
        Task task = new Task(2, LocalDateTime.now(), "B", "Task", LocalDateTime.now(), false);

        exporter.export(List.of(task), outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            assertEquals("No", workbook.getSheetAt(0).getRow(1).getCell(5).getStringCellValue());
        }
    }

    // Verify that exporting multiple tasks creates one data row per task with correct person and completed values
    @Test
    void exportWithMultipleTasksWritesOneRowPerTask() throws Exception {
        Path outputPath = temporaryDirectory.resolve("multiple-tasks.xlsx");
        List<Task> tasks = List.of(
                new Task(1, LocalDateTime.of(2026, 3, 1, 10, 0), "Alice", "Task A",
                        LocalDateTime.of(2026, 3, 5, 18, 0), true),
                new Task(2, LocalDateTime.of(2026, 3, 2, 11, 0), "Bob", "Task B",
                        LocalDateTime.of(2026, 3, 6, 18, 0), false),
                new Task(3, LocalDateTime.of(2026, 3, 3, 12, 0), "Carol", "Task C",
                        LocalDateTime.of(2026, 3, 7, 18, 0), true)
        );

        exporter.export(tasks, outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            Sheet sheet = workbook.getSheetAt(0);

            // header row (0) + 3 data rows (1, 2, 3)
            assertEquals(3, sheet.getLastRowNum());

            // Spot-check each row's person and completed status
            assertEquals("Alice", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("Yes", sheet.getRow(1).getCell(5).getStringCellValue());

            assertEquals("Bob", sheet.getRow(2).getCell(2).getStringCellValue());
            assertEquals("No", sheet.getRow(2).getCell(5).getStringCellValue());

            assertEquals("Carol", sheet.getRow(3).getCell(2).getStringCellValue());
            assertEquals("Yes", sheet.getRow(3).getCell(5).getStringCellValue());
        }
    }

    // ── export() with empty list ─────────────────────────────────────

    // Verify that exporting an empty task list produces a valid file with only the header row
    @Test
    void exportWithEmptyListWritesOnlyHeaderRow() throws Exception {
        Path outputPath = temporaryDirectory.resolve("empty-tasks.xlsx");

        exporter.export(Collections.emptyList(), outputPath);

        assertTrue(Files.isRegularFile(outputPath));
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("Tasks", sheet.getSheetName());
            // Only the header row exists
            assertEquals(0, sheet.getLastRowNum());
            assertEquals("ID", sheet.getRow(0).getCell(0).getStringCellValue());
        }
    }

    // ── formatDateTime() ─────────────────────────────────────────────

    // Verify that dateCreated and deadline are formatted using the "dd MMM yyyy, HH:mm" pattern
    @Test
    void exportFormatsDateTimeWithExpectedPattern() throws Exception {
        Path outputPath = temporaryDirectory.resolve("date-format.xlsx");
        Task task = new Task(
                1,
                LocalDateTime.of(2026, 12, 25, 14, 30),
                "Test",
                "Verify format",
                LocalDateTime.of(2026, 12, 31, 23, 59),
                false
        );

        exporter.export(List.of(task), outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            Row dataRow = workbook.getSheetAt(0).getRow(1);
            assertEquals("25 Dec 2026, 14:30", dataRow.getCell(1).getStringCellValue());
            assertEquals("31 Dec 2026, 23:59", dataRow.getCell(4).getStringCellValue());
        }
    }

    // Verify that a null dateCreated is written as an empty string instead of throwing an exception
    @Test
    void exportWritesEmptyStringWhenDateCreatedIsNull() throws Exception {
        Path outputPath = temporaryDirectory.resolve("null-date-created.xlsx");
        Task task = new Task(1, null, "Test", "Null date created", LocalDateTime.of(2026, 6, 1, 9, 0), false);

        exporter.export(List.of(task), outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(1).getStringCellValue());
        }
    }

    // Verify that a null deadline is written as an empty string instead of throwing an exception
    @Test
    void exportWritesEmptyStringWhenDeadlineIsNull() throws Exception {
        Path outputPath = temporaryDirectory.resolve("null-deadline.xlsx");
        Task task = new Task(1, LocalDateTime.of(2026, 6, 1, 9, 0), "Test", "Null deadline", null, true);

        exporter.export(List.of(task), outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            assertEquals("", workbook.getSheetAt(0).getRow(1).getCell(4).getStringCellValue());
        }
    }
}
