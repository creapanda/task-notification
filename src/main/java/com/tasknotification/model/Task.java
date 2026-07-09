package com.tasknotification.model;

import java.time.LocalDateTime;

/**
 * Immutable representation of one row in the tasks table.
 */
public record Task(
        long id,
        LocalDateTime dateCreated,
        String person,
        String taskDescription,
        LocalDateTime deadline,
        boolean completed
) {
}
