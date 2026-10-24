package org.pcdoqtar.ui.theme;

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
        retintInlineStyles(node, getCurrentTheme());
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

    private static String replacementFor(
            String property, String colour, String targetTheme) {
        Map<String, String> target = "Light".equalsIgnoreCase(targetTheme) ? LIGHT : DARK;
        String value = colour.toUpperCase(Locale.ROOT);
        String key = null;

        for (String candidate : DARK.keySet()) {
            if (value.equals(DARK.get(candidate)) || value.equals(LIGHT.get(candidate))) {
                key = candidate;
                break;
            }
        }

        String normalizedProperty = property.toLowerCase(Locale.ROOT);

        if (normalizedProperty.contains("text-fill")
                || normalizedProperty.contains("text-background")
                || normalizedProperty.contains("text-base")
                || normalizedProperty.equals("-fx-fill")
                || normalizedProperty.contains("tick-label-fill")) {
            if (value.equals("#FFFFFF") || value.equals("#F5F5F5")
                    || value.equals("#F2F5FA") || value.equals("#172033")
                    || value.equals("#243247")) {
                return target.get("text");
            }
            if (value.equals("#A0A9B8") || value.equals("#58677B")
                    || value.equals("#929292") || value.equals("#8993A3")
                    || value.equals("#666666") || value.equals("#B0B0B0")) {
                return target.get("muted");
            }
            if (value.equals("#101010")) {
                return "Light".equalsIgnoreCase(targetTheme) ? "#FFFFFF" : "#090B10";
            }
            if (key != null) {
                return target.get(key);
            }
            return null;
        }

        if (normalizedProperty.contains("background")
                || normalizedProperty.contains("control-inner")) {
            if (key != null) {
                return target.get(key);
            }
            return switch (value) {
                case "#090909", "#0D0D0D" -> target.get("background");
                case "#101010" -> "Light".equalsIgnoreCase(targetTheme) ? "#FFFFFF" : "#10141C";
                case "#111111", "#141414" -> target.get("panel");
                case "#151515", "#171717" -> "Light".equalsIgnoreCase(targetTheme) ? "#F4F6FA" : "#171C26";
                case "#202020" -> "Light".equalsIgnoreCase(targetTheme) ? "#E2E8F0" : "#202633";
                case "#34451F", "#26331A" -> "Light".equalsIgnoreCase(targetTheme) ? "#DFEBC8" : "#34451F";
                case "#29351B" -> "Light".equalsIgnoreCase(targetTheme) ? "#EAF3D7" : "#29351B";
                default -> null;
            };
        }

        if (normalizedProperty.contains("border")) {
            if (value.equals("#FFB020") || value.equals("#925400")) {
                return target.get("warning");
            }
            if (value.equals("#FF5555") || value.equals("#B42318")
                    || value.equals("#C62828")) {
                return target.get("critical");
            }
            if (value.equals("#B8FF3B") || value.equals("#647D35")
                    || value.equals("#527A00")) {
                return target.get("accent");
            }
            if (key != null) {
                return target.get(key);
            }
            if (value.equals("#383838") || value.equals("#444444")
                    || value.equals("#343D4D") || value.equals("#292929")) {
                return target.get("border");
            }
            return null;
        }

        if (normalizedProperty.contains("pie-color")) {
            if (key != null) {
                return target.get(key);
            }
            if (value.equals("#454545")) {
                return target.get("grey");
            }
            return null;
        }

        if (key != null) {
            return target.get(key);
        }
        return null;
    }

    private static void retintInlineStyles(Node node, String targetTheme) {
        String style = node.getStyle();
        if (style != null && !style.isBlank()) {
            Matcher matcher = HEX_COLOUR.matcher(style);
            StringBuffer updated = new StringBuffer();
            while (matcher.find()) {
                int declarationStart = style.lastIndexOf(';', matcher.start()) + 1;
                int colon = style.indexOf(':', declarationStart);
                String property = colon >= 0 && colon < matcher.start()
                        ? style.substring(declarationStart, colon).trim() : "";
                String replacement = replacementFor(property, matcher.group(), targetTheme);
                matcher.appendReplacement(updated, Matcher.quoteReplacement(
                        replacement == null ? matcher.group() : replacement));
            }
            matcher.appendTail(updated);
            node.setStyle(updated.toString());
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                retintInlineStyles(child, targetTheme);
            }
        }
    }
}
