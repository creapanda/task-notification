package com.tasknotification.startup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Registers or removes the packaged app from the current user's Windows startup folder.
 */
public final class WindowsStartupRegistration {
    private static final String STARTUP_SCRIPT_NAME = "TaskNotificationApp.cmd";
    private static final String DISABLED_MARKER_NAME = "startup-disabled.txt";
    private static final String UNINSTALL_SCRIPT_NAME = "TaskNotificationUninstall.cmd";

    private WindowsStartupRegistration() {
    }

    public static void registerPackagedApp() {
        if (!isWindows() || isStartupDisabled()) {
            return;
        }

        writeStartupScript();
    }

    public static boolean enableStartup() {
        if (!isWindows()) {
            return false;
        }

        Path disabledMarker = disabledMarkerPath();
        try {
            if (disabledMarker != null) {
                Files.deleteIfExists(disabledMarker);
            }
        } catch (IOException exception) {
            System.err.println("Could not save the startup preference: " + exception.getMessage());
            return false;
        }

        return writeStartupScript();
    }

    public static boolean isStartupEnabled() {
        return isWindows() && !isStartupDisabled() && findPackagedExecutable() != null;
    }

    private static boolean writeStartupScript() {
        Path executablePath = findPackagedExecutable();
        if (executablePath == null) {
            return false;
        }

        Path startupScript = startupScriptPath();
        if (startupScript == null) {
            return false;
        }

        String executable = executablePath.toAbsolutePath().normalize().toString();
        String script = "@echo off\r\nstart \"\" \"" + executable + "\" --background\r\n";

        try {
            Files.createDirectories(startupScript.getParent());
            Files.writeString(startupScript, script, StandardCharsets.UTF_8);
            return true;
        } catch (IOException exception) {
            System.err.println("Could not register the app for Windows startup: " + exception.getMessage());
            return false;
        }
    }

    public static boolean disableStartup() {
        if (!isWindows()) {
            return false;
        }

        boolean disabled = true;
        Path startupScript = startupScriptPath();
        Path disabledMarker = disabledMarkerPath();

        try {
            if (startupScript != null) {
                Files.deleteIfExists(startupScript);
            }
        } catch (IOException exception) {
            disabled = false;
            System.err.println("Could not remove the Windows startup script: " + exception.getMessage());
        }

        try {
            if (disabledMarker != null) {
                Files.createDirectories(disabledMarker.getParent());
                Files.writeString(disabledMarker, "disabled", StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            disabled = false;
            System.err.println("Could not save the startup preference: " + exception.getMessage());
        }

        return disabled;
    }

    public static boolean uninstallPackagedApp() {
        if (!isWindows()) {
            return false;
        }

        disableStartup();

        Path appDirectory = findPackagedAppDirectory();
        if (appDirectory == null) {
            return false;
        }

        return scheduleDirectoryDeletion(appDirectory);
    }

    private static boolean isStartupDisabled() {
        Path disabledMarker = disabledMarkerPath();
        return disabledMarker != null && Files.isRegularFile(disabledMarker);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private static Path findPackagedExecutable() {
        Path appDirectory = findPackagedAppDirectory();
        if (appDirectory == null) {
            return null;
        }

        Path packagedLauncher = appDirectory.resolve("Task Notification.exe");
        return Files.isRegularFile(packagedLauncher) ? packagedLauncher : null;
    }

    private static Path findPackagedAppDirectory() {
        Path javaHome = Path.of(System.getProperty("java.home", ""));
        Path appDirectory = javaHome.getParent();
        if (appDirectory == null) {
            return null;
        }

        Path packagedLauncher = appDirectory.resolve("Task Notification.exe");
        return Files.isRegularFile(packagedLauncher) ? appDirectory : null;
    }

    private static boolean scheduleDirectoryDeletion(Path directory) {
        Path uninstallScript = Path.of(System.getProperty("java.io.tmpdir", ".")).resolve(UNINSTALL_SCRIPT_NAME);
        String script = """
                @echo off
                timeout /t 2 /nobreak >nul
                rmdir /s /q "%s"
                del "%%~f0"
                """.formatted(directory.toAbsolutePath().normalize());

        try {
            Files.writeString(uninstallScript, script, StandardCharsets.UTF_8);
            new ProcessBuilder("cmd.exe", "/c", "start \"\" \"" + uninstallScript.toAbsolutePath() + "\"").start();
            return true;
        } catch (IOException exception) {
            System.err.println("Could not schedule app folder deletion: " + exception.getMessage());
            return false;
        }
    }

    private static Path startupScriptPath() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            return null;
        }

        return Path.of(
                appData,
                "Microsoft",
                "Windows",
                "Start Menu",
                "Programs",
                "Startup",
                STARTUP_SCRIPT_NAME
        );
    }

    private static Path disabledMarkerPath() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            return null;
        }

        return Path.of(appData, "TaskNotification", DISABLED_MARKER_NAME);
    }
}
