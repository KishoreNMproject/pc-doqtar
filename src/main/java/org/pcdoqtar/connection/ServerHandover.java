package org.pcdoqtar.connection;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import org.pcdoqtar.auth.FirebaseAuthService;
import org.pcdoqtar.model.SystemHealthSnapshot;

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

        String sessionId;
        try {
            sessionId = ServerConnection.getSessionId(idToken);
        } catch (IOException noServerSessionEndpoint) {
            // Older server builds have no /getsession route:
            // fall back to a client-generated session id.
            sessionId = UUID.randomUUID().toString();
        }

        return new HandoverResult(true, sessionId);
    }

    /** Handover + upload one snapshot. Never throws for expected failures. */
    public UploadOutcome uploadSnapshot(SystemHealthSnapshot snapshot) {
        try {
            HandoverResult handover = performHandover();

            if (!handover.sessionAvailable()) {
                return UploadOutcome.failed(
                        "The server database is not available.");
            }

            String idToken = authService.getValidIdToken();

            if (idToken == null || idToken.isBlank()) {
                return UploadOutcome.failed(
                        "Authentication expired. Please sign in again.");
            }

            long recordId = ServerConnection.uploadSnapshot(
                    idToken,
                    SnapshotMapper.toJson(handover.sessionId(), snapshot));

            return UploadOutcome.saved(recordId);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return UploadOutcome.failed("Upload was interrupted.");

        } catch (IOException | RuntimeException exception) {
            String message = exception.getMessage();
            return UploadOutcome.failed(
                    message == null || message.isBlank()
                            ? "Unable to reach the server."
                            : message);
        }
    }

    public record UploadOutcome(
            boolean success,
            long recordId,
            String message) {

        static UploadOutcome saved(long recordId) {
            return new UploadOutcome(true, recordId, "Saved to server.");
        }

        static UploadOutcome failed(String message) {
            return new UploadOutcome(false, -1L, message);
        }
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
