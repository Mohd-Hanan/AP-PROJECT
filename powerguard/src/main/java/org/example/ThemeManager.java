package org.example;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class ThemeManager {

    public static final String DARK_THEME = "Dark Theme";
    public static final String BLUE_THEME = "Blue Analytics Theme";

    private static final List<Consumer<String>> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile String currentTheme = DARK_THEME;

    private ThemeManager() {
    }

    public static String getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDarkTheme() {
        return DARK_THEME.equals(currentTheme);
    }

    public static void toggleTheme() {
        setCurrentTheme(isDarkTheme() ? BLUE_THEME : DARK_THEME);
    }

    public static void setCurrentTheme(String theme) {
        if (theme == null || theme.isBlank()) {
            return;
        }
        currentTheme = theme;
        for (Consumer<String> listener : LISTENERS) {
            listener.accept(theme);
        }
    }

    public static void addListener(Consumer<String> listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Consumer<String> listener) {
        LISTENERS.remove(listener);
    }
}
