package org.pcdoqtar.connection;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ServerConnection {

    /**
     * Override for local testing:
     *   set PC_DOQTAR_SERVER_URL=http://localhost:8080
     * or pass -Dpcdoqtar.server.url=http://localhost:8080
     */
    private static final String SERVER_URL = resolveServerUrl();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ServerConnection() {
        // Utility class.
    }

    public static boolean checkConnection(String idToken)
            throws IOException, InterruptedException {

        HttpRequest request = authenticatedRequest(
                "/sessions", idToken)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return Boolean.parseBoolean(response.body().trim());
        }

        throw createHttpException(
                "Checking server sessions", response);
    }

    public static String getSessionId(String idToken)
            throws IOException, InterruptedException {

        HttpRequest request = authenticatedRequest(
                "/getsession", idToken)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String sessionId = response.body().trim();

            if (sessionId.isBlank()) {
                throw new IOException(
                        "The server returned an empty session ID.");
            }

            return sessionId;
        }

        throw createHttpException(
                "Retrieving the session ID", response);
    }

    /** POSTs one diagnostic snapshot; returns the MySQL record id. */
    public static long uploadSnapshot(String idToken, String json)
            throws IOException, InterruptedException {

        HttpRequest request = authenticatedRequest(
                "/api/diagnostics/upload", idToken)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return MAPPER.readTree(response.body())
                    .path("recordId").asLong(-1L);
        }

        throw createHttpException(
                "Uploading diagnostics", response);
    }

    private static HttpRequest.Builder authenticatedRequest(
            String path,
            String idToken) {

        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException(
                    "A Firebase ID token is required.");
        }

        if (!path.startsWith("/") || path.contains("..")) {
            throw new IllegalArgumentException(
                    "Invalid server endpoint path.");
        }

        return HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + path))
                .header("Authorization", "Bearer " + idToken)
                .header("Accept", "application/json");
    }

    private static String resolveServerUrl() {
        String url = System.getProperty("pcdoqtar.server.url");

        if (url == null || url.isBlank()) {
            url = System.getenv("PC_DOQTAR_SERVER_URL");
        }

        if (url == null || url.isBlank()) {
            url = "https://pc-doqtar-connection-service.vercel.app";
        }

        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static IOException createHttpException(
            String operation,
            HttpResponse<String> response) {

        int status = response.statusCode();

        String reason = switch (status) {
            case 400 -> "The server rejected the diagnostic data.";
            case 401 -> "Authentication was rejected or has expired.";
            case 403 -> "The account is not authorised for this operation.";
            case 404 -> "The requested server endpoint was not found.";
            case 429 -> "The server rate limit was reached.";
            default -> "The server returned HTTP " + status + ".";
        };

        // Do not include raw response bodies, which may contain
        // implementation details or sensitive server information.
        return new IOException(operation + " failed. " + reason);
    }
}
