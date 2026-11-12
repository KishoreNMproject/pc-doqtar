package org.pcdoqtar.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FirebaseAuthServiceTest {

    @Nested
    @DisplayName("Email Input Validation")
    class EmailValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "user@example.com",
                "first.last@domain.org",
                "doctor+diagnostics@pcdoqtar.health",
                "admin123@sub.domain.co.in"
        })
        void validEmailsAreAccepted(String email) {
            assertTrue(FirebaseAuthService.isValidEmail(email),
                    () -> "Expected valid: " + email);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "   ",
                "not-an-email",
                "missing-at-sign.com",
                "@no-local-part.com",
                "user@no-tld",
                "user@.domain.com",
                "user@domain..com"
        })
        void invalidEmailsAreRejected(String email) {
            assertFalse(FirebaseAuthService.isValidEmail(email),
                    () -> "Expected invalid: " + email);
        }

        @Test
        void nullEmailIsRejected() {
            assertFalse(FirebaseAuthService.isValidEmail(null));
        }
    }

    @Nested
    @DisplayName("Firebase Password-Reset Error Mapping")
    class ErrorMappingTests {

        @Test
        void mapsInvalidAndMissingEmail() {
            assertEquals("Enter a valid email address.",
                    FirebaseAuthService.toPasswordResetUserMessage("INVALID_EMAIL"));
            assertEquals("Enter a valid email address.",
                    FirebaseAuthService.toPasswordResetUserMessage("MISSING_EMAIL"));
            assertEquals("Enter a valid email address.",
                    FirebaseAuthService.toPasswordResetUserMessage("INVALID_EMAIL : something"));
        }

        @Test
        void mapsRateLimiting() {
            assertEquals("Too many attempts. Try again later.",
                    FirebaseAuthService.toPasswordResetUserMessage("TOO_MANY_ATTEMPTS_TRY_LATER"));
        }

        @Test
        void mapsInvalidApiKey() {
            assertEquals("The Firebase Web API key is invalid.",
                    FirebaseAuthService.toPasswordResetUserMessage("API_KEY_INVALID"));
        }

        @Test
        void mapsOperationNotAllowed() {
            assertEquals("Password reset is not enabled in Firebase.",
                    FirebaseAuthService.toPasswordResetUserMessage("OPERATION_NOT_ALLOWED"));
        }

        @Test
        void sanitizesUnknownErrorsWithoutLeakingSensitiveDetails() {
            String sanitized = FirebaseAuthService.toPasswordResetUserMessage("UNKNOWN_INTERNAL_ERROR_xyz");
            assertEquals("Password reset request failed. Please try again later.", sanitized);
            assertFalse(sanitized.contains("UNKNOWN_INTERNAL_ERROR_xyz"),
                    "Raw Firebase error details must never be exposed to the user.");
        }

        @Test
        void handlesNullAndBlankErrors() {
            assertEquals("Unable to send password reset email. Check your connection and try again.",
                    FirebaseAuthService.toPasswordResetUserMessage(null));
            assertEquals("Unable to send password reset email. Check your connection and try again.",
                    FirebaseAuthService.toPasswordResetUserMessage("   "));
        }
    }

    @Nested
    @DisplayName("Password Reset Execution & Configuration")
    class ResetExecutionTests {

        @Test
        void throwsWhenApiKeyNotConfigured() {
            AuthSession session = new AuthSession();
            FirebaseAuthService service = new FirebaseAuthService(session, null);

            IOException ex = assertThrows(IOException.class, () ->
                    service.sendPasswordResetEmail("test@example.com"));
            assertTrue(ex.getMessage().contains("FIREBASE_WEB_API_KEY is not configured"),
                    "Expected missing key error message.");
        }

        @Test
        void resolvesApiKeyFromSystemOrRegistry() {
            String resolved = FirebaseAuthService.resolveApiKey();
            assertNotNull(resolved, "Firebase Web API key should be resolved from environment or registry.");
            assertFalse(resolved.isBlank(), "Resolved Firebase Web API key must not be blank.");
        }

        @Test
        void rejectsBlankEmailBeforeNetworkCall() {
            AuthSession session = new AuthSession();
            FirebaseAuthService service = new FirebaseAuthService(session);

            IOException ex = assertThrows(IOException.class, () ->
                    service.sendPasswordResetEmail("   "));
            assertTrue(ex.getMessage().contains("Enter your email address")
                    || ex.getMessage().contains("FIREBASE_WEB_API_KEY is not configured"));
        }

        @Test
        void sessionIsNeverModifiedByPasswordReset() {
            AuthSession session = new AuthSession();
            session.setTokens("uid123", "logged-in@example.com", "id-tok", "ref-tok", 3600);

            FirebaseAuthService service = new FirebaseAuthService(session);
            assertTrue(session.isLoggedIn());
            assertEquals("logged-in@example.com", session.getEmail());

            // Attempting reset for another email address must not touch session state
            try {
                service.sendPasswordResetEmail("other@example.com");
            } catch (Exception ignored) {
            }

            assertTrue(session.isLoggedIn(), "Session must remain logged in.");
            assertEquals("uid123", session.getUid());
            assertEquals("logged-in@example.com", session.getEmail());
            assertEquals("id-tok", session.getIdToken());
            assertEquals("ref-tok", session.getRefreshToken());
        }
    }

    @Nested
    @DisplayName("AuthenticationCoordinator Integration")
    class CoordinatorTests {

        @Test
        void coordinatorValidatesInputBeforeExecuting() throws ExecutionException, InterruptedException {
            AuthSession session = new AuthSession();
            FirebaseAuthService service = new FirebaseAuthService(session);
            try (AuthenticationCoordinator coordinator = new AuthenticationCoordinator(service)) {
                CompletableFuture<AuthenticationCoordinator.AuthenticationResult> blankResult =
                        coordinator.resetPassword("   ");
                assertFalse(blankResult.get().authenticated());
                assertEquals("Enter your email address.", blankResult.get().message());

                CompletableFuture<AuthenticationCoordinator.AuthenticationResult> invalidResult =
                        coordinator.resetPassword("bad-email");
                assertFalse(invalidResult.get().authenticated());
                assertEquals("Enter a valid email address.", invalidResult.get().message());
            }
        }
    }

    @Nested
    @DisplayName("Existing Authentication Flow Regressions")
    class ExistingAuthBehaviorTests {

        @Test
        void signInRejectsMissingCredentials() {
            AuthSession session = new AuthSession();
            FirebaseAuthService service = new FirebaseAuthService(session);

            IOException ex1 = assertThrows(IOException.class, () -> service.signIn("", "pass"));
            assertTrue(ex1.getMessage().contains("Enter both email and password")
                    || ex1.getMessage().contains("FIREBASE_WEB_API_KEY is not configured"));

            IOException ex2 = assertThrows(IOException.class, () -> service.signIn("user@test.com", ""));
            assertTrue(ex2.getMessage().contains("Enter both email and password")
                    || ex2.getMessage().contains("FIREBASE_WEB_API_KEY is not configured"));
        }

        @Test
        void signUpRejectsMissingCredentials() {
            AuthSession session = new AuthSession();
            FirebaseAuthService service = new FirebaseAuthService(session);

            IOException ex1 = assertThrows(IOException.class, () -> service.signUp("", "pass"));
            assertTrue(ex1.getMessage().contains("Enter both email and password")
                    || ex1.getMessage().contains("FIREBASE_WEB_API_KEY is not configured"));

            IOException ex2 = assertThrows(IOException.class, () -> service.signUp("user@test.com", ""));
            assertTrue(ex2.getMessage().contains("Enter both email and password")
                    || ex2.getMessage().contains("FIREBASE_WEB_API_KEY is not configured"));
        }

        @Test
        void logoutClearsExistingSession() {
            AuthSession session = new AuthSession();
            session.setTokens("uid1", "test@example.com", "id1", "refresh1", 3600);
            assertTrue(session.isLoggedIn());

            FirebaseAuthService service = new FirebaseAuthService(session);
            service.logout();

            assertFalse(session.isLoggedIn());
            assertNull(session.getEmail());
            assertNull(session.getIdToken());
            assertNull(session.getRefreshToken());
            assertEquals(0L, session.getExpiresAtMillis());
        }

        @Test
        void getValidIdTokenReturnsNullWhenLoggedOut() throws Exception {
            AuthSession session = new AuthSession();
            FirebaseAuthService service = new FirebaseAuthService(session);
            assertNull(service.getValidIdToken());
        }
    }
}
