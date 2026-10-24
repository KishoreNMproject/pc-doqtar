package org.pcdoqtar.ui;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.pcdoqtar.diagnostics.SystemDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics;
import org.pcdoqtar.settings.AppSettings;
import org.pcdoqtar.ui.navigation.NavigationController;
import org.pcdoqtar.ui.navigation.PageRegistry;
import org.pcdoqtar.ui.navigation.SidebarFactory;
import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class DashboardViewFactory {

    private static final double DEFAULT_WIDTH = 1180;
    private static final double DEFAULT_HEIGHT = 760;

    private DashboardViewFactory() { }

    public static DashboardView create(
            SystemDiagnostics systemDiagnostics,
            NetworkDiagnostics networkDiagnostics) {
        Objects.requireNonNull(systemDiagnostics, "System diagnostics is required.");
        Objects.requireNonNull(networkDiagnostics, "Network diagnostics is required.");

        BorderPane root = new BorderPane();
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("dashboard-shell");
        PageRegistry registry = new PageRegistry();
        registry.register("Overview", new OverviewPage(systemDiagnostics));
        registry.register("Diagnostics", new DiagnosticsPage(systemDiagnostics));
        registry.register("Network", new NetworkPage(networkDiagnostics));
        registry.register("Processes", new ProcessesPage());
        registry.register("Settings", new SettingsPage());

        VBox sidebar = SidebarFactory.create(page -> {
            NavigationController controller = navigationReference.get();
            if (controller != null) {
                controller.navigateTo(page);
                SidebarFactory.setActive(page);
            }
        });

        Button menuButton = new Button("☰");
        menuButton.getStyleClass().add("hamburger-button");
        menuButton.setAccessibleText("Toggle navigation menu");
        menuButton.setFocusTraversable(true);

        Label sectionTitle = new Label("PC DOQTAR");
        sectionTitle.getStyleClass().add("navigation-section-title");
        HBox navigationBar = new HBox(14, menuButton, sectionTitle);
        navigationBar.getStyleClass().add("navigation-toolbar");
        navigationBar.setPadding(new Insets(10, 18, 10, 18));

        NavigationController navigation =
                new NavigationController(registry, shell, page -> {
                    SidebarFactory.setActive(page);
                    sectionTitle.setText(page);
                });
        navigationReference.set(navigation);

        menuButton.setOnAction(event -> {
            if (shell.getLeft() == null) {
                shell.setLeft(sidebar);
            } else {
                shell.setLeft(null);
            }
        });

        shell.setTop(navigationBar);
        shell.setLeft(AppSettings.isNavigationCollapsedByDefault() ? null : sidebar);
        root.setCenter(shell);

        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        ThemeManager.apply(scene);
        navigation.navigateTo("Overview");

        return new DashboardView(root, scene, registry, navigation, sidebar);
    }

    private static final AtomicReference<NavigationController> navigationReference =
            new AtomicReference<>();
}
