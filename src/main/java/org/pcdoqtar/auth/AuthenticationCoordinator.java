
package org.pcdoqtar.auth;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.stage.Window;

public final class AuthenticationCoordinator implements AutoCloseable {

    private final FirebaseAuthService authService;
    private final ExecutorService executor;

    public AuthenticationCoordinator(FirebaseAuthService authService) {
        this.authService = Objects.requireNonNull(
                authService, "Authentication service is required.");

        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(
                    task, "pc-doqtar-authentication");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Must be called on the JavaFX Application Thread because
     * the login dialog is a JavaFX component.
     *
     * The dialog opens immediately. Firebase authentication then
     * runs asynchronously on the coordinator's worker thread.
     */
    public CompletableFuture<AuthenticationResult> authenticate(
            Window owner) {

        final Optional<LoginDialog.Credentials> credentials;

        try {
            credentials = LoginDialog.show(owner);
        } catch (RuntimeException exception) {
            return CompletableFuture.completedFuture(
                    AuthenticationResult.failure(
                            "Unable to open the sign-in dialog."));
        }

        if (credentials.isEmpty()) {
            return CompletableFuture.completedFuture(
                    AuthenticationResult.cancelledResult());
        }

        LoginDialog.Credentials entered = credentials.get();

        if (entered.email() == null
                || entered.email().isBlank()
                || entered.password() == null
                || entered.password().isBlank()) {

            return CompletableFuture.completedFuture(
                    AuthenticationResult.failure(
                            "Enter both your email address and password."));
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (entered.register()) {
                    authService.signUp(
                            entered.email().trim(),
                            entered.password());
                } else {
                    authService.signIn(
                            entered.email().trim(),
                            entered.password());
                }

                return AuthenticationResult.success(
                        authServiceSessionEmail());

            } catch (IOException exception) {
                return AuthenticationResult.failure(
                        safeMessage(exception));

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                return AuthenticationResult.failure(
                        "Sign-in was interrupted.");

            } catch (RuntimeException exception) {
                return AuthenticationResult.failure(
                        "Sign-in could not be completed.");
            }
        }, executor);
    }

    private String authServiceSessionEmail() {
        // FirebaseAuthService owns the session and confirms sign-in
        // by returning normally. Keep the coordinator independent
        // of the session's internal token fields.
        return "Signed in successfully.";
    }

    private String safeMessage(IOException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return "Unable to sign in. Check your connection and try again.";
        }

        return message;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    public record AuthenticationResult(
            boolean authenticated,
            boolean cancelled,
            String message) {

        public static AuthenticationResult success(String message) {
            return new AuthenticationResult(true, false, message);
        }

        public static AuthenticationResult cancelledResult() {
            return new AuthenticationResult(false, true, "Sign-in cancelled.");
        }

        public static AuthenticationResult failure(String message) {
            return new AuthenticationResult(false, false, message);
        }
    }
}
