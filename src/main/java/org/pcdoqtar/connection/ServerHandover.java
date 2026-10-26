package org.pcdoqtar.connection;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.pcdoqtar.model.SystemHealthSnapshot;

public class ServerHandover {

    private final ServerConnection serverConnection;

    public ServerHandover() {
        this(new ServerConnection());
    }

    public ServerHandover(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
    }

    /**
     * Executes the initial server connection check and obtains a session ID.
     */
    public HandoverResult performHandover(String idToken) {
        try {
            boolean connected = serverConnection.checkConnection(idToken);
            if (!connected) {
                return new HandoverResult(false, null, "Server database connection check returned false.");
            }
            String sessionId = serverConnection.getSessionId(idToken);
            return new HandoverResult(true, sessionId, "Connected to PC DOQTAR server.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new HandoverResult(false, null, "Server handover interrupted: " + e.getMessage());
        } catch (IOException e) {
            return new HandoverResult(false, null, "Server handover failed: " + e.getMessage());
        }
    }

    /**
     * Performs handover (if sessionId is not yet provided) and uploads the collected
     * SystemHealthSnapshot to POST /api/diagnostics/upload.
     */
    public UploadOutcome uploadSnapshot(
            String idToken,
            String sessionId,
            SystemHealthSnapshot snapshot) {

        try {
            String activeSessionId = sessionId;
            if (activeSessionId == null || activeSessionId.isBlank()) {
                HandoverResult handover = performHandover(idToken);
                if (!handover.connected()) {
                    return new UploadOutcome(false, null, null, null, handover.message());
                }
                activeSessionId = handover.sessionId();
            }

            JsonNode responseNode =
                    serverConnection.uploadDiagnostics(idToken, activeSessionId, snapshot);

            Long recordId = responseNode.hasNonNull("recordId")
                    ? responseNode.get("recordId").asLong()
                    : null;
            String trendAssessment = null;
            if (responseNode.hasNonNull("historicalComparison")) {
                JsonNode comp = responseNode.get("historicalComparison");
                if (comp.hasNonNull("trendAssessment")) {
                    trendAssessment = comp.get("trendAssessment").asText();
                }
            }

            return new UploadOutcome(
                    true,
                    recordId,
                    activeSessionId,
                    trendAssessment,
                    "Diagnostic snapshot uploaded and persisted successfully."
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new UploadOutcome(false, null, sessionId, null, "Upload interrupted: " + e.getMessage());
        } catch (IOException e) {
            return new UploadOutcome(false, null, sessionId, null, "Upload failed: " + e.getMessage());
        }
    }

    public record HandoverResult(
            boolean connected,
            String sessionId,
            String message
    ) {
    }

    public record UploadOutcome(
            boolean success,
            Long recordId,
            String sessionId,
            String trendAssessment,
            String message
    ) {
    }
}