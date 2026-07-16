package com.tasknotification.app;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskNotificationAppTest {

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
