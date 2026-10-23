
package org.pcdoqtar.connection;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class ServerConnection {

    private static final String SERVER_URL =
            "https://pc-doqtar-connection-service.vercel.app";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

    private static IOException createHttpException(
            String operation,
            HttpResponse<String> response) {

        int status = response.statusCode();

        String reason = switch (status) {
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
