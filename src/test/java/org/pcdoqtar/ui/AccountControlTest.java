package org.pcdoqtar.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pcdoqtar.auth.AuthSession;
import org.pcdoqtar.auth.FirebaseAuthService;
import org.pcdoqtar.connection.ServerHandover;
import org.pcdoqtar.connection.ServerHandover.UploadOutcome;
import org.pcdoqtar.connection.SyncCoordinator;
import org.pcdoqtar.connection.SyncCoordinator.SyncStatus;
import org.pcdoqtar.diagnostics.DiagnosticTaskManager;
import org.pcdoqtar.diagnostics.SystemDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics;
import org.pcdoqtar.model.SystemHealthSnapshot;

import javafx.application.Platform;

class AccountControlTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit already started
        }
    }

    private AccountControl createAccountControl(AuthSession session, ServerHandover handover) {
        FirebaseAuthService authService = new FirebaseAuthService(session);
        DiagnosticTaskManager taskManager = new DiagnosticTaskManager(
                new SystemDiagnostics(), new NetworkDiagnostics());
        return new AccountControl(null, authService, session, taskManager, handover);
    }

    @Test
    @DisplayName("Title bar status must never expose database record IDs or row numbers on success")
    void testSuccessfulUploadHidesRecordId() {
        AuthSession session = new AuthSession();
        session.setTokens("u123", "test@pcdoqtar.local", "tok", "ref", 3600);

        ServerHandover handover = new ServerHandover(new FirebaseAuthService(session));
        AccountControl control = createAccountControl(session, handover);

        // Simulate finish with a recordId (e.g., record #4 or #999)
        control.finishForTest(UploadOutcome.saved(4L), null);

        String statusText = control.getStatusText();
        assertNotNull(statusText);
        assertEquals("Synced \u2713", statusText);
        assertFalse(statusText.contains("4"), "Status text must not contain record ID #4");
        assertFalse(statusText.toLowerCase().contains("record"), "Status text must not contain 'record'");

        // Simulate finish with another recordId #42
        control.finishForTest(UploadOutcome.saved(42L), null);
        assertEquals("Synced \u2713", control.getStatusText());
        assertFalse(control.getStatusText().contains("42"));
    }

    @Test
    @DisplayName("Title bar status must never expose internal error details or raw server messages on failure")
    void testFailureHidesInternalServerDetails() {
        AuthSession session = new AuthSession();
        session.setTokens("u123", "test@pcdoqtar.local", "tok", "ref", 3600);

        ServerHandover handover = new ServerHandover(new FirebaseAuthService(session));
        AccountControl control = createAccountControl(session, handover);

        // Simulate failure with internal details
        control.finishForTest(UploadOutcome.failed("SQLSTATE[HY000] [2002] Connection refused at MySQL port 3306"), null);

        String status = control.getStatusText();
        assertNotNull(status);
        assertFalse(status.contains("SQLSTATE"), "Internal SQL details must not be exposed");
        assertFalse(status.contains("3306"), "Port numbers must not be exposed");
        assertTrue(status.equals("Sync failed") || status.equals("Offline"));
    }

    @Test
    @DisplayName("Unauthenticated state displays Sign in to sync button")
    void testUnauthenticatedState() {
        AuthSession session = new AuthSession(); // Not logged in
        ServerHandover handover = new ServerHandover(new FirebaseAuthService(session));
        AccountControl control = createAccountControl(session, handover);

        assertTrue(control.getSignInButton().isVisible());
        assertFalse(control.getSyncButton().isVisible());
        assertFalse(control.getSignOutButton().isVisible());
        assertEquals("Sign in to sync", control.getSignInButton().getText());
    }

    @Test
    @DisplayName("Signed in state displays Sync now and Sign out buttons")
    void testSignedInState() {
        AuthSession session = new AuthSession();
        session.setTokens("u123", "user@pcdoqtar.local", "tok", "ref", 3600);

        ServerHandover handover = new ServerHandover(new FirebaseAuthService(session));
        AccountControl control = createAccountControl(session, handover);

        assertFalse(control.getSignInButton().isVisible());
        assertTrue(control.getSyncButton().isVisible());
        assertTrue(control.getSignOutButton().isVisible());
        assertTrue(control.getStatusText().contains("user@pcdoqtar.local"));
    }
}
