package com.tasknotification.app;

import com.tasknotification.startup.WindowsStartupRegistration;
import javafx.application.Application;

import java.util.Arrays;

/**
 * Plain Java entry point used by Maven and the packaged Windows application.
 */
public class TaskNotificationLauncher {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--disable-startup")) {
            WindowsStartupRegistration.disableStartup();
            return;
        }
        if (Arrays.asList(args).contains("--enable-startup")) {
            WindowsStartupRegistration.enableStartup();
            return;
        }
        if (Arrays.asList(args).contains("--uninstall-app")) {
            WindowsStartupRegistration.uninstallPackagedApp();
            return;
        }

        WindowsStartupRegistration.registerPackagedApp();
        Application.launch(TaskNotificationApp.class, args);
    }
}
