package com.tasknotification.model;

import java.time.LocalDateTime;

public record Task(
        long id,
        LocalDateTime dateCreated,
        String person,
        String taskDescription,
        LocalDateTime deadline,
        boolean completed
) {
}
