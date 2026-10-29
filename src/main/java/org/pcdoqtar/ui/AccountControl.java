package org.pcdoqtar.ui;

import java.util.Objects;

import org.pcdoqtar.auth.AuthSession;
import org.pcdoqtar.auth.FirebaseAuthService;
import org.pcdoqtar.auth.LoginDialog;
import org.pcdoqtar.connection.ServerHandover;
import org.pcdoqtar.connection.ServerHandover.UploadOutcome;
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

        this.owner = owner;
        this.authService = Objects.requireNonNull(authService);
        this.session = Objects.requireNonNull(session);
        this.taskManager = Objects.requireNonNull(taskManager);
        this.handover = Objects.requireNonNull(handover);

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
        refresh();
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
        setStatus("Signed out. Working offline.");
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
            setStatus("Sync failed: could not collect or send diagnostics.");
        } else if (outcome.success()) {
            setStatus("Synced \u2713  (record #" + outcome.recordId() + ")");
        } else {
            System.err.println("[PC DOQTAR] Sync failed: " + outcome.message());
            setStatus("Sync failed: " + outcome.message());
        }

        refresh();
    }

    private void refresh() {
        boolean signedIn = session.isLoggedIn();

        show(signInButton, !signedIn);
        show(syncButton, signedIn);
        show(signOutButton, signedIn);

        if (signedIn && (status.getText() == null || status.getText().isBlank())) {
            setStatus("Signed in as " + session.getEmail());
        }
        if (!signedIn && (status.getText() == null || status.getText().isBlank()
                || status.getText().startsWith("Syncing"))) {
            setStatus("Offline");
        }
    }

    private void setStatus(String text) {
        status.setText(text);
        status.setTooltip(new Tooltip(text));
    }

    private static void show(Button button, boolean visible) {
        button.setVisible(visible);
        button.setManaged(visible);
    }
}
