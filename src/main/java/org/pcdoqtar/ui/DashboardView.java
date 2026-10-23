package org.pcdoqtar.ui;

import java.util.Objects;

import org.pcdoqtar.ui.navigation.NavigationController;
import org.pcdoqtar.ui.navigation.PageRegistry;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public final class DashboardView {

    private final BorderPane root;
    private final Scene scene;
    private final PageRegistry pageRegistry;
    private final NavigationController navigationController;
    private final VBox sidebar;

    public DashboardView(
            BorderPane root,
            Scene scene,
            PageRegistry pageRegistry,
            NavigationController navigationController,
            VBox sidebar) {

        this.root = Objects.requireNonNull(root, "Root is required.");
        this.scene = Objects.requireNonNull(scene, "Scene is required.");
        this.pageRegistry = Objects.requireNonNull(
                pageRegistry, "Page registry is required.");
        this.navigationController = Objects.requireNonNull(
                navigationController, "Navigation controller is required.");
        this.sidebar = Objects.requireNonNull(
                sidebar, "Sidebar is required.");
    }

    public BorderPane getRoot() {
        return root;
    }

    public Scene getScene() {
        return scene;
    }

    public PageRegistry getPageRegistry() {
        return pageRegistry;
    }

    public NavigationController getNavigationController() {
        return navigationController;
    }

    public VBox getSidebar() {
        return sidebar;
    }
}