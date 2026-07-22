package com.tasknotification.notification;

import com.tasknotification.model.Task;
import com.tasknotification.repository.TaskRepository;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
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
import java.util.function.BooleanSupplier;

/**
 * Checks unfinished tasks and sends desktop notifications for deadlines
 * within the 12-hour and 24-hour notification windows.
 */
public class DeadlineNotificationService {
    private static final Duration TWELVE_HOURS = Duration.ofHours(12);
    private static final Duration TWENTY_FOUR_HOURS = Duration.ofHours(24);
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final TaskRepository taskRepository;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Set<String> sentNotifications = ConcurrentHashMap.newKeySet();
    private Runnable openAction = () -> {
    };
    private Runnable exitAction = () -> {
    };
    private Runnable toggleStartupAction = () -> {
    };
    private Runnable uninstallAction = () -> {
    };
    private BooleanSupplier startupEnabledSupplier = () -> false;
    private MenuItem startupItem;
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

    public void setOpenAction(Runnable openAction) {
        this.openAction = openAction;
    }

    public void setExitAction(Runnable exitAction) {
        this.exitAction = exitAction;
    }

    public void setToggleStartupAction(Runnable toggleStartupAction) {
        this.toggleStartupAction = toggleStartupAction;
    }

    public void setUninstallAction(Runnable uninstallAction) {
        this.uninstallAction = uninstallAction;
    }

    public void setStartupEnabledSupplier(BooleanSupplier startupEnabledSupplier) {
        this.startupEnabledSupplier = startupEnabledSupplier;
        updateStartupMenuLabel();
    }

    public void updateStartupMenuLabel() {
        if (startupItem != null) {
            startupItem.setLabel(startupEnabledSupplier.getAsBoolean() ? "Turn Off Startup" : "Turn On Startup");
        }
    }

    public boolean isTrayAvailable() {
        return isTrayReady();
    }

    boolean isTrayReady() {
        return trayIcon != null;
    }

    public void checkNow() {
        if (!isTrayReady()) {
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
            // Exclude the inner window because those tasks receive the 12-hour message.
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
        // Send each threshold notification only once per task during this app session.
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

        Image image = loadTrayImage();
        trayIcon = new TrayIcon(image, "Task Notification");
        trayIcon.setImageAutoSize(true);
        trayIcon.setPopupMenu(buildTrayMenu());
        trayIcon.addActionListener(event -> openAction.run());

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException exception) {
            trayIcon = null;
        }
    }

    private PopupMenu buildTrayMenu() {
        PopupMenu menu = new PopupMenu();

        MenuItem openItem = new MenuItem("Open Task Notification");
        openItem.addActionListener(event -> openAction.run());

        startupItem = new MenuItem();
        updateStartupMenuLabel();
        startupItem.addActionListener(event -> toggleStartupAction.run());

        MenuItem uninstallItem = new MenuItem("Delete App");
        uninstallItem.addActionListener(event -> uninstallAction.run());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(event -> exitAction.run());

        menu.add(openItem);
        menu.add(startupItem);
        menu.add(uninstallItem);
        menu.addSeparator();
        menu.add(exitItem);
        return menu;
    }

    private Image loadTrayImage() {
        java.net.URL iconUrl = DeadlineNotificationService.class.getResource("/images/app-icon.png");
        if (iconUrl != null) {
            return Toolkit.getDefaultToolkit().getImage(iconUrl);
        }
        return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    public void showStatusNotification(String title, String message) {
        showNotification(title, message);
    }

    void showNotification(String title, String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }
}
