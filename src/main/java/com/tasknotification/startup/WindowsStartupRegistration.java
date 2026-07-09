package com.tasknotification.startup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Registers the packaged application to launch when the current user signs in.
 */
public final class WindowsStartupRegistration {
    private WindowsStartupRegistration() {
    }

    public static void registerPackagedApp() {
        if (!isWindows()) {
            return;
        }

        Path executablePath = findPackagedExecutable();
        if (executablePath == null) {
            return;
        }

        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            return;
        }

        Path startupDirectory = Path.of(
                appData,
                "Microsoft",
                "Windows",
                "Start Menu",
                "Programs",
                "Startup"
        );
        Path startupScript = startupDirectory.resolve("TaskNotificationApp.cmd");
        String executable = executablePath.toAbsolutePath().normalize().toString();
        String script = "@echo off\r\nstart \"\" \"" + executable + "\" --background\r\n";

        try {
            Files.createDirectories(startupDirectory);
            Files.writeString(startupScript, script, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.err.println("Could not register the app for Windows startup: " + exception.getMessage());
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private static Path findPackagedExecutable() {
        Path javaHome = Path.of(System.getProperty("java.home", ""));
        Path appDirectory = javaHome.getParent();
        if (appDirectory == null) {
            return null;
        }

        Path packagedLauncher = appDirectory.resolve("Task Notification.exe");
        return Files.isRegularFile(packagedLauncher) ? packagedLauncher : null;
    }
}
