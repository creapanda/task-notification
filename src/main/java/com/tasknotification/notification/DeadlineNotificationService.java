package com.tasknotification.notification;

import com.tasknotification.model.Task;
import com.tasknotification.repository.TaskRepository;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeadlineNotificationService {
    private static final Duration TWELVE_HOURS = Duration.ofHours(12);
    private static final Duration TWENTY_FOUR_HOURS = Duration.ofHours(24);
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final TaskRepository taskRepository;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Set<String> sentNotifications = ConcurrentHashMap.newKeySet();
    private TrayIcon trayIcon;

    public DeadlineNotificationService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void start() {
        initializeTrayIcon();
        checkNow();
        executorService.scheduleAtFixedRate(this::checkNow, 5, 5, TimeUnit.MINUTES);
    }

    public void stop() {
        executorService.shutdownNow();
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }

    public void checkNow() {
        if (trayIcon == null) {
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            notifyDueTasks(taskRepository.findUnfinishedDueWithin(now, TWELVE_HOURS), 12);
            notifyDueTasksOutsideTwelveHours(now);
        } catch (SQLException exception) {
            showNotification("Task Notification", "Could not check task deadlines.");
        }
    }

    private void notifyDueTasksOutsideTwelveHours(LocalDateTime now) throws SQLException {
        List<Task> tasks = taskRepository.findUnfinishedDueWithin(now, TWENTY_FOUR_HOURS);
        for (Task task : tasks) {
            if (task.deadline() != null && task.deadline().isAfter(now.plus(TWELVE_HOURS))) {
                notifyTask(task, 24);
            }
        }
    }

    private void notifyDueTasks(List<Task> tasks, int hours) {
        for (Task task : tasks) {
            notifyTask(task, hours);
        }
    }

    private void notifyTask(Task task, int hours) {
        String notificationKey = task.id() + ":" + hours;
        if (!sentNotifications.add(notificationKey)) {
            return;
        }

        String deadline = task.deadline() == null ? "No deadline" : DISPLAY_DATE_TIME.format(task.deadline());
        showNotification(
                "Task due within " + hours + " hours",
                task.taskDescription() + "\nDeadline: " + deadline
        );
    }

    private void initializeTrayIcon() {
        if (!SystemTray.isSupported()) {
            return;
        }

        Image image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        trayIcon = new TrayIcon(image, "Task Notification");
        trayIcon.setImageAutoSize(true);

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException exception) {
            trayIcon = null;
        }
    }

    private void showNotification(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }
}
