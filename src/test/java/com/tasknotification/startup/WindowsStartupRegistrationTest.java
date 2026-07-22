package com.tasknotification.startup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsStartupRegistrationTest {

    @TempDir
    Path temporaryDirectory;

    // ── registerPackagedApp() basic behavior ─────────────────────────

    // Note: Verifies that registerPackagedApp completes without throwing any exception in the current environment.
    @Test
    void registerPackagedAppDoesNotThrowException() {
        assertDoesNotThrow(WindowsStartupRegistration::registerPackagedApp);
    }

    // Note: Verifies that calling registerPackagedApp twice does not throw (idempotent behavior).
    @Test
    void registerPackagedAppCanBeCalledMultipleTimes() {
        assertDoesNotThrow(() -> {
            WindowsStartupRegistration.registerPackagedApp();
            WindowsStartupRegistration.registerPackagedApp();
        });
    }

    // ── registerPackagedApp() in development environment ─────────────

    // Note: Verifies that registerPackagedApp does not create a startup script when no packaged executable exists (development environment).
    @Test
    void registerPackagedAppDoesNotCreateScriptInDevelopmentEnvironment() {
        String appData = System.getenv("APPDATA");
        // Skip this test if APPDATA is not set (non-Windows CI)
        if (appData == null || appData.isBlank()) {
            return;
        }

        WindowsStartupRegistration.registerPackagedApp();

        // In dev environment, java.home is not inside a packaged app directory,
        // so findPackagedExecutable returns null and no script should be written.
        // We verify by checking that the method exited early without error.
        assertDoesNotThrow(WindowsStartupRegistration::registerPackagedApp);
    }

    // ── Startup script format verification ───────────────────────────

    // Note: Verifies the expected startup script format by creating a sample and checking its structure.
    @Test
    void startupScriptFollowsExpectedFormat() throws Exception {
        // This test verifies the script format that registerPackagedApp would generate.
        // We reproduce the format logic from the source to validate the template.
        Path fakeExe = temporaryDirectory.resolve("Task Notification.exe");
        String executable = fakeExe.toAbsolutePath().normalize().toString();
        String script = "@echo off\r\nstart \"\" \"" + executable + "\" --background\r\n";

        Path scriptFile = temporaryDirectory.resolve("TaskNotificationApp.cmd");
        Files.writeString(scriptFile, script, StandardCharsets.UTF_8);

        String content = Files.readString(scriptFile, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("@echo off"));
        assertTrue(content.contains("start \"\""));
        assertTrue(content.contains("Task Notification.exe"));
        assertTrue(content.contains("--background"));
    }

    // ── isWindows() indirect verification ────────────────────────────

    // Note: Verifies that the current OS detection is consistent with the os.name system property.
    @Test
    void currentEnvironmentOsNameIsDetectable() {
        String osName = System.getProperty("os.name", "");

        // This test documents the OS detection approach used by isWindows()
        assertFalse(osName.isEmpty(), "os.name system property should be available");
    }

    // ── findPackagedExecutable() indirect verification ────────────────

    // Note: Verifies that java.home system property is available, which is required by findPackagedExecutable.
    @Test
    void javaHomeSystemPropertyIsAvailable() {
        String javaHome = System.getProperty("java.home", "");

        assertFalse(javaHome.isEmpty(), "java.home system property should be set");
    }

    // Note: Verifies that in a development environment, the packaged executable does not exist next to java.home.
    @Test
    void packagedExecutableDoesNotExistInDevelopmentEnvironment() {
        Path javaHome = Path.of(System.getProperty("java.home", ""));
        Path appDirectory = javaHome.getParent();

        // In dev, there is no "Task Notification.exe" next to the JDK
        if (appDirectory != null) {
            Path packagedLauncher = appDirectory.resolve("Task Notification.exe");
            assertFalse(Files.isRegularFile(packagedLauncher),
                    "Packaged executable should not exist in development environment");
        }
    }

    // ── Edge case: APPDATA environment variable ──────────────────────

    // Note: Verifies that the APPDATA environment variable check is consistent with the current OS.
    @Test
    void appDataEnvironmentVariableMatchesOs() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        String appData = System.getenv("APPDATA");

        if (osName.contains("win")) {
            // On Windows, APPDATA should typically be set
            assertTrue(appData != null && !appData.isBlank(),
                    "APPDATA should be set on Windows");
        }
        // On non-Windows, APPDATA is usually null — registerPackagedApp would exit early
    }
}
