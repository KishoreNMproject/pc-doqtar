
package org.pcdoqtar.settings;

import java.util.prefs.Preferences;

public final class AppSettings {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(AppSettings.class);

    private static final String THEME_KEY = "theme";
    private static final String WARNING_THRESHOLD_KEY =
            "warningThreshold";

    private static final String DEFAULT_THEME = "Dark";
    private static final double DEFAULT_WARNING_THRESHOLD = 80.0;

    private AppSettings() {
        // Utility class; do not instantiate.
    }

    public static String getTheme() {
        String theme = PREFS.get(THEME_KEY, DEFAULT_THEME);

        return "Light".equalsIgnoreCase(theme) ? "Light" : "Dark";
    }

    public static void setTheme(String theme) {
        if (theme == null
                || (!theme.equalsIgnoreCase("Dark")
                && !theme.equalsIgnoreCase("Light"))) {
            throw new IllegalArgumentException(
                    "Theme must be Dark or Light."
            );
        }

        PREFS.put(THEME_KEY,
                theme.equalsIgnoreCase("Light") ? "Light" : "Dark");
    }

    public static double getWarningThreshold() {
        double threshold = PREFS.getDouble(
                "warningThreshold",
                DEFAULT_WARNING_THRESHOLD);

        return Math.max(10.0, Math.min(89.0, threshold));
    }

    public static void setWarningThreshold(double percentage) {
        if (!Double.isFinite(percentage)
                || percentage < 10.0
                || percentage > 89.0) {
            throw new IllegalArgumentException(
                    "Warning threshold must be between 10 and 89."
            );
        }

        PREFS.putDouble(
                WARNING_THRESHOLD_KEY,
                percentage
        );
    }
}
