package com.tasknotification.startup;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Snapshots and restores the real files {@link WindowsStartupRegistration} writes under %APPDATA%.
 *
 * The production methods deliberately touch the current user's real Startup folder, so without this
 * snapshot a test run would leave the developer's machine with app startup silently turned off.
 */
public final class StartupStateBackup {
    private final Path startupScript;
    private final byte[] startupScriptContent;
    private final Path disabledMarker;
    private final byte[] disabledMarkerContent;

    private StartupStateBackup(Path startupScript, byte[] startupScriptContent,
                               Path disabledMarker, byte[] disabledMarkerContent) {
        this.startupScript = startupScript;
        this.startupScriptContent = startupScriptContent;
        this.disabledMarker = disabledMarker;
        this.disabledMarkerContent = disabledMarkerContent;
    }

    public static StartupStateBackup capture() throws IOException {
        Path startupScript = startupScriptPath();
        Path disabledMarker = disabledMarkerPath();

        return new StartupStateBackup(
                startupScript, readIfPresent(startupScript),
                disabledMarker, readIfPresent(disabledMarker)
        );
    }

    public void restore() throws IOException {
        restoreFile(startupScript, startupScriptContent);
        restoreFile(disabledMarker, disabledMarkerContent);
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static Path startupScriptPath() {
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
                constant("STARTUP_SCRIPT_NAME")
        );
    }

    public static Path disabledMarkerPath() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isBlank()) {
            return null;
        }

        return Path.of(appData, "TaskNotification", constant("DISABLED_MARKER_NAME"));
    }

    // Reads the file name from the class under test so a rename there cannot silently leave this
    // backup pointing at the wrong file while tests keep mutating the real one.
    private static String constant(String fieldName) {
        try {
            Field field = WindowsStartupRegistration.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "WindowsStartupRegistration." + fieldName + " is missing", exception);
        }
    }

    private static byte[] readIfPresent(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }

        return Files.readAllBytes(path);
    }

    private static void restoreFile(Path path, byte[] content) throws IOException {
        if (path == null) {
            return;
        }

        if (content == null) {
            Files.deleteIfExists(path);
            return;
        }

        Files.createDirectories(path.getParent());
        Files.write(path, content);
    }
}
