package com.tasknotification.service;

import com.tasknotification.model.Task;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TaskExcelExporter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final String[] HEADERS = {
            "ID",
            "Date Created",
            "Person",
            "Task Description",
            "Deadline",
            "Completed"
    };

    public void export(List<Task> tasks, Path outputPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tasks");
            writeHeader(workbook, sheet);
            writeTasks(sheet, tasks);

            for (int columnIndex = 0; columnIndex < HEADERS.length; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }

            try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
                workbook.write(outputStream);
            }
        }
    }

    private void writeHeader(Workbook workbook, Sheet sheet) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(0);
        for (int columnIndex = 0; columnIndex < HEADERS.length; columnIndex++) {
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(HEADERS[columnIndex]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeTasks(Sheet sheet, List<Task> tasks) {
        for (int taskIndex = 0; taskIndex < tasks.size(); taskIndex++) {
            Task task = tasks.get(taskIndex);
            Row row = sheet.createRow(taskIndex + 1);

            row.createCell(0).setCellValue(task.id());
            row.createCell(1).setCellValue(formatDateTime(task.dateCreated()));
            row.createCell(2).setCellValue(task.person());
            row.createCell(3).setCellValue(task.taskDescription());
            row.createCell(4).setCellValue(formatDateTime(task.deadline()));
            row.createCell(5).setCellValue(task.completed() ? "Yes" : "No");
        }
    }

    private String formatDateTime(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
    }
}
