package org.pcdoqtar.ui.theme;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pcdoqtar.settings.AppSettings;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

class ThemeCssTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    @DisplayName("Dark theme stylesheet loads and applies to Scene without ClassCastException")
    void testDarkThemeStylesheetApplication() {
        BorderPane root = new BorderPane();
        VBox content = new VBox();
        content.getStyleClass().add("app-tile");
        root.setCenter(content);

        Scene scene = new Scene(root, 800, 600);
        AppSettings.setTheme("Dark");

        assertDoesNotThrow(() -> {
            ThemeManager.apply(scene);
        }, "ThemeManager.apply(scene) must not throw ClassCastException");

        assertTrue(scene.getRoot().getStyleClass().contains("theme-dark"));
        assertFalse(scene.getRoot().getStyleClass().contains("theme-light"));

        URL darkCss = getClass().getResource("/org/pcdoqtar/ui/theme/theme-dark.css");
        assertNotNull(darkCss);
        assertTrue(scene.getStylesheets().contains(darkCss.toExternalForm()));
    }

    @Test
    @DisplayName("Light theme stylesheet loads and applies to Scene without ClassCastException")
    void testLightThemeStylesheetApplication() {
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 800, 600);
        AppSettings.setTheme("Light");

        assertDoesNotThrow(() -> {
            ThemeManager.apply(scene);
        }, "ThemeManager.apply(scene) for Light theme must not throw ClassCastException");

        assertTrue(scene.getRoot().getStyleClass().contains("theme-light"));
        assertFalse(scene.getRoot().getStyleClass().contains("theme-dark"));

        URL lightCss = getClass().getResource("/org/pcdoqtar/ui/theme/theme-light.css");
        assertNotNull(lightCss);
        assertTrue(scene.getStylesheets().contains(lightCss.toExternalForm()));
    }

    @Test
    @DisplayName("Theme switching swaps stylesheets cleanly")
    void testThemeSwitching() {
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 800, 600);

        // Switch to Dark
        AppSettings.setTheme("Dark");
        ThemeManager.apply(scene);
        assertTrue(scene.getRoot().getStyleClass().contains("theme-dark"));

        // Switch to Light
        AppSettings.setTheme("Light");
        ThemeManager.apply(scene);
        assertTrue(scene.getRoot().getStyleClass().contains("theme-light"));
        assertFalse(scene.getRoot().getStyleClass().contains("theme-dark"));

        // Switch back to Dark
        AppSettings.setTheme("Dark");
        ThemeManager.apply(scene);
        assertTrue(scene.getRoot().getStyleClass().contains("theme-dark"));
    }
}
