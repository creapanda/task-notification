package com.tasknotification.startup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Registers the packaged application to launch when the current user signs in.
 */
public final class WindowsStartupRegistration {
    private static final String RUN_KEY =
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE_NAME = "TaskNotificationApp";

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

        String executable = executablePath.toAbsolutePath().normalize().toString();
        try {
            new ProcessBuilder(
                    "reg.exe",
                    "add",
                    RUN_KEY,
                    "/v",
                    VALUE_NAME,
                    "/t",
                    "REG_SZ",
                    "/d",
                    executable,
                    "/f"
            ).start().waitFor();
        } catch (IOException exception) {
            System.err.println("Could not register the app for Windows startup: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private static Path findPackagedExecutable() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            return Path.of(appPath);
        }

        String command = ProcessHandle.current().info().command().orElse("");
        if (!command.isBlank()) {
            Path commandPath = Path.of(command);
            String fileName = commandPath.getFileName().toString().toLowerCase(Locale.ROOT);
            if (fileName.endsWith(".exe")
                    && !fileName.equals("java.exe")
                    && !fileName.equals("javaw.exe")) {
                return commandPath;
            }
        }

        try {
            Path codeLocation = Path.of(
                    WindowsStartupRegistration.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            );
            Path appDirectory = codeLocation.getParent();
            if (appDirectory != null && appDirectory.getParent() != null) {
                Path packagedLauncher = appDirectory.getParent().resolve("Task Notification.exe");
                if (Files.isRegularFile(packagedLauncher)) {
                    return packagedLauncher;
                }
            }
        } catch (URISyntaxException exception) {
            return null;
        }

        Path javaHome = Path.of(System.getProperty("java.home", ""));
        Path appDirectory = javaHome.getParent();
        if (appDirectory == null) {
            return null;
        }

        Path packagedLauncher = appDirectory.resolve("Task Notification.exe");
        return Files.isRegularFile(packagedLauncher) ? packagedLauncher : null;
    }
}
