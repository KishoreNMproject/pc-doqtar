
package org.pcdoqtar.ui.theme;

import java.util.Map;

import org.pcdoqtar.settings.AppSettings;

public final class ThemeManager {

    private static final String DARK = "Dark";
    private static final String LIGHT = "Light";

    private static final Map<String, String> DARK_COLOURS = Map.of(
            "background", "#090909",
            "panel", "#141414",
            "border", "#292929",
            "text", "#F5F5F5",
            "muted", "#929292",
            "accent", "#B8FF3B",
            "warning", "#FFB020",
            "critical", "#FF5555",
            "grey", "#454545"
    );

    private static final Map<String, String> LIGHT_COLOURS = Map.of(
            "background", "#F5F7FA",
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
        // Utility class; do not instantiate.
    }

    public static String getCurrentTheme() {
        return AppSettings.getTheme();
    }

    public static void setTheme(String theme) {
        AppSettings.setTheme(theme);
    }

    public static String getColour(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Colour key must not be empty."
            );
        }

        Map<String, String> colours =
                LIGHT.equalsIgnoreCase(getCurrentTheme())
                        ? LIGHT_COLOURS
                        : DARK_COLOURS;

        String colour = colours.get(key);

        if (colour == null) {
            throw new IllegalArgumentException(
                    "Unknown theme colour: " + key
            );
        }

        return colour;
    }
}
