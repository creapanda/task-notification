package com.tasknotification.startup;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsStartupRegistrationTest {

    // These methods write to the current user's real %APPDATA% folders, so every test runs against
    // a snapshot that is put back afterwards.
    private StartupStateBackup startupStateBackup;

    @BeforeEach
    void captureRealStartupState() throws Exception {
        startupStateBackup = StartupStateBackup.capture();
    }

    @AfterEach
    void restoreRealStartupState() throws Exception {
        startupStateBackup.restore();
    }

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

    // Note: Verifies that registerPackagedApp leaves the startup script untouched when no packaged
    //       executable exists next to java.home (development environment).
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void registerPackagedAppDoesNotCreateStartupScriptInDevelopmentEnvironment() throws Exception {
        Path startupScript = StartupStateBackup.startupScriptPath();
        boolean existedBefore = Files.isRegularFile(startupScript);

        WindowsStartupRegistration.registerPackagedApp();

        assertEquals(existedBefore, Files.isRegularFile(startupScript),
                "No packaged executable exists, so the startup script must not be created");
    }

    // Note: Verifies that in a development environment the packaged executable does not exist next to java.home.
    //       This is the precondition the other development-environment tests rely on.
    @Test
    void packagedExecutableDoesNotExistInDevelopmentEnvironment() {
        Path javaHome = Path.of(System.getProperty("java.home", ""));
        Path appDirectory = javaHome.getParent();

        if (appDirectory != null) {
            Path packagedLauncher = appDirectory.resolve("Task Notification.exe");
            assertFalse(Files.isRegularFile(packagedLauncher),
                    "Packaged executable should not exist in development environment");
        }
    }

    // ── enableStartup() ──────────────────────────────────────────────

    // Note: Verifies that enableStartup does not throw in development environment (no packaged exe).
    @Test
    void enableStartupDoesNotThrowInDevelopmentEnvironment() {
        assertDoesNotThrow(WindowsStartupRegistration::enableStartup);
    }

    // Note: Verifies that enableStartup returns false when not running from a packaged app directory.
    @Test
    void enableStartupReturnsFalseInDevelopmentEnvironment() {
        // In dev there is no packaged executable, so writeStartupScript returns false.
        boolean result = WindowsStartupRegistration.enableStartup();

        // On non-Windows OR Windows without the packaged exe both return false.
        assertFalse(result, "enableStartup should return false in development environment");
    }

    // Note: Verifies that calling enableStartup twice does not throw.
    @Test
    void enableStartupCanBeCalledMultipleTimes() {
        assertDoesNotThrow(() -> {
            WindowsStartupRegistration.enableStartup();
            WindowsStartupRegistration.enableStartup();
        });
    }

    // Note: Verifies that enableStartup removes the disabled marker so the app is allowed to
    //       register itself again on the next start.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void enableStartupRemovesDisabledMarker() throws Exception {
        WindowsStartupRegistration.disableStartup();
        Path disabledMarker = StartupStateBackup.disabledMarkerPath();
        assertTrue(Files.isRegularFile(disabledMarker), "disableStartup should have created the marker");

        WindowsStartupRegistration.enableStartup();

        assertFalse(Files.isRegularFile(disabledMarker),
                "enableStartup should delete the disabled marker");
    }

    // Note: Verifies that enableStartup does not write a startup script when no packaged executable exists.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void enableStartupDoesNotCreateStartupScriptWithoutPackagedExecutable() throws Exception {
        Path startupScript = StartupStateBackup.startupScriptPath();
        Files.deleteIfExists(startupScript);

        WindowsStartupRegistration.enableStartup();

        assertFalse(Files.isRegularFile(startupScript),
                "Without a packaged executable there is nothing to point the startup script at");
    }

    // ── disableStartup() ─────────────────────────────────────────────

    // Note: Verifies that disableStartup does not throw in development environment.
    @Test
    void disableStartupDoesNotThrowInDevelopmentEnvironment() {
        assertDoesNotThrow(WindowsStartupRegistration::disableStartup);
    }

    // Note: Verifies that disableStartup writes the disabled marker file and reports success on Windows.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void disableStartupCreatesDisabledMarker() throws Exception {
        Path disabledMarker = StartupStateBackup.disabledMarkerPath();
        Files.deleteIfExists(disabledMarker);

        boolean result = WindowsStartupRegistration.disableStartup();

        assertTrue(result, "disableStartup should succeed on Windows");
        assertTrue(Files.isRegularFile(disabledMarker), "disableStartup should create the marker file");
        assertEquals("disabled", Files.readString(disabledMarker, StandardCharsets.UTF_8));
    }

    // Note: Verifies that disableStartup deletes an existing startup script from the Startup folder.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void disableStartupRemovesExistingStartupScript() throws Exception {
        Path startupScript = StartupStateBackup.startupScriptPath();
        Files.createDirectories(startupScript.getParent());
        Files.writeString(startupScript, "@echo off\r\nrem placeholder written by tests\r\n",
                StandardCharsets.UTF_8);

        WindowsStartupRegistration.disableStartup();

        assertFalse(Files.isRegularFile(startupScript),
                "disableStartup should delete the startup script");
    }

    // Note: Verifies that calling disableStartup twice does not throw.
    @Test
    void disableStartupCanBeCalledMultipleTimes() {
        assertDoesNotThrow(() -> {
            WindowsStartupRegistration.disableStartup();
            WindowsStartupRegistration.disableStartup();
        });
    }

    // ── isStartupEnabled() ────────────────────────────────────────────

    // Note: Verifies that isStartupEnabled does not throw in development environment.
    @Test
    void isStartupEnabledDoesNotThrowInDevelopmentEnvironment() {
        assertDoesNotThrow(WindowsStartupRegistration::isStartupEnabled);
    }

    // Note: Verifies that isStartupEnabled returns false in development environment
    //       (no packaged executable next to java.home).
    @Test
    void isStartupEnabledReturnsFalseInDevelopmentEnvironment() {
        boolean result = WindowsStartupRegistration.isStartupEnabled();

        assertFalse(result, "isStartupEnabled should be false in development environment");
    }

    // Note: Verifies the relationship: disabling startup then calling isStartupEnabled returns false.
    @Test
    void isStartupEnabledReturnsFalseAfterDisableStartup() {
        WindowsStartupRegistration.disableStartup();

        boolean result = WindowsStartupRegistration.isStartupEnabled();

        assertFalse(result);
    }

    // Note: Verifies that the disabled marker alone is enough to report startup as not enabled.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void isStartupEnabledReturnsFalseWhenDisabledMarkerExists() throws Exception {
        Path disabledMarker = StartupStateBackup.disabledMarkerPath();
        Files.createDirectories(disabledMarker.getParent());
        Files.writeString(disabledMarker, "disabled", StandardCharsets.UTF_8);

        assertFalse(WindowsStartupRegistration.isStartupEnabled(),
                "The disabled marker must short-circuit isStartupEnabled");
    }

    // ── uninstallPackagedApp() ────────────────────────────────────────

    // Note: Verifies that uninstallPackagedApp reports failure when the app is not running from a
    //       packaged directory, so no folder deletion is ever scheduled in development.
    @Test
    void uninstallPackagedAppReturnsFalseWithoutPackagedApp() {
        boolean result = WindowsStartupRegistration.uninstallPackagedApp();

        assertFalse(result,
                "Without a packaged app directory there is nothing to uninstall");
    }

    // Note: Verifies that uninstallPackagedApp disables startup before it gives up, so an app that
    //       cannot delete itself at least stops launching on boot.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void uninstallPackagedAppDisablesStartupFirst() throws Exception {
        Path disabledMarker = StartupStateBackup.disabledMarkerPath();
        Files.deleteIfExists(disabledMarker);

        WindowsStartupRegistration.uninstallPackagedApp();

        assertTrue(Files.isRegularFile(disabledMarker),
                "uninstallPackagedApp should call disableStartup before returning");
    }

    // Note: Verifies that uninstallPackagedApp does not throw when called twice.
    @Test
    void uninstallPackagedAppCanBeCalledMultipleTimes() {
        assertDoesNotThrow(() -> {
            WindowsStartupRegistration.uninstallPackagedApp();
            WindowsStartupRegistration.uninstallPackagedApp();
        });
    }

    // ── Constant name verification (via reflection) ───────────────────

    // Note: Verifies that the STARTUP_SCRIPT_NAME constant has the expected value.
    @Test
    void startupScriptNameConstantHasExpectedValue() throws Exception {
        java.lang.reflect.Field field = WindowsStartupRegistration.class
                .getDeclaredField("STARTUP_SCRIPT_NAME");
        field.setAccessible(true);

        assertEquals("TaskNotificationApp.cmd", field.get(null));
    }

    // Note: Verifies that the DISABLED_MARKER_NAME constant has the expected value.
    @Test
    void disabledMarkerNameConstantHasExpectedValue() throws Exception {
        java.lang.reflect.Field field = WindowsStartupRegistration.class
                .getDeclaredField("DISABLED_MARKER_NAME");
        field.setAccessible(true);

        assertEquals("startup-disabled.txt", field.get(null));
    }

    // Note: Verifies that the UNINSTALL_SCRIPT_NAME constant has the expected value.
    @Test
    void uninstallScriptNameConstantHasExpectedValue() throws Exception {
        java.lang.reflect.Field field = WindowsStartupRegistration.class
                .getDeclaredField("UNINSTALL_SCRIPT_NAME");
        field.setAccessible(true);

        assertEquals("TaskNotificationUninstall.cmd", field.get(null));
    }
}
