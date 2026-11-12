package org.pcdoqtar.ui;

import java.util.Objects;

import org.pcdoqtar.auth.AuthSession;
import org.pcdoqtar.auth.FirebaseAuthService;
import org.pcdoqtar.auth.LoginDialog;
import org.pcdoqtar.connection.ServerHandover;
import org.pcdoqtar.connection.ServerHandover.UploadOutcome;
import org.pcdoqtar.connection.SyncCoordinator;
import org.pcdoqtar.connection.SyncCoordinator.SyncStatus;
import org.pcdoqtar.diagnostics.DiagnosticTaskManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

/**
 * Optional account area for the title bar.
 * Signed out: one "Sign in to sync" button (the app works fully offline).
 * Signed in : sync status + "Sync now" + "Sign out".
 */
public final class AccountControl extends HBox {

    private final Window owner;
    private final FirebaseAuthService authService;
    private final AuthSession session;
    private final DiagnosticTaskManager taskManager;
    private final ServerHandover handover;
    private final SyncCoordinator syncCoordinator;

    private final Label status = new Label();
    private final Button signInButton = new Button("Sign in to sync");
    private final Button syncButton = new Button("Sync now");
    private final Button signOutButton = new Button("Sign out");

    private boolean busy;

    public AccountControl(
            Window owner,
            FirebaseAuthService authService,
            AuthSession session,
            DiagnosticTaskManager taskManager,
            ServerHandover handover) {
        this(owner, authService, session, taskManager, handover, null);
    }

    public AccountControl(
            Window owner,
            FirebaseAuthService authService,
            AuthSession session,
            DiagnosticTaskManager taskManager,
            ServerHandover handover,
            SyncCoordinator syncCoordinator) {

        this.owner = owner;
        this.authService = Objects.requireNonNull(authService);
        this.session = Objects.requireNonNull(session);
        this.taskManager = Objects.requireNonNull(taskManager);
        this.handover = Objects.requireNonNull(handover);
        this.syncCoordinator = syncCoordinator;

        setAlignment(Pos.CENTER_RIGHT);
        setSpacing(8);
        setPadding(new Insets(0, 10, 0, 0));

        status.getStyleClass().add("page-description");
        status.setMaxWidth(300);

        signInButton.getStyleClass().add("primary-action");
        signInButton.setTooltip(new Tooltip(
                "Optional. Sign in to save your diagnostics to the server."));

        for (Button button : new Button[] {signInButton, syncButton, signOutButton}) {
            button.setFocusTraversable(false);
            button.setStyle("-fx-padding: 5 12 5 12;");
        }

        signInButton.setOnAction(event -> signIn());
        syncButton.setOnAction(event -> syncNow());
        signOutButton.setOnAction(event -> signOut());

        getChildren().addAll(status, signInButton, syncButton, signOutButton);

        if (syncCoordinator != null) {
            syncCoordinator.addStatusListener(syncStatus ->
                    Platform.runLater(() -> applyCoordinatorStatus(syncStatus)));
        }

        refresh();
    }

    private void applyCoordinatorStatus(SyncStatus syncStatus) {
        if (!session.isLoggedIn()) {
            refresh();
            return;
        }

        switch (syncStatus) {
            case SYNCING -> {
                busy = true;
                syncButton.setDisable(true);
                setStatus("Syncing…");
            }
            case SYNCED -> {
                busy = false;
                syncButton.setDisable(false);
                setStatus("Synced \u2713");
            }
            case SYNC_FAILED -> {
                busy = false;
                syncButton.setDisable(false);
                setStatus("Sync failed");
            }
            case OFFLINE -> {
                busy = false;
                syncButton.setDisable(false);
                setStatus("Offline");
            }
            case UNAUTHENTICATED -> {
                busy = false;
                syncButton.setDisable(false);
                refresh();
            }
        }
    }

    private void signIn() {
        boolean signedIn = LoginDialog.showAndSignIn(owner, authService);
        refresh();

        if (signedIn) {
            syncNow();
        }
    }

    private void signOut() {
        authService.logout();
        setStatus("Offline");
        refresh();
    }

    private void syncNow() {
        if (busy) {
            return;
        }

        if (!session.isLoggedIn()) {
            refresh();
            return;
        }

        busy = true;
        syncButton.setDisable(true);
        setStatus("Syncing…");

        if (syncCoordinator != null) {
            syncCoordinator.syncNow().whenComplete((outcome, error) ->
                    Platform.runLater(() -> {
                        busy = false;
                        syncButton.setDisable(false);
                        finish(outcome, error);
                    }));
            return;
        }

        try {
            taskManager.collectSnapshot()
                    .thenApply(handover::uploadSnapshot)
                    .whenComplete((outcome, error) ->
                            Platform.runLater(() -> finish(outcome, error)));
        } catch (RuntimeException exception) {
            finish(null, exception);
        }
    }

    private void finish(UploadOutcome outcome, Throwable error) {
        busy = false;
        syncButton.setDisable(false);

        if (error != null || outcome == null) {
            System.err.println("[PC DOQTAR] Sync failed: " + error);
            setStatus("Sync failed");
        } else if (outcome.success()) {
            setStatus("Synced \u2713");
        } else {
            System.err.println("[PC DOQTAR] Sync failed: " + outcome.message());
            if (!SyncCoordinator.probeInternetConnectivity()) {
                setStatus("Offline");
            } else {
                setStatus("Sync failed");
            }
        }

        refresh();
    }

    private void refresh() {
        boolean signedIn = session.isLoggedIn();

        show(signInButton, !signedIn);
        show(syncButton, signedIn);
        show(signOutButton, signedIn);

        if (signedIn) {
            if (status.getText() == null || status.getText().isBlank()) {
                setStatus("Signed in as " + session.getEmail());
            }
        } else {
            if (!SyncCoordinator.probeInternetConnectivity()) {
                setStatus("Offline");
            } else if (status.getText() == null || status.getText().isBlank()
                    || status.getText().startsWith("Syncing")
                    || status.getText().startsWith("Signed")) {
                status.setText("");
                status.setTooltip(null);
            }
        }
    }

    private void setStatus(String text) {
        status.setText(text);
        status.setTooltip(text == null || text.isBlank() ? null : new Tooltip(text));
    }

    // Accessors for unit testing
    public String getStatusText() {
        return status.getText();
    }

    public Button getSyncButton() {
        return syncButton;
    }

    public Button getSignInButton() {
        return signInButton;
    }

    public Button getSignOutButton() {
        return signOutButton;
    }

    public void finishForTest(UploadOutcome outcome, Throwable error) {
        finish(outcome, error);
    }

    private static void show(Button button, boolean visible) {
        button.setVisible(visible);
        button.setManaged(visible);
    }
}
