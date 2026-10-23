package org.pcdoqtar.ui;

import java.util.Objects;

import org.pcdoqtar.diagnostics.SystemDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics;
import org.pcdoqtar.ui.navigation.NavigationController;
import org.pcdoqtar.ui.navigation.PageRegistry;
import org.pcdoqtar.ui.navigation.SidebarFactory;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public final class DashboardViewFactory {

    private static final double DEFAULT_WIDTH = 1180;
    private static final double DEFAULT_HEIGHT = 760;

    private DashboardViewFactory() {
        // Utility class.
    }

    public static DashboardView create(
            SystemDiagnostics systemDiagnostics,
            NetworkDiagnostics networkDiagnostics) {

        Objects.requireNonNull(
                systemDiagnostics, "System diagnostics is required.");
        Objects.requireNonNull(
                networkDiagnostics, "Network diagnostics is required.");

        BorderPane root = new BorderPane();
        PageRegistry registry = new PageRegistry();

        registry.register(
                "Overview",
                new OverviewPage(systemDiagnostics));

        registry.register(
                "Diagnostics",
                new DiagnosticsPage(systemDiagnostics));

        registry.register(
                "Network",
                new NetworkPage(networkDiagnostics));

        registry.register(
                "Processes",
                new ProcessesPage());

        registry.register(
                "Settings",
                new SettingsPage());

        NavigationController navigation =
                new NavigationController(registry, root, null);

        VBox sidebar = SidebarFactory.create(navigation::navigateTo);
        root.setLeft(sidebar);

        Scene scene = new Scene(
                root,
                DEFAULT_WIDTH,
                DEFAULT_HEIGHT);

        navigation.navigateTo("Overview");

        return new DashboardView(
                root,
                scene,
                registry,
                navigation,
                sidebar);
    }
}