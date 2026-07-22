package com.tasknotification.app;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

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
}
