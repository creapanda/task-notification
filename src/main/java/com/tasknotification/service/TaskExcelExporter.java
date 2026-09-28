package com.tasknotification.service;

import com.tasknotification.model.Task;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.TreeMap;

/**
 * Writes task data to an Excel workbook at the path selected by the user.
 */
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
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Tasks");
            writeHeader(workbook, sheet);
            writeTasks(sheet, tasks);
            writeSummaryChart(workbook, tasks);

            for (int columnIndex = 0; columnIndex < HEADERS.length; columnIndex++) {
                sheet.autoSizeColumn(columnIndex);
            }

            try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
                workbook.write(outputStream);
            }
        }
    }

    private void writeHeader(XSSFWorkbook workbook, Sheet sheet) {
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

    private void writeSummaryChart(XSSFWorkbook workbook, List<Task> tasks) {
        XSSFSheet summarySheet = workbook.createSheet("Summary");
        Map<String, Integer> completedTasksByPerson = countCompletedTasksByPerson(tasks);

        Row headerRow = summarySheet.createRow(0);
        headerRow.createCell(0).setCellValue("Person");
        headerRow.createCell(1).setCellValue("Completed Tasks");

        int rowIndex = 1;
        for (Map.Entry<String, Integer> entry : completedTasksByPerson.entrySet()) {
            Row row = summarySheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        if (completedTasksByPerson.isEmpty()) {
            Row row = summarySheet.createRow(rowIndex++);
            row.createCell(0).setCellValue("No completed tasks");
            row.createCell(1).setCellValue(0);
        }

        summarySheet.autoSizeColumn(0);
        summarySheet.autoSizeColumn(1);
        createCompletedTasksChart(summarySheet, rowIndex - 1);
    }

    private Map<String, Integer> countCompletedTasksByPerson(List<Task> tasks) {
        Map<String, Integer> completedTasksByPerson = new TreeMap<>();
        for (Task task : tasks) {
            if (!task.completed()) {
                continue;
            }

            String person = task.person() == null || task.person().isBlank() ? "Unassigned" : task.person();
            completedTasksByPerson.merge(person, 1, Integer::sum);
        }
        return completedTasksByPerson;
    }

    private void createCompletedTasksChart(XSSFSheet sheet, int lastDataRowIndex) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 0, 13, 18);
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Completed Tasks by Person");
        chart.setTitleOverlay(false);
        chart.getOrAddLegend().setPosition(LegendPosition.BOTTOM);

        XDDFDataSource<String> persons = XDDFDataSourcesFactory.fromStringCellRange(
                sheet,
                new CellRangeAddress(1, lastDataRowIndex, 0, 0)
        );
        XDDFNumericalDataSource<Double> counts = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet,
                new CellRangeAddress(1, lastDataRowIndex, 1, 1)
        );

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(BarDirection.COL);
        XDDFChartData.Series series = data.addSeries(persons, counts);
        series.setTitle("Completed Tasks", null);
        chart.plot(data);
    }
}
