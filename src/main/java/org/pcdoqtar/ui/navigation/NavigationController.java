
package org.pcdoqtar.ui.navigation;

import java.util.function.Consumer;

import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;

public final class NavigationController {

    private final PageRegistry pageRegistry;
    private final BorderPane root;
    private final Consumer<String> pageChangeListener;

    private String currentPage;

    public NavigationController(
            PageRegistry pageRegistry,
            BorderPane root,
            Consumer<String> pageChangeListener) {

        if (pageRegistry == null || root == null) {
            throw new IllegalArgumentException(
                    "Page registry and root must not be null.");
        }

        this.pageRegistry = pageRegistry;
        this.root = root;
        this.pageChangeListener = pageChangeListener != null
                ? pageChangeListener : page -> { };
    }

    public void navigateTo(String pageName) {
        Node page = pageRegistry.get(pageName);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background-color: "
                + ThemeManager.getColour("background")
                + "; -fx-background: "
                + ThemeManager.getColour("background")
                + "; -fx-border-width: 0;");

        root.setCenter(scrollPane);
        ThemeManager.applyToNode(page);
        currentPage = pageName;
        pageChangeListener.accept(pageName);
    }

    public String getCurrentPage() {
        return currentPage;
    }
}
