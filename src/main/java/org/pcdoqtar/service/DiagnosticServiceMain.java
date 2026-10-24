
package org.pcdoqtar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.pcdoqtar.diagnostics.SystemDiagnostics;
import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.SystemHealthSnapshot;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DiagnosticServiceMain {

    private static final long INTERVAL_MILLIS = 5L * 60L * 1000L;

    private static final AtomicBoolean RUNNING = new AtomicBoolean(true);

    private static final ObjectMapper JSON =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss.SSSSSSSSS");

    private DiagnosticServiceMain() {
    }

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();
        mainThread.setName("PC-DOQTAR-Diagnostic-Service");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            RUNNING.set(false);
            mainThread.interrupt();
        }, "PC-DOQTAR-Service-Shutdown"));

        Path dataDirectory = resolveDataDirectory();

        try {
            Files.createDirectories(dataDirectory);
            Files.createDirectories(dataDirectory.resolve("history"));
            Files.createDirectories(dataDirectory.resolve("logs"));
        } catch (IOException exception) {
            System.err.println("Cannot create diagnostic directories: "
                    + exception.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("PC DOQTAR diagnostic service starting.");
        System.out.println("Data directory: " + dataDirectory);

        SystemDiagnostics diagnostics = new SystemDiagnostics();

        while (RUNNING.get()) {
            try {
                SystemHealthSnapshot snapshot = diagnostics.collectSnapshot();
                Map<String, Object> payload = createPayload(snapshot);

                persistSnapshot(dataDirectory, payload, snapshot);

                System.out.println("Snapshot persisted: "
                        + snapshot.getTimestamp());
            } catch (Exception exception) {
                System.err.println("Diagnostic cycle failed: "
                        + exception.getMessage());
                exception.printStackTrace(System.err);
            }

            if (!RUNNING.get()) {
                break;
            }

            try {
                Thread.sleep(INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                RUNNING.set(false);
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("PC DOQTAR diagnostic service stopped.");
    }

    private static Path resolveDataDirectory() {
        String programData = System.getenv("PROGRAMDATA");

        if (programData == null || programData.isBlank()) {
            throw new IllegalStateException(
                    "PROGRAMDATA environment variable is unavailable.");
        }

        return Path.of(programData, "PCDOQTAR");
    }

    private static Map<String, Object> createPayload(
            SystemHealthSnapshot snapshot) {

        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("schemaVersion", 1);
        payload.put("snapshotId", UUID.randomUUID().toString());
        payload.put("application", "PC DOQTAR");
        payload.put("recordType", "diagnostic_snapshot");
        payload.put("timestamp", snapshot.getTimestamp().toString());
        payload.put("overallStatus", snapshot.getOverallStatus().name());
        payload.put("warningCount", snapshot.getWarningCount());
        payload.put("containsUnavailableResults",
                snapshot.hasUnavailableResults());

        payload.put("cpu", resultToMap(snapshot.getCpu()));
        payload.put("memory", resultToMap(snapshot.getMemory()));

        payload.put("storage", snapshot.getStorage()
                .stream()
                .map(DiagnosticServiceMain::resultToMap)
                .toList());

        return payload;
    }

    private static Map<String, Object> resultToMap(
            DiagnosticResult result) {

        Map<String, Object> item = new LinkedHashMap<>();

        item.put("metric", result.getMetric());
        item.put("value", result.getValue());
        item.put("unit", result.getUnit());
        item.put("status", result.getStatus().name());
        item.put("message", result.getMessage());

        return item;
    }

    private static void persistSnapshot(
            Path dataDirectory,
            Map<String, Object> payload,
            SystemHealthSnapshot snapshot) throws IOException {

        String timestamp = snapshot.getTimestamp().format(FILE_TIME);
        String snapshotId = (String) payload.get("snapshotId");

        Path dayDirectory = dataDirectory.resolve("history")
                .resolve(LocalDate.now().toString());

        Files.createDirectories(dayDirectory);

        Path historicalRecord = dayDirectory.resolve(
                "snapshot-" + timestamp + "-" + snapshotId + ".json");

        Path latestRecord = dataDirectory.resolve("latest-diagnostics.json");

        // Historical records are created once and never replaced.
        writeNewRecordAtomically(historicalRecord, payload);

        // This convenience file always contains the newest snapshot.
        writeReplaceAtomically(latestRecord, payload);
    }

    private static void writeNewRecordAtomically(
            Path destination,
            Map<String, Object> payload) throws IOException {

        Path temporary = Files.createTempFile(
                destination.getParent(), "snapshot-", ".tmp");

        boolean moved = false;

        try {
            JSON.writeValue(temporary.toFile(), payload);

            // No REPLACE_EXISTING: an existing historical record is protected.
            try {
                Files.move(temporary, destination,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination);
            }

            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static void writeReplaceAtomically(
            Path destination,
            Map<String, Object> payload) throws IOException {

        Path temporary = Files.createTempFile(
                destination.getParent(), "latest-", ".tmp");

        boolean moved = false;

        try {
            JSON.writeValue(temporary.toFile(), payload);

            try {
                Files.move(temporary, destination,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination,
                        StandardCopyOption.REPLACE_EXISTING);
            }

            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
