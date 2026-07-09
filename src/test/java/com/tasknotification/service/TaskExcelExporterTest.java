package com.tasknotification.service;

import com.tasknotification.model.Task;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskExcelExporterTest {
    @TempDir
    Path temporaryDirectory;

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

        new TaskExcelExporter().export(List.of(task), outputPath);

        assertTrue(Files.isRegularFile(outputPath));
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(outputPath))) {
            assertEquals("Tasks", workbook.getSheetAt(0).getSheetName());
            assertEquals("Task Description", workbook.getSheetAt(0).getRow(0).getCell(3).getStringCellValue());
            assertEquals("Submit report", workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue());
            assertEquals("Yes", workbook.getSheetAt(0).getRow(1).getCell(5).getStringCellValue());
        }
    }
}
