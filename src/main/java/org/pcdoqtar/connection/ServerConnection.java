package org.pcdoqtar.connection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import org.pcdoqtar.model.SystemHealthSnapshot;

public class ServerConnection {

    private static final String DEFAULT_BASE_URL = "https://pc-doqtar-connection-service.vercel.app";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ServerConnection() {
        this(resolveConfiguredBaseUrl());
    }

    public ServerConnection(String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    private static String resolveConfiguredBaseUrl() {
        String envUrl = System.getenv("PC_DOQTAR_SERVER_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            return envUrl.trim();
        }
        String propUrl = System.getProperty("pcdoqtar.server.url");
        if (propUrl != null && !propUrl.isBlank()) {
            return propUrl.trim();
        }
        return DEFAULT_BASE_URL;
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        String trimmed = url.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    /**
     * Checks server & database liveness via GET /sessions.
     */
    public boolean checkConnection(String idToken) throws IOException, InterruptedException {
        HttpRequest request = baseRequestBuilder("/sessions", idToken)
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String body = response.body() != null ? response.body().trim() : "";
            return "true".equalsIgnoreCase(body) || !body.isEmpty();
        }
        return false;
    }

    /**
     * Requests a new diagnostic session ID from GET /getsession.
     */
    public String getSessionId(String idToken) throws IOException, InterruptedException {
        HttpRequest request = baseRequestBuilder("/getsession", idToken)
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
            return response.body().replace("", "").trim();
        }
        throw new IOException("Failed to obtain session ID from server. HTTP status: " + response.statusCode());
    }

    /**
     * Serializes a SystemHealthSnapshot and uploads it to POST /api/diagnostics/upload.
     *
     * @param idToken   Firebase ID token (Bearer token)
     * @param sessionId Active session ID obtained during server handover
     * @param snapshot  Collected SystemHealthSnapshot from SystemDiagnostics
     * @return Parsed JsonNode containing saved recordId and historicalComparison
     */
    public JsonNode uploadDiagnostics(
            String idToken,
            String sessionId,
            SystemHealthSnapshot snapshot) throws IOException, InterruptedException {

        String payloadJson = buildUploadPayloadJson(sessionId, snapshot);

        HttpRequest request = baseRequestBuilder("/api/diagnostics/upload", idToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return objectMapper.readTree(response.body());
        }

        throw new IOException("Diagnostic upload failed with HTTP " + response.statusCode()
                + ": " + response.body());
    }

    /**
     * Fetches the user's latest stored diagnostic record and comparison from GET /api/diagnostics/latest.
     */
    public JsonNode fetchLatestDiagnostics(String idToken) throws IOException, InterruptedException {
        HttpRequest request = baseRequestBuilder("/api/diagnostics/latest", idToken)
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() == 204 || response.body() == null || response.body().isBlank()) {
            return null;
        }
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readTree(response.body());
        }
        throw new IOException("Failed to fetch latest diagnostics. HTTP status: " + response.statusCode());
    }

    /**
     * Fetches the user's diagnostic history from GET /api/diagnostics/history?limit=N.
     */
    public JsonNode fetchDiagnosticHistory(String idToken, int limit) throws IOException, InterruptedException {
        HttpRequest request = baseRequestBuilder("/api/diagnostics/history?limit=" + limit, idToken)
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readTree(response.body());
        }
        throw new IOException("Failed to fetch diagnostic history. HTTP status: " + response.statusCode());
    }

    /**
     * Converts any SystemHealthSnapshot into the JSON structure expected by
     * POST /api/diagnostics/upload while preserving the full raw snapshot JSON.
     */
    private String buildUploadPayloadJson(String sessionId, SystemHealthSnapshot snapshot)
            throws IOException {

        ObjectNode root = objectMapper.createObjectNode();
        root.put("sessionId", sessionId);
        root.put("hostname", resolveHostname());
        root.put("osInfo", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        root.put("capturedAt", Instant.now().toString());

        if (snapshot != null) {
            String rawJson = objectMapper.writeValueAsString(snapshot);
            root.put("rawSnapshotJson", rawJson);

            JsonNode snapshotTree = objectMapper.readTree(rawJson);

            Double cpuUsage = findNumericMetric(snapshotTree,
                    "cpuUsagePercent", "cpuLoad", "systemCpuLoad", "cpuUsage", "loadPercent");
            if (cpuUsage != null) {
                root.put("cpuUsagePercent", clampPercent(cpuUsage));
            }

            Double memUsage = findNumericMetric(snapshotTree,
                    "memoryUsagePercent", "ramUsagePercent", "memoryLoad", "usedPercent");
            if (memUsage != null) {
                root.put("memoryUsagePercent", clampPercent(memUsage));
            }

            Long memUsed = findLongMetric(snapshotTree, "memoryUsedBytes", "usedMemory", "usedBytes");
            if (memUsed != null) {
                root.put("memoryUsedBytes", memUsed);
            }

            Long memTotal = findLongMetric(snapshotTree, "memoryTotalBytes", "totalMemory", "totalBytes");
            if (memTotal != null) {
                root.put("memoryTotalBytes", memTotal);
            }

            Double storageUsage = findNumericMetric(snapshotTree,
                    "storageUsagePercent", "diskUsagePercent", "storageLoad");
            if (storageUsage != null) {
                root.put("storageUsagePercent", clampPercent(storageUsage));
            }

            Boolean netConnected = findBooleanMetric(snapshotTree,
                    "networkConnected", "connected", "internetAvailable", "online");
            if (netConnected != null) {
                root.put("networkConnected", netConnected);
            }

            Integer processCount = findIntMetric(snapshotTree,
                    "runningProcessCount", "processCount", "totalProcesses");
            if (processCount != null) {
                root.put("runningProcessCount", processCount);
            }

            String overallStatus = findStringMetric(snapshotTree,
                    "overallHealthStatus", "overallStatus", "healthStatus", "status");
            if (overallStatus != null && !overallStatus.isBlank()) {
                root.put("overallHealthStatus", overallStatus);
            }
        }

        return objectMapper.writeValueAsString(root);
    }

    private HttpRequest.Builder baseRequestBuilder(String path, String idToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT);

        if (idToken != null && !idToken.isBlank()) {
            builder.header("Authorization", "Bearer " + idToken.trim());
        }
        return builder;
    }

    private String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            String envName = System.getenv("COMPUTERNAME");
            return (envName != null && !envName.isBlank()) ? envName : "unknown-pc";
        }
    }

    private double clampPercent(double val) {
        return Math.max(0.0, Math.min(100.0, val));
    }

    private Double findNumericMetric(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) return null;
        for (String name : fieldNames) {
            JsonNode match = findFieldRecursive(node, name);
            if (match != null && match.isNumber()) {
                return match.doubleValue();
            }
        }
        return null;
    }

    private Long findLongMetric(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) return null;
        for (String name : fieldNames) {
            JsonNode match = findFieldRecursive(node, name);
            if (match != null && match.canConvertToLong()) {
                return match.longValue();
            }
        }
        return null;
    }

    private Integer findIntMetric(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) return null;
        for (String name : fieldNames) {
            JsonNode match = findFieldRecursive(node, name);
            if (match != null && match.canConvertToInt()) {
                return match.intValue();
            }
        }
        return null;
    }

    private Boolean findBooleanMetric(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) return null;
        for (String name : fieldNames) {
            JsonNode match = findFieldRecursive(node, name);
            if (match != null && match.isBoolean()) {
                return match.booleanValue();
            }
        }
        return null;
    }

    private String findStringMetric(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) return null;
        for (String name : fieldNames) {
            JsonNode match = findFieldRecursive(node, name);
            if (match != null && match.isTextual()) {
                return match.asText();
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
	private JsonNode findFieldRecursive(JsonNode current, String targetField) {
        if (current == null || current.isNull()) {
            return null;
        }
        if (current.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = current.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (entry.getKey().equalsIgnoreCase(targetField)) {
                    return entry.getValue();
                }
            }
            fields = current.fields();
            while (fields.hasNext()) {
                JsonNode nested = findFieldRecursive(fields.next().getValue(), targetField);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}