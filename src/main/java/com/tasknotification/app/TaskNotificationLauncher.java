package com.tasknotification.app;

import com.tasknotification.startup.WindowsStartupRegistration;
import javafx.application.Application;

public class TaskNotificationLauncher {
    public static void main(String[] args) {
        WindowsStartupRegistration.registerPackagedApp();
        Application.launch(TaskNotificationApp.class, args);
    }
}
