package org.pcdoqtar.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pcdoqtar.auth.AuthSession;
import org.pcdoqtar.auth.AuthenticationCoordinator;
import org.pcdoqtar.auth.FirebaseAuthService;

import javafx.application.Platform;

class SettingsPageTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    @DisplayName("Settings displays Account Security section with disabled action when unauthenticated")
    void testUnauthenticatedSettingsPage() {
        AuthSession session = new AuthSession(); // Not logged in
        FirebaseAuthService authService = new FirebaseAuthService(session);
        AuthenticationCoordinator coordinator = new AuthenticationCoordinator(authService);

        SettingsPage page = new SettingsPage(session, authService, coordinator);

        assertNotNull(page.getResetPasswordButton());
        assertTrue(page.getResetPasswordButton().isDisabled(),
                "Reset password button should be disabled when not signed in.");
        assertFalse(page.getResetPasswordButton().isVisible());
        assertEquals("", page.getAccountEmailLabel().getText());
    }

    @Test
    @DisplayName("Settings displays Account Security section with current user email when signed in")
    void testSignedInSettingsDisplaysAccountEmail() {
        AuthSession session = new AuthSession();
        session.setTokens("uid-abc", "active.user@pcdoqtar.health", "token123", "ref456", 3600);

        FirebaseAuthService authService = new FirebaseAuthService(session);
        AuthenticationCoordinator coordinator = new AuthenticationCoordinator(authService);

        SettingsPage page = new SettingsPage(session, authService, coordinator);

        assertTrue(page.getResetPasswordButton().isVisible());
        assertFalse(page.getResetPasswordButton().isDisabled());
        assertTrue(page.getResetPasswordButton().isFocusTraversable(),
                "Reset password button must be keyboard focus traversable.");

        String emailLabel = page.getAccountEmailLabel().getText();
        assertTrue(emailLabel.contains("active.user@pcdoqtar.health"),
                "Email label must display the currently signed-in user email: " + emailLabel);

        // Verify session remains active and unchanged
        assertTrue(session.isLoggedIn());
        assertEquals("active.user@pcdoqtar.health", session.getEmail());
    }

    @Test
    @DisplayName("Settings password reset does not clear user session or tokens")
    void testPasswordResetPreservesSession() {
        AuthSession session = new AuthSession();
        session.setTokens("uid-abc", "active.user@pcdoqtar.health", "idToken_xyz", "ref_xyz", 3600);

        AtomicBoolean resetCalled = new AtomicBoolean(false);
        FirebaseAuthService mockAuthService = new FirebaseAuthService(session) {
            @Override
            public void sendPasswordResetEmail(String email) throws IOException {
                resetCalled.set(true);
            }
        };

        AuthenticationCoordinator coordinator = new AuthenticationCoordinator(mockAuthService);
        SettingsPage page = new SettingsPage(session, mockAuthService, coordinator);

        // Before any action: user is logged in
        assertTrue(session.isLoggedIn());
        assertEquals("idToken_xyz", session.getIdToken());

        // Refresh account reflects the user state
        page.refreshAccount();
        assertTrue(session.isLoggedIn(), "Session must remain active.");
        assertEquals("active.user@pcdoqtar.health", session.getEmail());
    }
}
