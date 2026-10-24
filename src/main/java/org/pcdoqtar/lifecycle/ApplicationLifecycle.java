
package org.pcdoqtar.lifecycle;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.pcdoqtar.ui.CustomTitleBar;
import org.pcdoqtar.ui.DashboardView;
import org.pcdoqtar.ui.DashboardViewFactory;
import org.pcdoqtar.ui.theme.ThemeManager;

public final class ApplicationLifecycle {

    private final ShutdownManager shutdownManager = new ShutdownManager();

    private ApplicationServices services;
    private boolean started;
    private boolean stopped;

    public void start(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException(
                    "Primary stage must not be null.");
        }

        if (started) {
            throw new IllegalStateException(
                    "Application lifecycle has already started.");
        }

        started = true;
        shutdownManager.initialise(stage, this::shutdown);

        try {
            if (!StartupConfiguration.hasFirebaseApiKey()) {
                ApplicationErrorHandler.showStartupError(
                        stage,
                        StartupConfiguration.getFirebaseConfigurationError());
                shutdownManager.shutdown();
                return;
            }

            services = new ApplicationServices();

            DashboardView dashboard = DashboardViewFactory.create(
                    services.getSystemDiagnostics(),
                    services.getNetworkDiagnostics());

            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle("PC DOQTAR | System Health");
            stage.setMinWidth(1000);
            stage.setMinHeight(700);

            BorderPane root = dashboard.getRoot();
            root.setTop(new CustomTitleBar(stage));

            Scene scene = dashboard.getScene();
            ThemeManager.apply(scene);

            stage.setScene(scene);
            stage.show();

        } catch (RuntimeException exception) {
            ApplicationErrorHandler.showStartupError(
                    stage,
                    exception.getMessage() == null
                            ? exception.getClass().getSimpleName()
                            : exception.getMessage());

            shutdownManager.shutdown();
        }
    }

    public void shutdown() {
        if (stopped) {
            return;
        }

        stopped = true;

        if (services != null) {
            services.close();
            services = null;
        }
    }
}
