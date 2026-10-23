
package org.pcdoqtar.ui.navigation;

import java.util.function.Consumer;

import javafx.scene.Node;
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
                    "Page registry and root must not be null."
            );
        }

        this.pageRegistry = pageRegistry;
        this.root = root;
        this.pageChangeListener = pageChangeListener != null
                ? pageChangeListener
                : page -> { };
    }

    public void navigateTo(String pageName) {
        Node page = pageRegistry.get(pageName);

        root.setCenter(page);
        currentPage = pageName;
        pageChangeListener.accept(pageName);
    }

    public String getCurrentPage() {
        return currentPage;
    }
}
