package com.tasknotification.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskTest {

    private static final LocalDateTime SAMPLE_DATE_CREATED = LocalDateTime.of(2026, 7, 1, 9, 0);
    private static final LocalDateTime SAMPLE_DEADLINE = LocalDateTime.of(2026, 7, 10, 17, 0);

    // ── Constructor and accessor methods ─────────────────────────────

    // Note: Verifies that the record constructor stores all fields and each accessor returns the correct value.
    @Test
    void constructorStoresAllFieldsCorrectly() {
        Task task = new Task(1, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, false);

        assertEquals(1, task.id());
        assertEquals(SAMPLE_DATE_CREATED, task.dateCreated());
        assertEquals("Alex", task.person());
        assertEquals("Write report", task.taskDescription());
        assertEquals(SAMPLE_DEADLINE, task.deadline());
        assertFalse(task.completed());
    }

    // Note: Verifies that the completed accessor returns true when the task is marked as completed.
    @Test
    void completedReturnsTrueWhenTaskIsCompleted() {
        Task task = new Task(2, SAMPLE_DATE_CREATED, "Sam", "Review code", SAMPLE_DEADLINE, true);

        assertTrue(task.completed());
    }

    // Note: Verifies that nullable fields (dateCreated, deadline) can hold null values.
    @Test
    void nullableFieldsCanBeNull() {
        Task task = new Task(3, null, "Jordan", "No dates task", null, false);

        assertNull(task.dateCreated());
        assertNull(task.deadline());
    }

    // ── equals() ─────────────────────────────────────────────────────

    // Note: Verifies that two Task records with identical field values are considered equal.
    @Test
    void equalsReturnsTrueForIdenticalTasks() {
        Task task1 = new Task(1, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, false);
        Task task2 = new Task(1, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, false);

        assertEquals(task1, task2);
    }

    // Note: Verifies that two Task records with different ids are not equal.
    @Test
    void equalsReturnsFalseWhenIdDiffers() {
        Task task1 = new Task(1, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, false);
        Task task2 = new Task(2, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, false);

        assertNotEquals(task1, task2);
    }

    // Note: Verifies that two Task records with different completed values are not equal.
    @Test
    void equalsReturnsFalseWhenCompletedDiffers() {
        Task task1 = new Task(1, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, false);
        Task task2 = new Task(1, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, true);

        assertNotEquals(task1, task2);
    }

    // Note: Verifies that a Task is not equal to null.
    @Test
    void equalsReturnsFalseWhenComparedToNull() {
        Task task = new Task(1, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, false);

        assertNotEquals(null, task);
    }

    // ── hashCode() ───────────────────────────────────────────────────

    // Note: Verifies that two equal Task records produce the same hash code.
    @Test
    void hashCodeIsConsistentForEqualTasks() {
        Task task1 = new Task(1, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, false);
        Task task2 = new Task(1, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, false);

        assertEquals(task1.hashCode(), task2.hashCode());
    }

    // Note: Verifies that two different Task records produce different hash codes.
    @Test
    void hashCodeDiffersForDifferentTasks() {
        Task task1 = new Task(1, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, false);
        Task task2 = new Task(2, SAMPLE_DATE_CREATED, "Sam", "Review code", SAMPLE_DEADLINE, true);

        assertNotEquals(task1.hashCode(), task2.hashCode());
    }

    // ── toString() ───────────────────────────────────────────────────

    // Note: Verifies that toString contains all field values for debugging readability.
    @Test
    void toStringContainsAllFieldValues() {
        Task task = new Task(5, SAMPLE_DATE_CREATED, "Alex", "Write report", SAMPLE_DEADLINE, false);

        String result = task.toString();

        assertTrue(result.contains("5"));
        assertTrue(result.contains("Alex"));
        assertTrue(result.contains("Write report"));
        assertTrue(result.contains("false"));
    }
}
