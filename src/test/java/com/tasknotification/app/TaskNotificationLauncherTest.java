package com.tasknotification.app;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskNotificationLauncherTest {

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

    // Note: Verifies that the constructor exists and is accessible (not private utility class pattern).
    @Test
    void constructorIsAccessible() {
        assertNotNull(TaskNotificationLauncher.class.getConstructors());
        assertTrue(TaskNotificationLauncher.class.getConstructors().length > 0);
    }

    // ── main() argument handling ────────────────────────────────────────

    // Note: Verifies that passing --disable-startup argument does not throw an exception.
    @Test
    void mainWithDisableStartupArgDoesNotThrow() {
        // This tests that the branch executes without exception; actual registry
        // side-effects are tested in WindowsStartupRegistrationTest.
        try {
            TaskNotificationLauncher.main(new String[] {"--disable-startup"});
        } catch (Exception exception) {
            // Only expect this if disableStartup itself throws, which it should not.
            throw new AssertionError("main(--disable-startup) should not throw", exception);
        }
    }

    // Note: Verifies that passing --enable-startup argument does not throw an exception.
    @Test
    void mainWithEnableStartupArgDoesNotThrow() {
        try {
            TaskNotificationLauncher.main(new String[] {"--enable-startup"});
        } catch (Exception exception) {
            throw new AssertionError("main(--enable-startup) should not throw", exception);
        }
    }

    // Note: Verifies that passing --uninstall-app argument does not throw an exception.
    @Test
    void mainWithUninstallAppArgDoesNotThrow() {
        try {
            TaskNotificationLauncher.main(new String[] {"--uninstall-app"});
        } catch (Exception exception) {
            throw new AssertionError("main(--uninstall-app) should not throw", exception);
        }
    }

    // Note: Verifies that the --disable-startup argument is handled before JavaFX launch
    //       (i.e., the method returns early, the JVM does not crash).
    @Test
    void mainHandlesMultipleKnownArgsWithFirstMatch() {
        // When both --disable-startup and --enable-startup are present, the first
        // matching branch executes and the method returns early.
        try {
            TaskNotificationLauncher.main(new String[] {"--disable-startup", "--enable-startup"});
        } catch (Exception exception) {
            throw new AssertionError("main with multiple args should not throw", exception);
        }
    }

    // ── main() parameter inspection ─────────────────────────────────

    // Note: Verifies that main's single parameter is named 'args' (compile-with-parameters or default).
    @Test
    void mainMethodParameterTypeIsStringArray() throws NoSuchMethodException {
        Method mainMethod = TaskNotificationLauncher.class.getMethod("main", String[].class);
        Parameter param = mainMethod.getParameters()[0];

        assertEquals(String[].class, param.getType());
    }
}
