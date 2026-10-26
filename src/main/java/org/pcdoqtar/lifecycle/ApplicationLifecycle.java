
package org.pcdoqtar.lifecycle;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.image.Image;

import org.pcdoqtar.app.DashboardApp;
import org.pcdoqtar.auth.AuthenticationCoordinator.AuthenticationResult;
import org.pcdoqtar.ui.CustomTitleBar;
import org.pcdoqtar.ui.DashboardView;
import org.pcdoqtar.ui.DashboardViewFactory;
import org.pcdoqtar.ui.theme.ThemeManager;

@SuppressWarnings("unused")
public final class ApplicationLifecycle {

    private static final String BASE_TITLE = "PC DOQTAR | System Health";

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
            services = new ApplicationServices();

            // The login dialog is the only window open at first; stop JavaFX
            // from exiting when it closes. Re-enabled once the dashboard shows.
            Platform.setImplicitExit(false);

            promptForLogin(stage);

        } catch (RuntimeException exception) {
            ApplicationErrorHandler.showStartupError(
                    stage,
                    exception.getMessage() == null
                            ? exception.getClass().getSimpleName()
                            : exception.getMessage());

            shutdownManager.shutdown();
        }
    }

    // ---------- login ----------

    private void promptForLogin(Stage stage) {
        // Opens the dialog now (FX thread); Firebase call runs in background.
        services.getAuthenticationCoordinator()
                .authenticate(null)
                .whenComplete((result, error) ->
                        Platform.runLater(
                                () -> onAuthenticated(stage, result, error)));
    }

    private void onAuthenticated(
            Stage stage,
            AuthenticationResult result,
            Throwable error) {

        if (services == null) {
            return; // application was shut down meanwhile
        }

        if (error != null || result == null) {
            ApplicationErrorHandler.showError(
                    null, "PC DOQTAR — Sign-in failed",
                    "Sign-in could not be completed. Please try again.");
            promptForLogin(stage);
            return;
        }

        if (result.authenticated()) {
            showDashboard(stage, "signed in");
            syncWithServer(stage);

        } else if (result.cancelled()) {
            // Local diagnostics still work without an account.
            showDashboard(stage, "offline");

        } else {
            ApplicationErrorHandler.showError(
                    null, "PC DOQTAR — Sign-in failed", result.message());
            promptForLogin(stage);
        }
    }

    // ---------- dashboard ----------

    private void showDashboard(Stage stage, String mode) {
        try {
            DashboardView dashboard = DashboardViewFactory.create(
                    services.getSystemDiagnostics(),
                    services.getNetworkDiagnostics());

            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle(BASE_TITLE + " (" + mode + ")");
            stage.setMinWidth(1000);
            stage.setMinHeight(700);

            BorderPane root = dashboard.getRoot();
            root.setTop(new CustomTitleBar(stage));

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
            Platform.setImplicitExit(true);

        } catch (RuntimeException exception) {
            ApplicationErrorHandler.showStartupError(
                    stage,
                    exception.getMessage() == null
                            ? exception.getClass().getSimpleName()
                            : exception.getMessage());

            shutdownManager.shutdown();
        }
    }

    // ---------- server sync ----------

    /** Collects one snapshot and uploads it; result is shown in the title bar. */
    private void syncWithServer(Stage stage) {
        ApplicationServices current = services;

        current.getDiagnosticTaskManager()
                .collectSnapshot()
                .thenApply(snapshot ->
                        current.getServerHandover().uploadSnapshot(snapshot))
                .whenComplete((outcome, error) -> Platform.runLater(() -> {
                    String status;

                    if (error != null || outcome == null) {
                        status = "sync failed";
                    } else if (outcome.success()) {
                        status = "synced, record #" + outcome.recordId();
                    } else {
                        status = "sync failed: " + outcome.message();
                    }

                    stage.setTitle(BASE_TITLE + " (" + status + ")");
                }));
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
