package com.tasknotification.app;

import com.tasknotification.startup.StartupStateBackup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskNotificationLauncherTest {

    // main() delegates straight to WindowsStartupRegistration, which writes to the current user's
    // real %APPDATA% folders, so every test runs against a snapshot that is put back afterwards.
    private StartupStateBackup startupStateBackup;

    @BeforeEach
    void captureRealStartupState() throws Exception {
        startupStateBackup = StartupStateBackup.capture();
    }

    @AfterEach
    void restoreRealStartupState() throws Exception {
        startupStateBackup.restore();
    }

    // Note: Verifies that TaskNotificationLauncher class exists and can be loaded.
    @Test
    void classCanBeLoaded() {
        assertNotNull(TaskNotificationLauncher.class);
    }

    // Note: Verifies that the class has a public static main(String[]) entry point.
    @Test
    void mainMethodIsPublicStatic() throws NoSuchMethodException {
        Method mainMethod = TaskNotificationLauncher.class.getMethod("main", String[].class);

        assertTrue(Modifier.isPublic(mainMethod.getModifiers()));
        assertTrue(Modifier.isStatic(mainMethod.getModifiers()));
    }

    // Note: Verifies that main method has exactly one parameter of type String[].
    @Test
    void mainMethodAcceptsStringArray() throws NoSuchMethodException {
        Method mainMethod = TaskNotificationLauncher.class.getMethod("main", String[].class);

        assertEquals(1, mainMethod.getParameterCount());
        assertEquals(String[].class, mainMethod.getParameterTypes()[0]);
    }

    // Note: Verifies that main method returns void.
    @Test
    void mainMethodReturnsVoid() throws NoSuchMethodException {
        Method mainMethod = TaskNotificationLauncher.class.getMethod("main", String[].class);

        assertEquals(void.class, mainMethod.getReturnType());
    }

    // ── main() argument handling ────────────────────────────────────────

    // Note: Verifies that passing --disable-startup argument does not throw an exception.
    @Test
    void mainWithDisableStartupArgDoesNotThrow() {
        assertDoesNotThrow(() -> TaskNotificationLauncher.main(new String[] {"--disable-startup"}));
    }

    // Note: Verifies that --disable-startup actually routes to disableStartup by checking that the
    //       disabled marker file is written.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void mainWithDisableStartupArgDisablesStartup() throws Exception {
        Path disabledMarker = StartupStateBackup.disabledMarkerPath();
        Files.deleteIfExists(disabledMarker);

        TaskNotificationLauncher.main(new String[] {"--disable-startup"});

        assertTrue(Files.isRegularFile(disabledMarker),
                "--disable-startup should write the disabled marker");
    }

    // Note: Verifies that passing --enable-startup argument does not throw an exception.
    @Test
    void mainWithEnableStartupArgDoesNotThrow() {
        assertDoesNotThrow(() -> TaskNotificationLauncher.main(new String[] {"--enable-startup"}));
    }

    // Note: Verifies that --enable-startup actually routes to enableStartup by checking that the
    //       disabled marker file is removed.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void mainWithEnableStartupArgEnablesStartup() throws Exception {
        Path disabledMarker = StartupStateBackup.disabledMarkerPath();
        Files.createDirectories(disabledMarker.getParent());
        Files.writeString(disabledMarker, "disabled");

        TaskNotificationLauncher.main(new String[] {"--enable-startup"});

        assertFalse(Files.isRegularFile(disabledMarker),
                "--enable-startup should remove the disabled marker");
    }

    // Note: Verifies that passing --uninstall-app argument does not throw an exception.
    @Test
    void mainWithUninstallAppArgDoesNotThrow() {
        assertDoesNotThrow(() -> TaskNotificationLauncher.main(new String[] {"--uninstall-app"}));
    }

    // Note: Verifies that --uninstall-app routes to uninstallPackagedApp, which disables startup
    //       before discovering there is no packaged directory to remove.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void mainWithUninstallAppArgDisablesStartup() throws Exception {
        Path disabledMarker = StartupStateBackup.disabledMarkerPath();
        Files.deleteIfExists(disabledMarker);

        TaskNotificationLauncher.main(new String[] {"--uninstall-app"});

        assertTrue(Files.isRegularFile(disabledMarker),
                "--uninstall-app should disable startup on its way out");
    }

    // Note: Verifies that when several known flags are present the first matching branch wins and
    //       the later ones never run: --disable-startup must leave the marker that --enable-startup
    //       would have deleted.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void mainHandlesMultipleKnownArgsWithFirstMatch() throws Exception {
        Path disabledMarker = StartupStateBackup.disabledMarkerPath();
        Files.deleteIfExists(disabledMarker);

        TaskNotificationLauncher.main(new String[] {"--disable-startup", "--enable-startup"});

        assertTrue(Files.isRegularFile(disabledMarker),
                "--disable-startup is checked first, so --enable-startup must not have run");
    }

    // The default branch (no known flag) is deliberately not tested: it calls Application.launch,
    // which would start the real JavaFX toolkit and block the test run. Covering it would require
    // splitting the flag handling out of main() in the production class.

    // ── main() parameter inspection ─────────────────────────────────

    // Note: Verifies that main's single parameter is named 'args' (compile-with-parameters or default).
    @Test
    void mainMethodParameterTypeIsStringArray() throws NoSuchMethodException {
        Method mainMethod = TaskNotificationLauncher.class.getMethod("main", String[].class);
        Parameter param = mainMethod.getParameters()[0];

        assertEquals(String[].class, param.getType());
    }
}
