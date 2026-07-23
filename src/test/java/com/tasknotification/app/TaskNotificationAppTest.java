package com.tasknotification.app;

import com.tasknotification.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskNotificationAppTest {

    private static final LocalDateTime SAMPLE_DATE = LocalDateTime.of(2026, 7, 1, 9, 0);
    private static final LocalDateTime SAMPLE_DEADLINE = LocalDateTime.of(2026, 7, 10, 17, 0);

    /**
     * Creates a TaskNotificationApp instance without invoking the constructor,
     * bypassing JavaFX field initializers so pure-logic private methods can be tested
     * in a headless environment without a running JavaFX toolkit.
     */
    private static TaskNotificationApp allocateApp() throws Exception {
        sun.misc.Unsafe unsafe = getUnsafe();
        return (TaskNotificationApp) unsafe.allocateInstance(TaskNotificationApp.class);
    }

    private static sun.misc.Unsafe getUnsafe() throws Exception {
        java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (sun.misc.Unsafe) f.get(null);
    }

    // Helper: invoke a declared (possibly private) method on the given instance via reflection.
    private static Object invokePrivate(Object instance, String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = TaskNotificationApp.class.getDeclaredMethod(methodName, paramTypes);
        method.setAccessible(true);
        return method.invoke(instance, args);
    }

    // Helper: read a static int constant declared on TaskNotificationApp.
    private static int readStaticInt(String fieldName) throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(null);
    }

    // Helper: read a static double constant declared on TaskNotificationApp.
    private static double readStaticDouble(String fieldName) throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getDouble(null);
    }

    // ── Class structure ──────────────────────────────────────────────

    // Note: Verifies that TaskNotificationApp extends javafx.application.Application.
    @Test
    void classExtendsJavaFxApplication() {
        assertTrue(javafx.application.Application.class.isAssignableFrom(TaskNotificationApp.class));
    }

    // Note: Verifies that the start(Stage) method is declared as public in TaskNotificationApp.
    @Test
    void startMethodIsPublic() throws NoSuchMethodException {
        Method startMethod = TaskNotificationApp.class.getMethod("start", javafx.stage.Stage.class);

        assertTrue(Modifier.isPublic(startMethod.getModifiers()));
    }

    // Note: Verifies that the stop() method is declared as public in TaskNotificationApp.
    @Test
    void stopMethodIsPublic() throws NoSuchMethodException {
        Method stopMethod = TaskNotificationApp.class.getMethod("stop");

        assertTrue(Modifier.isPublic(stopMethod.getModifiers()));
    }

    // Note: Verifies that the class has a public static main(String[]) entry point.
    @Test
    void mainMethodIsPublicStatic() throws NoSuchMethodException {
        Method mainMethod = TaskNotificationApp.class.getMethod("main", String[].class);

        assertTrue(Modifier.isPublic(mainMethod.getModifiers()));
        assertTrue(Modifier.isStatic(mainMethod.getModifiers()));
    }

    // ── Private method signatures (via reflection) ───────────────────

    // Note: Verifies that the private formatDate method exists with the expected signature.
    @Test
    void formatDateMethodExists() throws NoSuchMethodException {
        Method method = TaskNotificationApp.class.getDeclaredMethod("formatDate", LocalDateTime.class);

        assertEquals(String.class, method.getReturnType());
    }

    // Note: Verifies that the private toLocalDate method exists with the expected signature.
    @Test
    void toLocalDateMethodExists() throws NoSuchMethodException {
        Method method = TaskNotificationApp.class.getDeclaredMethod("toLocalDate", LocalDateTime.class);

        assertEquals(LocalDate.class, method.getReturnType());
    }

    // Note: Verifies that the private toStartOfDay method exists with the expected signature.
    @Test
    void toStartOfDayMethodExists() throws NoSuchMethodException {
        Method method = TaskNotificationApp.class.getDeclaredMethod("toStartOfDay", LocalDate.class);

        assertEquals(LocalDateTime.class, method.getReturnType());
    }

    // Note: Verifies that the private isBackgroundStart method exists with boolean return type.
    @Test
    void isBackgroundStartMethodExists() throws NoSuchMethodException {
        Method method = TaskNotificationApp.class.getDeclaredMethod("isBackgroundStart");

        assertEquals(boolean.class, method.getReturnType());
    }

    // ── Constants verification (via reflection) ──────────────────────

    // Note: Verifies that MAIN_TASK_LIMIT constant is set to 3 for the main window.
    @Test
    void mainTaskLimitIsThree() throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField("MAIN_TASK_LIMIT");
        field.setAccessible(true);

        assertEquals(3, field.getInt(null));
    }

    // Note: Verifies that MAIN_WINDOW_WIDTH constant is set to 520.
    @Test
    void mainWindowWidthIs520() throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField("MAIN_WINDOW_WIDTH");
        field.setAccessible(true);

        assertEquals(520.0, field.getDouble(null));
    }

    // Note: Verifies that MAIN_WINDOW_BASE_HEIGHT constant is set to 128.
    @Test
    void mainWindowBaseHeightIs128() throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField("MAIN_WINDOW_BASE_HEIGHT");
        field.setAccessible(true);

        assertEquals(128.0, field.getDouble(null));
    }

    // Note: Verifies that MAIN_MIN_ROW_HEIGHT constant is set to 38.
    @Test
    void mainMinRowHeightIs38() throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField("MAIN_MIN_ROW_HEIGHT");
        field.setAccessible(true);

        assertEquals(38.0, field.getDouble(null));
    }

    // Note: Verifies that MAIN_ROW_VERTICAL_PADDING constant is set to 18.
    @Test
    void mainRowVerticalPaddingIs18() throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField("MAIN_ROW_VERTICAL_PADDING");
        field.setAccessible(true);

        assertEquals(18.0, field.getDouble(null));
    }

    // Note: Verifies that MAIN_TEXT_LINE_HEIGHT constant is set to 18.
    @Test
    void mainTextLineHeightIs18() throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField("MAIN_TEXT_LINE_HEIGHT");
        field.setAccessible(true);

        assertEquals(18.0, field.getDouble(null));
    }

    // Note: Verifies that MAIN_PERSON_CHARS_PER_LINE constant is set to 16.
    @Test
    void mainPersonCharsPerLineIs16() throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField("MAIN_PERSON_CHARS_PER_LINE");
        field.setAccessible(true);

        assertEquals(16, field.getInt(null));
    }

    // Note: Verifies that MAIN_TASK_CHARS_PER_LINE constant is set to 36.
    @Test
    void mainTaskCharsPerLineIs36() throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField("MAIN_TASK_CHARS_PER_LINE");
        field.setAccessible(true);

        assertEquals(36, field.getInt(null));
    }

    // Note: Verifies that MAIN_TABLE_HEADER_HEIGHT constant is set to 32.
    @Test
    void mainTableHeaderHeightIs32() throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField("MAIN_TABLE_HEADER_HEIGHT");
        field.setAccessible(true);

        assertEquals(32.0, field.getDouble(null));
    }



    // ── formatDate() (via reflection) ────────────────────────────────

    // Note: Verifies that formatDate returns an empty string when passed null.
    @Test
    void formatDateReturnsEmptyStringForNull() throws Exception {
        TaskNotificationApp app = allocateApp();
        String result = (String) invokePrivate(app, "formatDate",
                new Class<?>[] {LocalDateTime.class}, (Object) null);

        assertEquals("", result);
    }

    // Note: Verifies that formatDate returns a correctly formatted string ("dd MMM yyyy") for a valid LocalDateTime.
    @Test
    void formatDateReturnsFormattedStringForNonNull() throws Exception {
        TaskNotificationApp app = allocateApp();
        LocalDateTime dt = LocalDateTime.of(2026, 7, 5, 14, 30);
        String result = (String) invokePrivate(app, "formatDate",
                new Class<?>[] {LocalDateTime.class}, dt);

        assertEquals("05 Jul 2026", result);
    }

    // Note: Verifies that the DISPLAY_DATE formatter formats dates correctly (starts with "dd MMM yyyy").
    @Test
    void displayDateFormatterUsesExpectedPattern() throws Exception {
        Field field = TaskNotificationApp.class.getDeclaredField("DISPLAY_DATE");
        field.setAccessible(true);

        java.time.format.DateTimeFormatter formatter = (java.time.format.DateTimeFormatter) field.get(null);
        LocalDate sample = LocalDate.of(2026, 1, 5);

        String formatted = formatter.format(sample);
        assertTrue(formatted.startsWith("05 Jan 2026"));
    }

    // ── TaskFormData inner record verification (via reflection) ──────

    // Note: Verifies that the private TaskFormData inner record exists and is a record type.
    @Test
    void taskFormDataInnerClassExists() {
        Class<?> taskFormDataClass = findInnerClass("TaskFormData");

        assertNotNull(taskFormDataClass, "TaskFormData inner class should exist");
        assertTrue(taskFormDataClass.isRecord(), "TaskFormData should be a record");
    }

    // Note: Verifies that TaskFormData record has exactly 4 components (person, taskDescription, deadline, completed).
    @Test
    void taskFormDataHasFourComponents() {
        Class<?> taskFormDataClass = findInnerClass("TaskFormData");

        assertNotNull(taskFormDataClass);
        assertEquals(4, taskFormDataClass.getRecordComponents().length);
    }

    // Note: Verifies that TaskFormData record components have the expected names.
    @Test
    void taskFormDataComponentNamesAreCorrect() {
        Class<?> taskFormDataClass = findInnerClass("TaskFormData");
        assertNotNull(taskFormDataClass);

        String[] expectedNames = {"person", "taskDescription", "deadline", "completed"};
        java.lang.reflect.RecordComponent[] components = taskFormDataClass.getRecordComponents();

        for (int i = 0; i < expectedNames.length; i++) {
            assertEquals(expectedNames[i], components[i].getName());
        }
    }

    // ── estimateWrappedLines() (via reflection) ──────────────────────

    // Note: Verifies that a null text returns 1 line.
    @Test
    void estimateWrappedLinesReturnsOneForNullText() throws Exception {
        TaskNotificationApp app = allocateApp();
        int result = (int) invokePrivate(app, "estimateWrappedLines",
                new Class<?>[] {String.class, int.class}, null, 16);

        assertEquals(1, result);
    }

    // Note: Verifies that a blank text returns 1 line.
    @Test
    void estimateWrappedLinesReturnsOneForBlankText() throws Exception {
        TaskNotificationApp app = allocateApp();
        int result = (int) invokePrivate(app, "estimateWrappedLines",
                new Class<?>[] {String.class, int.class}, "   ", 16);

        assertEquals(1, result);
    }

    // Note: Verifies that text shorter than charsPerLine occupies exactly 1 line.
    @Test
    void estimateWrappedLinesReturnsOneForShortText() throws Exception {
        TaskNotificationApp app = allocateApp();
        int result = (int) invokePrivate(app, "estimateWrappedLines",
                new Class<?>[] {String.class, int.class}, "Short", 16);

        assertEquals(1, result);
    }

    // Note: Verifies that text exactly equal to charsPerLine occupies 1 line.
    @Test
    void estimateWrappedLinesReturnsOneForExactLength() throws Exception {
        TaskNotificationApp app = allocateApp();
        String text = "a".repeat(16);
        int result = (int) invokePrivate(app, "estimateWrappedLines",
                new Class<?>[] {String.class, int.class}, text, 16);

        assertEquals(1, result);
    }

    // Note: Verifies that text one character longer than charsPerLine wraps into 2 lines.
    @Test
    void estimateWrappedLinesReturnsTwoWhenTextExceedsOneLine() throws Exception {
        TaskNotificationApp app = allocateApp();
        String text = "a".repeat(17);
        int result = (int) invokePrivate(app, "estimateWrappedLines",
                new Class<?>[] {String.class, int.class}, text, 16);

        assertEquals(2, result);
    }

    // Note: Verifies that a text with two physical newlines counts as 3 lines minimum.
    @Test
    void estimateWrappedLinesCountsPhysicalNewlines() throws Exception {
        TaskNotificationApp app = allocateApp();
        String text = "Line1\nLine2\nLine3";
        int result = (int) invokePrivate(app, "estimateWrappedLines",
                new Class<?>[] {String.class, int.class}, text, 100);

        assertEquals(3, result);
    }

    // Note: Verifies that combined newlines and long segments are counted together.
    @Test
    void estimateWrappedLinesCombinesNewlinesAndWrapping() throws Exception {
        TaskNotificationApp app = allocateApp();
        // Two lines: first wraps to 2 chunks of 16, second is short
        String text = "a".repeat(32) + "\n" + "b".repeat(5);
        int result = (int) invokePrivate(app, "estimateWrappedLines",
                new Class<?>[] {String.class, int.class}, text, 16);

        assertEquals(3, result);
    }

    // ── estimateMainRowHeight() (via reflection) ──────────────────────

    // Note: Verifies that a task with short person and task text returns the minimum row height.
    @Test
    void estimateMainRowHeightReturnsMinimumForShortText() throws Exception {
        TaskNotificationApp app = allocateApp();
        Task task = new Task(1, SAMPLE_DATE, "Alex", "Fix bug", SAMPLE_DEADLINE, false);
        double minRowHeight = readStaticDouble("MAIN_MIN_ROW_HEIGHT");

        double result = (double) invokePrivate(app, "estimateMainRowHeight",
                new Class<?>[] {Task.class}, task);

        assertEquals(minRowHeight, result);
    }

    // Note: Verifies that a task with a long task description causes the row height to exceed the minimum.
    @Test
    void estimateMainRowHeightExceedsMinimumForLongTaskText() throws Exception {
        TaskNotificationApp app = allocateApp();
        // 37 chars wraps to 2 lines with charsPerLine=36
        String longTask = "a".repeat(37);
        Task task = new Task(1, SAMPLE_DATE, "Alex", longTask, SAMPLE_DEADLINE, false);
        double minRowHeight = readStaticDouble("MAIN_MIN_ROW_HEIGHT");

        double result = (double) invokePrivate(app, "estimateMainRowHeight",
                new Class<?>[] {Task.class}, task);

        assertTrue(result > minRowHeight);
    }

    // Note: Verifies that a task where person wraps more than task drives the row height.
    @Test
    void estimateMainRowHeightUsesMaxOfPersonAndTask() throws Exception {
        TaskNotificationApp app = allocateApp();
        // Person is 17 chars -> 2 lines with charsPerLine=16; task is short -> 1 line
        String longPerson = "a".repeat(17);
        Task task = new Task(1, SAMPLE_DATE, longPerson, "Short", SAMPLE_DEADLINE, false);
        double rowVertPad = readStaticDouble("MAIN_ROW_VERTICAL_PADDING");
        double textLineH = readStaticDouble("MAIN_TEXT_LINE_HEIGHT");
        double expectedHeight = rowVertPad + (2 * textLineH);

        double result = (double) invokePrivate(app, "estimateMainRowHeight",
                new Class<?>[] {Task.class}, task);

        assertEquals(expectedHeight, result, 0.001);
    }

    // ── estimateMainRowsHeight() (via reflection) ─────────────────────

    // Note: Verifies that an empty task list returns the minimum row height.
    @Test
    void estimateMainRowsHeightReturnsMinRowHeightForEmptyList() throws Exception {
        TaskNotificationApp app = allocateApp();
        double minRowHeight = readStaticDouble("MAIN_MIN_ROW_HEIGHT");

        double result = (double) invokePrivate(app, "estimateMainRowsHeight",
                new Class<?>[] {List.class}, List.of());

        assertEquals(minRowHeight, result);
    }

    // Note: Verifies that a single short-text task returns its single row height.
    @Test
    void estimateMainRowsHeightReturnsSingleRowHeightForOneTask() throws Exception {
        TaskNotificationApp app = allocateApp();
        Task task = new Task(1, SAMPLE_DATE, "Alex", "Fix bug", SAMPLE_DEADLINE, false);
        double singleHeight = (double) invokePrivate(app, "estimateMainRowHeight",
                new Class<?>[] {Task.class}, task);

        double result = (double) invokePrivate(app, "estimateMainRowsHeight",
                new Class<?>[] {List.class}, List.of(task));

        assertEquals(singleHeight, result, 0.001);
    }

    // Note: Verifies that two tasks sum their individual row heights.
    @Test
    void estimateMainRowsHeightSumsHeightsForMultipleTasks() throws Exception {
        TaskNotificationApp app = allocateApp();
        Task task1 = new Task(1, SAMPLE_DATE, "Alex", "Fix bug", SAMPLE_DEADLINE, false);
        Task task2 = new Task(2, SAMPLE_DATE, "Sam", "a".repeat(37), SAMPLE_DEADLINE, false);
        double h1 = (double) invokePrivate(app, "estimateMainRowHeight", new Class<?>[] {Task.class}, task1);
        double h2 = (double) invokePrivate(app, "estimateMainRowHeight", new Class<?>[] {Task.class}, task2);

        double result = (double) invokePrivate(app, "estimateMainRowsHeight",
                new Class<?>[] {List.class}, List.of(task1, task2));

        assertEquals(h1 + h2, result, 0.001);
    }

    // ── startupMessage() (via reflection) ────────────────────────────

    // Note: Verifies that startupMessage returns correct message when startup was enabled and change succeeded.
    @Test
    void startupMessageWhenWasEnabledAndChanged() throws Exception {
        TaskNotificationApp app = allocateApp();
        String result = (String) invokePrivate(app, "startupMessage",
                new Class<?>[] {boolean.class, boolean.class}, true, true);

        assertEquals("Startup has been turned off.", result);
    }

    // Note: Verifies that startupMessage returns correct message when startup was disabled and change succeeded.
    @Test
    void startupMessageWhenWasDisabledAndChanged() throws Exception {
        TaskNotificationApp app = allocateApp();
        String result = (String) invokePrivate(app, "startupMessage",
                new Class<?>[] {boolean.class, boolean.class}, false, true);

        assertEquals("Startup has been turned on.", result);
    }

    // Note: Verifies that startupMessage returns failure message when startup was enabled but change failed.
    @Test
    void startupMessageWhenWasEnabledAndNotChanged() throws Exception {
        TaskNotificationApp app = allocateApp();
        String result = (String) invokePrivate(app, "startupMessage",
                new Class<?>[] {boolean.class, boolean.class}, true, false);

        assertEquals("Could not turn off startup.", result);
    }

    // Note: Verifies that startupMessage returns failure message when startup was disabled but change failed.
    @Test
    void startupMessageWhenWasDisabledAndNotChanged() throws Exception {
        TaskNotificationApp app = allocateApp();
        String result = (String) invokePrivate(app, "startupMessage",
                new Class<?>[] {boolean.class, boolean.class}, false, false);

        assertEquals("Could not turn on startup.", result);
    }

    // ── Helper ───────────────────────────────────────────────────────

    private Class<?> findInnerClass(String simpleName) {
        for (Class<?> inner : TaskNotificationApp.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals(simpleName)) {
                return inner;
            }
        }
        return null;
    }
}
