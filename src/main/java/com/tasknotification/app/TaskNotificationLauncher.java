package com.tasknotification.app;

import com.tasknotification.startup.WindowsStartupRegistration;
import javafx.application.Application;

/**
 * Plain Java entry point used by Maven and the packaged Windows application.
 */
public class TaskNotificationLauncher {
    public static void main(String[] args) {
        WindowsStartupRegistration.registerPackagedApp();
        Application.launch(TaskNotificationApp.class, args);
    }
}
