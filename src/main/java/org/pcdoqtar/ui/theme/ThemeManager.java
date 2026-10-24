
package org.pcdoqtar.ui.theme;

import java.util.Map;

import org.pcdoqtar.settings.AppSettings;

import javafx.scene.Scene;

public final class ThemeManager {

    private static final Map<String, String> DARK = Map.of(
            "background", "#090B10",
            "panel", "#141821",
            "border", "#2B3240",
            "text", "#F2F5FA",
            "muted", "#A0A9B8",
            "accent", "#B8FF3B",
            "warning", "#FFB020",
            "critical", "#FF5555",
            "grey", "#454D5B"
    );

    private static final Map<String, String> LIGHT = Map.of(
            "background", "#F4F6FA",
            "panel", "#FFFFFF",
            "border", "#D8DEE8",
            "text", "#172033",
            "muted", "#667085",
            "accent", "#527A00",
            "warning", "#A85D00",
            "critical", "#C62828",
            "grey", "#98A2B3"
    );

    private ThemeManager() {
    }

    public static String getCurrentTheme() {
        return AppSettings.getTheme();
    }

    public static void setTheme(String theme) {
        AppSettings.setTheme(theme);
    }

    public static String getColour(String key) {
        Map<String, String> palette =
                "Light".equalsIgnoreCase(getCurrentTheme())
                        ? LIGHT : DARK;

        String value = palette.get(key);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Unknown theme colour: " + key);
        }
        return value;
    }

    public static void apply(Scene scene) {
        if (scene == null) {
            return;
        }

        scene.getRoot().getStyleClass().removeAll(
                "theme-dark", "theme-light");

        scene.getRoot().getStyleClass().add(
                "Light".equalsIgnoreCase(getCurrentTheme())
                        ? "theme-light" : "theme-dark");

        scene.getRoot().setStyle(
                "-fx-background-color: " + getColour("background") + ";");

        String stylesheet = ThemeManager.class.getResource(
                "/org/pcdoqtar/ui/theme/pcdoqtar.css") == null
                ? null
                : ThemeManager.class.getResource(
                        "/org/pcdoqtar/ui/theme/pcdoqtar.css").toExternalForm();

        if (stylesheet != null
                && !scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }
}
