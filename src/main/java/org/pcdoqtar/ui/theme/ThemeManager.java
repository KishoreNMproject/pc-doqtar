package org.pcdoqtar.ui.theme;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pcdoqtar.settings.AppSettings;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

public final class ThemeManager {

    private static final Pattern HEX_COLOUR = Pattern.compile("#[0-9a-fA-F]{6}");

    private static final Map<String, String> DARK = Map.of(
            "background", "#090B10",
            "panel", "#141821",
            "border", "#2B3240",
            "text", "#F2F5FA",
            "muted", "#A0A9B8",
            "accent", "#B8FF3B",
            "warning", "#FFB020",
            "critical", "#FF5555",
            "grey", "#454D5B");

    private static final Map<String, String> LIGHT = Map.of(
            "background", "#EEF2F7",
            "panel", "#FFFFFF",
            "border", "#CBD5E1",
            "text", "#243247",
            "muted", "#58677B",
            "accent", "#647D35",
            "warning", "#925400",
            "critical", "#B42318",
            "grey", "#94A3B8");

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

    /**
     * Applies exactly one theme stylesheet and updates the scene's theme class.
     * Legacy inline styles are retinted as a compatibility layer while pages
     * are progressively moved to shared CSS classes.
     */
    public static void apply(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }

        boolean light = "Light".equalsIgnoreCase(getCurrentTheme());
        String activeClass = light ? "theme-light" : "theme-dark";
        String activePath = light
                ? "/org/pcdoqtar/ui/theme/theme-light.css"
                : "/org/pcdoqtar/ui/theme/theme-dark.css";
        String inactivePath = light
                ? "/org/pcdoqtar/ui/theme/theme-dark.css"
                : "/org/pcdoqtar/ui/theme/theme-light.css";

        String activeUrl = stylesheetUrl(activePath);
        String inactiveUrl = stylesheetUrl(inactivePath);

        if (inactiveUrl != null) {
            scene.getStylesheets().remove(inactiveUrl);
        }
        if (activeUrl != null && !scene.getStylesheets().contains(activeUrl)) {
            scene.getStylesheets().add(activeUrl);
        }

        scene.getRoot().getStyleClass().removeAll("theme-dark", "theme-light");
        scene.getRoot().getStyleClass().add(activeClass);
        scene.getRoot().setStyle("-fx-background-color: " + getColour("background") + ";");

        applyToNode(scene.getRoot());
        scene.getRoot().applyCss();
        scene.getRoot().requestLayout();
    }

    /**
     * Retints a page when it is attached to the scene. Pages are created up
     * front and may be hidden when Settings changes the theme, so styling only
     * the currently visible scene tree is not sufficient.
     */
    public static void applyToNode(Node node) {
        if (node == null) {
            return;
        }
        retintInlineStyles(node, colourReplacements(getCurrentTheme()));
        if (node instanceof Parent parent) {
            parent.applyCss();
            parent.requestLayout();
        }
    }

    private static String stylesheetUrl(String path) {
        var resource = ThemeManager.class.getResource(path);
        return resource == null ? null : resource.toExternalForm();
    }

    private static Map<String, String> palette() {
        return "Light".equalsIgnoreCase(getCurrentTheme()) ? LIGHT : DARK;
    }

    /**
     * Maps colours from either palette and the previous hard-coded dark styles
     * to the currently selected palette. Applying it repeatedly is safe.
     */
    private static Map<String, String> colourReplacements(String targetTheme) {
        Map<String, String> target = "Light".equalsIgnoreCase(targetTheme) ? LIGHT : DARK;
        Map<String, String> replacements = new HashMap<>();

        for (String key : DARK.keySet()) {
            replacements.put(DARK.get(key).toUpperCase(Locale.ROOT), target.get(key));
            replacements.put(LIGHT.get(key).toUpperCase(Locale.ROOT), target.get(key));
        }

        boolean light = "Light".equalsIgnoreCase(targetTheme);
        replacements.put("#090909", target.get("background"));
        replacements.put("#0D0D0D", target.get("background"));
        replacements.put("#111111", target.get("panel"));
        replacements.put("#101010", light ? "#FFFFFF" : "#10141C");
        replacements.put("#141414", target.get("panel"));
        replacements.put("#151515", light ? "#EEF2F7" : "#151515");
        replacements.put("#171717", light ? "#F4F6FA" : "#171717");
        replacements.put("#202020", light ? "#E2E8F0" : "#202633");
        replacements.put("#292929", target.get("border"));
        replacements.put("#383838", target.get("border"));
        replacements.put("#444444", target.get("border"));
        replacements.put("#454545", target.get("grey"));
        replacements.put("#666666", target.get("muted"));
        replacements.put("#929292", target.get("muted"));
        replacements.put("#8993A3", target.get("muted"));
        replacements.put("#B0B0B0", target.get("muted"));
        replacements.put("#F5F5F5", target.get("text"));
        replacements.put("#FFFFFF", target.get("text"));
        replacements.put("#172033", target.get("text"));
        replacements.put("#42B8FF", target.get("accent"));
        replacements.put("#10141C", light ? "#FFFFFF" : "#10141C");
        replacements.put("#171C26", light ? "#F4F6FA" : "#171C26");
        replacements.put("#343D4D", target.get("border"));
        replacements.put("#34451F", light ? "#DFEBC8" : "#34451F");
        replacements.put("#29351B", light ? "#EAF3D7" : "#29351B");
        replacements.put("#26331A", light ? "#DFEBC8" : "#26331A");
        replacements.put("#FFFFFF", light ? "#FFFFFF" : target.get("text"));
        return replacements;
    }

    private static void retintInlineStyles(Node node, Map<String, String> replacements) {
        String style = node.getStyle();
        if (style != null && !style.isBlank()) {
            Matcher matcher = HEX_COLOUR.matcher(style);
            StringBuffer updated = new StringBuffer();
            while (matcher.find()) {
                String replacement = replacements.get(matcher.group().toUpperCase(Locale.ROOT));
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
