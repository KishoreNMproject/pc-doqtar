package org.pcdoqtar.ui.navigation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class SidebarFactory {

    private static final List<String> PAGE_NAMES =
            List.of("Overview", "Diagnostics", "Network", "Processes", "Settings");
    private static final Map<String, Button> BUTTONS = new LinkedHashMap<>();

    private SidebarFactory() { }

    public static VBox create(Consumer<String> onNavigate) {
        if (onNavigate == null) {
            throw new IllegalArgumentException("Navigation callback must not be null.");
        }

        BUTTONS.clear();
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("app-sidebar");
        sidebar.setPadding(new Insets(18, 10, 18, 10));
        sidebar.setPrefWidth(190);
        sidebar.setMinWidth(190);

        Label title = new Label("PC DOQTAR");
        title.getStyleClass().add("sidebar-brand");
        Label subtitle = new Label("SYSTEM HEALTH");
        subtitle.getStyleClass().add("sidebar-subtitle");
        VBox branding = new VBox(4, title, subtitle);
        branding.setPadding(new Insets(0, 8, 20, 8));
        sidebar.getChildren().add(branding);

        for (String pageName : PAGE_NAMES) {
            Button button = new Button(pageName);
            button.getStyleClass().add("sidebar-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setPadding(new Insets(12, 14, 12, 14));
            button.setFocusTraversable(true);
            button.setOnAction(event -> onNavigate.accept(pageName));
            BUTTONS.put(pageName, button);
            sidebar.getChildren().add(button);
        }
        setActive("Overview");
        return sidebar;
    }

    public static void setActive(String pageName) {
        BUTTONS.forEach((name, button) -> {
            button.getStyleClass().remove("active");
            if (name.equals(pageName)) {
                button.getStyleClass().add("active");
            }
        });
    }
}
