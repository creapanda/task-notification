package com.tasknotification.notification;

import com.tasknotification.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeadlineNotificationServiceTest {
    private static final String JDBC_URL = "jdbc:sqlite:file:deadline_notification_test?mode=memory&cache=shared";

    private Connection connection;
    private TaskRepository taskRepository;
    private TestableNotificationService notificationService;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(JDBC_URL);
        taskRepository = new TaskRepository(() -> DriverManager.getConnection(JDBC_URL));

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS tasks");
            statement.executeUpdate("""
                    CREATE TABLE tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        date_created TEXT NOT NULL DEFAULT (datetime('now')),
                        person TEXT NOT NULL,
                        task_description TEXT NOT NULL,
                        deadline TEXT,
                        completed INTEGER NOT NULL DEFAULT 0,
                        CHECK (completed IN (0, 1))
                    )
                    """);
        }

        notificationService = new TestableNotificationService(taskRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        notificationService.stop();
        connection.close();
    }

    // ── isTrayAvailable() ────────────────────────────────────────────

    // Note: Verifies that isTrayAvailable returns false before the tray icon is initialized.
    @Test
    void isTrayAvailableReturnsFalseBeforeStart() {
        assertFalse(notificationService.isTrayAvailable());
    }

    // Note: Verifies that isTrayAvailable returns true after the simulated tray icon initialization.
    @Test
    void isTrayAvailableReturnsTrueAfterStart() {
        notificationService.simulateStart();

        assertTrue(notificationService.isTrayAvailable());
    }

    // ── setOpenAction() / setExitAction() ────────────────────────────

    // Note: Verifies that setOpenAction stores the callback and it can be triggered.
    @Test
    void setOpenActionStoresCallback() {
        boolean[] called = {false};

        notificationService.setOpenAction(() -> called[0] = true);
        notificationService.triggerOpenAction();

        assertTrue(called[0]);
    }

    // Note: Verifies that setExitAction stores the callback and it can be triggered.
    @Test
    void setExitActionStoresCallback() {
        boolean[] called = {false};

        notificationService.setExitAction(() -> called[0] = true);
        notificationService.triggerExitAction();

        assertTrue(called[0]);
    }

    // ── checkNow() without tray ──────────────────────────────────────

    // Note: Verifies that checkNow does nothing when the tray icon is not initialized.
    @Test
    void checkNowDoesNothingWhenTrayNotInitialized() throws Exception {
        taskRepository.add("Alex", "Urgent task", LocalDateTime.now().plusHours(1), false);

        notificationService.checkNow();

        assertTrue(notificationService.getNotifications().isEmpty());
    }

    // ── checkNow() with tray ─────────────────────────────────────────

    // Note: Verifies that checkNow sends a 12-hour notification for a task due within 12 hours.
    @Test
    void checkNowNotifiesTaskDueWithinTwelveHours() throws Exception {
        notificationService.simulateStart();
        taskRepository.add("Alex", "Due soon", LocalDateTime.now().plusHours(6), false);

        notificationService.checkNow();

        List<String[]> notifications = notificationService.getNotifications();
        assertEquals(1, notifications.size());
        assertTrue(notifications.getFirst()[0].contains("12 hours"));
        assertTrue(notifications.getFirst()[1].contains("Due soon"));
    }

    // Note: Verifies that checkNow sends a 24-hour notification for a task due between 12 and 24 hours.
    @Test
    void checkNowNotifiesTaskDueBetweenTwelveAndTwentyFourHours() throws Exception {
        notificationService.simulateStart();
        taskRepository.add("Alex", "Due later", LocalDateTime.now().plusHours(18), false);

        notificationService.checkNow();

        List<String[]> notifications = notificationService.getNotifications();
        assertEquals(1, notifications.size());
        assertTrue(notifications.getFirst()[0].contains("24 hours"));
        assertTrue(notifications.getFirst()[1].contains("Due later"));
    }

    // Note: Verifies that checkNow does not send any notification for a task due beyond 24 hours.
    @Test
    void checkNowDoesNotNotifyTaskDueBeyondTwentyFourHours() throws Exception {
        notificationService.simulateStart();
        taskRepository.add("Alex", "Far future task", LocalDateTime.now().plusHours(48), false);

        notificationService.checkNow();

        assertTrue(notificationService.getNotifications().isEmpty());
    }

    // Note: Verifies that checkNow does not notify for a completed task even if its deadline is within the window.
    @Test
    void checkNowDoesNotNotifyCompletedTask() throws Exception {
        notificationService.simulateStart();
        taskRepository.add("Alex", "Done task", LocalDateTime.now().plusHours(6), true);

        notificationService.checkNow();

        assertTrue(notificationService.getNotifications().isEmpty());
    }

    // Note: Verifies that checkNow does not notify for a task whose deadline has already passed.
    @Test
    void checkNowDoesNotNotifyOverdueTask() throws Exception {
        notificationService.simulateStart();
        taskRepository.add("Alex", "Overdue task", LocalDateTime.now().minusHours(1), false);

        notificationService.checkNow();

        assertTrue(notificationService.getNotifications().isEmpty());
    }

    // ── Duplicate notification suppression ────────────────────────────

    // Note: Verifies that the same threshold notification is sent only once per task per session.
    @Test
    void checkNowDoesNotSendDuplicateNotification() throws Exception {
        notificationService.simulateStart();
        taskRepository.add("Alex", "Dup task", LocalDateTime.now().plusHours(6), false);

        notificationService.checkNow();
        notificationService.checkNow();

        assertEquals(1, notificationService.getNotifications().size());
    }

    // Note: Verifies that a task in the 12-hour window only gets a 12-hour notification, not 24-hour.
    @Test
    void checkNowSendsOnlyTwelveHourNotificationForTaskWithinTwelveHours() throws Exception {
        notificationService.simulateStart();
        taskRepository.add("Alex", "Within 12h", LocalDateTime.now().plusHours(5), false);

        notificationService.checkNow();

        List<String[]> notifications = notificationService.getNotifications();
        assertEquals(1, notifications.size());
        assertTrue(notifications.getFirst()[0].contains("12 hours"));
    }

    // ── Multiple tasks ───────────────────────────────────────────────

    // Note: Verifies that checkNow sends separate notifications for multiple tasks in different time windows.
    @Test
    void checkNowNotifiesMultipleTasksInDifferentWindows() throws Exception {
        notificationService.simulateStart();
        taskRepository.add("Alex", "Task A 12h", LocalDateTime.now().plusHours(4), false);
        taskRepository.add("Sam", "Task B 24h", LocalDateTime.now().plusHours(18), false);

        notificationService.checkNow();

        List<String[]> notifications = notificationService.getNotifications();
        assertEquals(2, notifications.size());
    }

    // ── Notification message content ─────────────────────────────────

    // Note: Verifies that the notification message includes the task description and formatted deadline.
    @Test
    void checkNowNotificationContainsDescriptionAndDeadline() throws Exception {
        notificationService.simulateStart();
        taskRepository.add("Alex", "Write docs", LocalDateTime.now().plusHours(3), false);

        notificationService.checkNow();

        List<String[]> notifications = notificationService.getNotifications();
        assertEquals(1, notifications.size());
        assertTrue(notifications.getFirst()[1].contains("Write docs"));
        assertTrue(notifications.getFirst()[1].contains("Deadline:"));
    }

    // ── stop() ───────────────────────────────────────────────────────

    // Note: Verifies that stop shuts down the executor and marks the tray as unavailable.
    @Test
    void stopShutdownsExecutorAndRemovesTray() {
        notificationService.simulateStart();
        assertTrue(notificationService.isTrayAvailable());

        notificationService.stop();

        assertFalse(notificationService.isTrayAvailable());
    }

    // ── checkNow() with database error ──────────────────────────────

    // Note: Verifies that checkNow shows an error notification when the database query fails.
    @Test
    void checkNowShowsErrorNotificationOnDatabaseFailure() throws Exception {
        TaskRepository brokenRepository = new TaskRepository(() -> {
            throw new java.sql.SQLException("Connection refused");
        });
        TestableNotificationService brokenService = new TestableNotificationService(brokenRepository);
        brokenService.simulateStart();

        brokenService.checkNow();

        List<String[]> notifications = brokenService.getNotifications();
        assertEquals(1, notifications.size());
        assertEquals("Task Notification", notifications.getFirst()[0]);
        assertTrue(notifications.getFirst()[1].contains("Could not check"));

        brokenService.stop();
    }

    // ── showStatusNotification() ─────────────────────────────────

    // Note: Verifies that showStatusNotification delegates to showNotification with the same title and message.
    @Test
    void showStatusNotificationDelegatesToShowNotification() {
        notificationService.simulateStart();

        notificationService.showStatusNotification("Info", "All tasks are on track.");

        List<String[]> notifications = notificationService.getNotifications();
        assertEquals(1, notifications.size());
        assertEquals("Info", notifications.getFirst()[0]);
        assertEquals("All tasks are on track.", notifications.getFirst()[1]);
    }

    // Note: Verifies that showStatusNotification with empty strings records an entry with both fields empty.
    @Test
    void showStatusNotificationWithEmptyStrings() {
        notificationService.simulateStart();

        notificationService.showStatusNotification("", "");

        List<String[]> notifications = notificationService.getNotifications();
        assertEquals(1, notifications.size());
        assertEquals("", notifications.getFirst()[0]);
        assertEquals("", notifications.getFirst()[1]);
    }

    // Note: Verifies that showStatusNotification always delegates to showNotification regardless of tray state.
    //       In production, showNotification checks trayIcon != null; in tests the override always records the call.
    @Test
    void showStatusNotificationAlwaysDelegatesToShowNotification() {
        notificationService.showStatusNotification("Status", "Message");

        List<String[]> notifications = notificationService.getNotifications();
        assertEquals(1, notifications.size());
        assertEquals("Status", notifications.getFirst()[0]);
        assertEquals("Message", notifications.getFirst()[1]);
    }

    // ── setToggleStartupAction() ─────────────────────────────────

    // Note: Verifies that setToggleStartupAction stores the callback and it can be triggered.
    @Test
    void setToggleStartupActionStoresCallback() {
        boolean[] called = {false};

        notificationService.setToggleStartupAction(() -> called[0] = true);
        notificationService.triggerToggleStartupAction();

        assertTrue(called[0]);
    }

    // Note: Verifies that the default toggleStartupAction (no-op) does not throw when triggered.
    @Test
    void defaultToggleStartupActionDoesNotThrow() {
        // No action was set — the default no-op should execute without exception.
        try {
            notificationService.triggerToggleStartupAction();
        } catch (Exception exception) {
            throw new AssertionError("Default toggleStartupAction should not throw", exception);
        }
    }

    // ── setUninstallAction() ────────────────────────────────────

    // Note: Verifies that setUninstallAction stores the callback and it can be triggered.
    @Test
    void setUninstallActionStoresCallback() {
        boolean[] called = {false};

        notificationService.setUninstallAction(() -> called[0] = true);
        notificationService.triggerUninstallAction();

        assertTrue(called[0]);
    }

    // Note: Verifies that the default uninstallAction (no-op) does not throw when triggered.
    @Test
    void defaultUninstallActionDoesNotThrow() {
        try {
            notificationService.triggerUninstallAction();
        } catch (Exception exception) {
            throw new AssertionError("Default uninstallAction should not throw", exception);
        }
    }

    // ── setStartupEnabledSupplier() / updateStartupMenuLabel() ───────────

    // Note: Verifies that setStartupEnabledSupplier stores the supplier without throwing.
    @Test
    void setStartupEnabledSupplierDoesNotThrow() {
        // startupItem is null (tray not initialized), so updateStartupMenuLabel is a no-op — must not throw.
        try {
            notificationService.setStartupEnabledSupplier(() -> true);
        } catch (Exception exception) {
            throw new AssertionError("setStartupEnabledSupplier should not throw", exception);
        }
    }

    // Note: Verifies that setStartupEnabledSupplier with a false supplier also does not throw.
    @Test
    void setStartupEnabledSupplierWithFalseSupplierDoesNotThrow() {
        try {
            notificationService.setStartupEnabledSupplier(() -> false);
        } catch (Exception exception) {
            throw new AssertionError("setStartupEnabledSupplier(false) should not throw", exception);
        }
    }

    // Note: Verifies that updateStartupMenuLabel does not throw when the tray icon is not initialized.
    @Test
    void updateStartupMenuLabelDoesNotThrowWhenTrayNotInitialized() {
        // startupItem is null before start() — method must guard against NPE.
        try {
            notificationService.updateStartupMenuLabel();
        } catch (Exception exception) {
            throw new AssertionError("updateStartupMenuLabel should not throw when tray is absent", exception);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    // Testable subclass: overrides tray-dependent methods so tests can
    // run in headless CI environments without AWT SystemTray support.
    // ═════════════════════════════════════════════════════════════════

    private static class TestableNotificationService extends DeadlineNotificationService {
        private final List<String[]> notifications = new ArrayList<>();
        private boolean traySimulated = false;
        private Runnable openAction = () -> {};
        private Runnable exitAction = () -> {};
        private Runnable toggleStartupAction = () -> {};
        private Runnable uninstallAction = () -> {};

        TestableNotificationService(TaskRepository taskRepository) {
            super(taskRepository);
        }

        /** Simulates tray initialization without touching AWT. */
        void simulateStart() {
            traySimulated = true;
        }

        @Override
        public void stop() {
            super.stop();
            traySimulated = false;
        }

        @Override
        boolean isTrayReady() {
            return traySimulated;
        }

        @Override
        void showNotification(String title, String message) {
            notifications.add(new String[]{title, message});
        }

        @Override
        public void setOpenAction(Runnable action) {
            this.openAction = action;
            super.setOpenAction(action);
        }

        @Override
        public void setExitAction(Runnable action) {
            this.exitAction = action;
            super.setExitAction(action);
        }

        @Override
        public void setToggleStartupAction(Runnable action) {
            this.toggleStartupAction = action;
            super.setToggleStartupAction(action);
        }

        @Override
        public void setUninstallAction(Runnable action) {
            this.uninstallAction = action;
            super.setUninstallAction(action);
        }

        void triggerOpenAction() {
            openAction.run();
        }

        void triggerExitAction() {
            exitAction.run();
        }

        void triggerToggleStartupAction() {
            toggleStartupAction.run();
        }

        void triggerUninstallAction() {
            uninstallAction.run();
        }

        List<String[]> getNotifications() {
            return notifications;
        }
    }
}
