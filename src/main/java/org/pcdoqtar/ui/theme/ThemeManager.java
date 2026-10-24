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
            "background", "#EEF2F7", "panel", "#E2E8F0", "border", "#CBD5E1",
            "text", "#243247", "muted", "#58677B", "accent", "#647D35",
            "warning", "#925400", "critical", "#B42318", "grey", "#94A3B8");

    private ThemeManager() { }

    public static String getCurrentTheme() { return AppSettings.getTheme(); }

    public static void setTheme(String theme) { AppSettings.setTheme(theme); }

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
        Object previous = scene.getProperties().get(APPLIED_THEME_KEY);
        String oldTheme = previous instanceof String ? (String) previous : targetTheme;

        if (!oldTheme.equalsIgnoreCase(targetTheme)) {
            retintInlineStyles(scene.getRoot(), colourReplacements(oldTheme, targetTheme));
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
        scene.getRoot().requestLayout();
    }

    private static Map<String, String> palette() {
        return "Light".equalsIgnoreCase(getCurrentTheme()) ? LIGHT : DARK;
    }

    private static Map<String, String> colourReplacements(String fromTheme, String toTheme) {
        Map<String, String> from = "Light".equalsIgnoreCase(fromTheme) ? LIGHT : DARK;
        Map<String, String> to = "Light".equalsIgnoreCase(toTheme) ? LIGHT : DARK;
        Map<String, String> result = new HashMap<>();
        for (String key : from.keySet()) result.put(from.get(key).toUpperCase(), to.get(key));

        boolean light = "Light".equalsIgnoreCase(toTheme);
        result.put("#090909", to.get("background"));
        result.put("#141414", to.get("panel"));
        result.put("#292929", to.get("border"));
        result.put("#F5F5F5", to.get("text"));
        result.put("#929292", to.get("muted"));
        result.put("#101010", light ? "#F1F5F9" : "#10141C");
        result.put("#666666", to.get("muted"));
        result.put("#42B8FF", to.get("accent"));
        result.put("#8993A3", to.get("muted"));
        result.put("#202633", light ? "#E2E8F0" : "#202633");
        result.put("#343D4D", to.get("border"));
        result.put("#151515", light ? "#EEF2F7" : "#151515");
        result.put("#171C26", light ? "#E8EEF5" : "#171C26");
        result.put("#34451F", light ? "#DDE8C7" : "#34451F");
        return result;
    }

    private static void retintInlineStyles(Node node, Map<String, String> replacements) {
        String style = node.getStyle();
        if (style != null && !style.isBlank()) {
            Matcher matcher = HEX_COLOUR.matcher(style);
            StringBuffer updated = new StringBuffer();
            while (matcher.find()) {
                String replacement = replacements.get(matcher.group().toUpperCase());
                matcher.appendReplacement(updated, Matcher.quoteReplacement(
                        replacement == null ? matcher.group() : replacement));
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