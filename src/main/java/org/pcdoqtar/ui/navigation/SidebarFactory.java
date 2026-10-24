
package org.pcdoqtar.ui.navigation;

import java.util.List;
import java.util.function.Consumer;

import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class SidebarFactory {

    private static final List<String> PAGE_NAMES = List.of(
            "Overview", "Diagnostics", "Network", "Processes", "Settings");

    private SidebarFactory() {
    }

    public static VBox create(Consumer<String> onNavigate) {
        if (onNavigate == null) {
            throw new IllegalArgumentException(
                    "Navigation callback must not be null.");
        }

        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("app-sidebar");
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setPrefWidth(190);
        sidebar.setAlignment(Pos.TOP_LEFT);
        sidebar.setStyle(
                "-fx-background-color: "
                + ThemeManager.getColour("panel")
                + "; -fx-border-color: "
                + ThemeManager.getColour("border")
                + "; -fx-border-width: 0 1 0 0;");

        Label title = new Label("PC DOQTAR");
        title.setStyle(
                "-fx-text-fill: " + ThemeManager.getColour("accent")
                + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label subtitle = new Label("SYSTEM HEALTH");
        subtitle.setStyle(
                "-fx-text-fill: " + ThemeManager.getColour("muted")
                + "; -fx-font-size: 10px;");

        VBox branding = new VBox(4, title, subtitle);
        branding.setPadding(new Insets(0, 0, 20, 4));
        sidebar.getChildren().add(branding);

        for (String pageName : PAGE_NAMES) {
            Button button = new Button(pageName);
            button.getStyleClass().add("sidebar-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setPadding(new Insets(11, 12, 11, 12));
            button.setStyle(
                    "-fx-background-color: transparent;"
                    + " -fx-text-fill: "
                    + ThemeManager.getColour("text")
                    + "; -fx-font-size: 13px;"
                    + " -fx-background-radius: 6px;"
                    + " -fx-cursor: hand;");

            button.setOnAction(event -> onNavigate.accept(pageName));
            sidebar.getChildren().add(button);
        }

        return sidebar;
    }
}
