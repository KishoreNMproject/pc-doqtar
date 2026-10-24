package org.pcdoqtar.ui.theme;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pcdoqtar.settings.AppSettings;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

public final class ThemeManager {

    private static final String APPLIED_THEME_KEY = ThemeManager.class.getName() + ".appliedTheme";
    private static final Pattern HEX_COLOUR = Pattern.compile("#[0-9a-fA-F]{6}");

    private static final Map<String, String> DARK = Map.of(
            "background", "#090B10", "panel", "#141821", "border", "#2B3240",
            "text", "#F2F5FA", "muted", "#A0A9B8", "accent", "#B8FF3B",
            "warning", "#FFB020", "critical", "#FF5555", "grey", "#454D5B");

    private static final Map<String, String> LIGHT = Map.of(
            "background", "#F4F6FA", "panel", "#FFFFFF", "border", "#D8DEE8",
            "text", "#172033", "muted", "#667085", "accent", "#527A00",
            "warning", "#A85D00", "critical", "#C62828", "grey", "#98A2B3");

    private ThemeManager() { }

    public static String getCurrentTheme() {
        return AppSettings.getTheme();
    }

    public static void setTheme(String theme) {
        AppSettings.setTheme(theme);
    }

    public static String getColour(String key) {
        String value = palette().get(key);
        if (value == null) {
            throw new IllegalArgumentException("Unknown theme colour: " + key);
        }
        return value;
    }

    public static void apply(Scene scene) {
        if (scene == null) return;

        String targetTheme = getCurrentTheme();
        Object previouslyApplied = scene.getProperties().get(APPLIED_THEME_KEY);
        String oldTheme = previouslyApplied instanceof String
                ? (String) previouslyApplied : targetTheme;

        if (!oldTheme.equalsIgnoreCase(targetTheme)) {
            Map<String, String> replacements = colourReplacements(oldTheme, targetTheme);
            retintInlineStyles(scene.getRoot(), replacements);
        }

        scene.getRoot().getStyleClass().removeAll("theme-dark", "theme-light");
        scene.getRoot().getStyleClass().add(
                "Light".equalsIgnoreCase(targetTheme) ? "theme-light" : "theme-dark");
        scene.getRoot().setStyle("-fx-background-color: " + getColour("background") + ";");

        var cssUrl = ThemeManager.class.getResource("/org/pcdoqtar/ui/theme/pcdoqtar.css");
        if (cssUrl != null && !scene.getStylesheets().contains(cssUrl.toExternalForm())) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        scene.getProperties().put(APPLIED_THEME_KEY, targetTheme);
        scene.getRoot().applyCss();
    }

    private static Map<String, String> palette() {
        return "Light".equalsIgnoreCase(getCurrentTheme()) ? LIGHT : DARK;
    }

    private static Map<String, String> colourReplacements(String fromTheme, String toTheme) {
        Map<String, String> from = "Light".equalsIgnoreCase(fromTheme) ? LIGHT : DARK;
        Map<String, String> to = "Light".equalsIgnoreCase(toTheme) ? LIGHT : DARK;
        Map<String, String> replacements = new HashMap<>();
        for (String key : from.keySet()) replacements.put(from.get(key), to.get(key));

        // Legacy hard-coded colours from the original process page/title bar.
        boolean toLight = "Light".equalsIgnoreCase(toTheme);
        replacements.put("#090909", to.get("background"));
        replacements.put("#141414", to.get("panel"));
        replacements.put("#292929", to.get("border"));
        replacements.put("#F5F5F5", to.get("text"));
        replacements.put("#929292", to.get("muted"));
        replacements.put("#101010", toLight ? "#FFFFFF" : "#10141C");
        replacements.put("#666666", to.get("muted"));
        replacements.put("#42B8FF", to.get("accent"));
        replacements.put("#8993A3", to.get("muted"));
        replacements.put("#202633", toLight ? "#E8EDF5" : "#202633");
        replacements.put("#343D4D", to.get("border"));
        replacements.put("#151515", toLight ? "#F4F6FA" : "#151515");
        replacements.put("#171C26", toLight ? "#F0F3F8" : "#171C26");
        replacements.put("#34451F", toLight ? "#DFEBC8" : "#34451F");
        replacements.put("#C62828", to.get("critical"));
        return replacements;
    }

    private static void retintInlineStyles(Node node, Map<String, String> replacements) {
        String style = node.getStyle();
        if (style != null && !style.isBlank()) {
            Matcher matcher = HEX_COLOUR.matcher(style);
            StringBuffer updated = new StringBuffer();
            while (matcher.find()) {
                String replacement = replacements.get(matcher.group().toUpperCase());
                if (replacement == null) replacement = replacements.get(matcher.group());
                matcher.appendReplacement(updated,
                        Matcher.quoteReplacement(replacement == null ? matcher.group() : replacement));
            }
            matcher.appendTail(updated);
            node.setStyle(updated.toString());
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                retintInlineStyles(child, replacements);
            }
        }
    }
}