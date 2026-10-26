
package org.pcdoqtar.lifecycle;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.image.Image;

import org.pcdoqtar.app.DashboardApp;
import org.pcdoqtar.ui.AccountControl;
import org.pcdoqtar.ui.CustomTitleBar;
import org.pcdoqtar.ui.DashboardView;
import org.pcdoqtar.ui.DashboardViewFactory;
import org.pcdoqtar.ui.theme.ThemeManager;

@SuppressWarnings("unused")
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
            // Local diagnostics never need an account. Sign-in is optional
            // and is offered from the title bar (see AccountControl).
            services = new ApplicationServices();

            DashboardView dashboard = DashboardViewFactory.create(
                    services.getSystemDiagnostics(),
                    services.getNetworkDiagnostics());

            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle("PC DOQTAR | System Health");
            stage.setMinWidth(1000);
            stage.setMinHeight(700);

            CustomTitleBar titleBar = new CustomTitleBar(stage);

            // Children are: title, spacer, minimise, maximise, close.
            // Put the account area between the spacer and the window buttons.
            titleBar.getChildren().add(2, new AccountControl(
                    stage,
                    services.getAuthService(),
                    services.getAuthSession(),
                    services.getDiagnosticTaskManager(),
                    services.getServerHandover()));

            BorderPane root = dashboard.getRoot();
            root.setTop(titleBar);

            Scene scene = dashboard.getScene();
            ThemeManager.apply(scene);

            stage.setScene(scene);
            var iconUrl = DashboardApp.class.getResource(
                    "/org/pcdoqtar/app/pc-doqtar.png");

            if (iconUrl != null) {
                stage.getIcons().clear();
                stage.getIcons().add(new Image(iconUrl.toExternalForm()));
            }
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
