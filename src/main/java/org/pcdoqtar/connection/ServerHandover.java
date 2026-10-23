
package org.pcdoqtar.connection;

import java.io.IOException;
import java.util.Objects;

import org.pcdoqtar.auth.FirebaseAuthService;

public final class ServerHandover {

    private final FirebaseAuthService authService;

    public ServerHandover(FirebaseAuthService authService) {
        this.authService = Objects.requireNonNull(
                authService, "Authentication service is required.");
    }

    public HandoverResult performHandover()
            throws IOException, InterruptedException {

        String idToken = authService.getValidIdToken();

        if (idToken == null || idToken.isBlank()) {
            throw new IOException(
                    "Authentication is required before server handover.");
        }

        boolean sessionAvailable =
                ServerConnection.checkConnection(idToken);

        if (!sessionAvailable) {
            return new HandoverResult(false, null);
        }

        // Obtain a fresh token again in case the first request took
        // long enough for the token to approach expiry.
        idToken = authService.getValidIdToken();

        if (idToken == null || idToken.isBlank()) {
            throw new IOException(
                    "Authentication expired during server handover.");
        }

        String sessionId = ServerConnection.getSessionId(idToken);

        return new HandoverResult(true, sessionId);
    }

    public record HandoverResult(
            boolean sessionAvailable,
            String sessionId) {

        public HandoverResult {
            if (sessionAvailable
                    && (sessionId == null || sessionId.isBlank())) {
                throw new IllegalArgumentException(
                        "An available session must have a session ID.");
            }

            if (!sessionAvailable && sessionId != null) {
                throw new IllegalArgumentException(
                        "A missing session cannot have a session ID.");
            }
        }
    }
}
