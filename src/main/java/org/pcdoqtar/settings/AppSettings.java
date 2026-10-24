package org.pcdoqtar.settings;

import java.util.prefs.Preferences;

public final class AppSettings {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(AppSettings.class);

    private static final String THEME_KEY = "theme";
    private static final String WARNING_THRESHOLD_KEY = "warningThreshold";
    private static final String NAV_COLLAPSED_KEY = "navigationCollapsedByDefault";
    private static final double DEFAULT_WARNING_THRESHOLD = 80.0;
    
    private AppSettings() { }
    public static String getTheme() {
        return PREFS.get(THEME_KEY, "Dark");
    }

    public static void setTheme(String theme) {
        if (theme == null
                || (!theme.equalsIgnoreCase("Dark")
                && !theme.equalsIgnoreCase("Light"))) {
            throw new IllegalArgumentException("Theme must be Dark or Light.");
        }
        PREFS.put(THEME_KEY, theme.equalsIgnoreCase("Light") ? "Light" : "Dark");
    }

    public static double getWarningThreshold() {
        double value = PREFS.getDouble(WARNING_THRESHOLD_KEY, DEFAULT_WARNING_THRESHOLD);
        if (!Double.isFinite(value) || value < 0 || value > 100) {
            return DEFAULT_WARNING_THRESHOLD;
        }
        return value;
    }

    public static void setWarningThreshold(double percentage) {
        if (!Double.isFinite(percentage) || percentage < 0.0 || percentage > 100.0) {
            throw new IllegalArgumentException("Warning threshold must be between 0 and 100.");
        }
        PREFS.putDouble(WARNING_THRESHOLD_KEY, percentage);
    }

    /** Whether the navigation drawer should start collapsed on application launch. */
    public static boolean isNavigationCollapsedByDefault() {
        return PREFS.getBoolean(NAV_COLLAPSED_KEY, false);
    }

    public static void setNavigationCollapsedByDefault(boolean collapsed) {
        PREFS.putBoolean(NAV_COLLAPSED_KEY, collapsed);
    }
}
