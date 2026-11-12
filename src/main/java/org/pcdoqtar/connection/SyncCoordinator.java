package org.pcdoqtar.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.pcdoqtar.auth.AuthSession;
import org.pcdoqtar.auth.FirebaseAuthService;
import org.pcdoqtar.connection.ServerHandover.UploadOutcome;
import org.pcdoqtar.diagnostics.DiagnosticTaskManager;
import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.HealthStatus;
import org.pcdoqtar.model.SystemHealthSnapshot;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Coordinates cloud synchronisation with robust retry logic, offline detection,
 * duplicate prevention (single-flight), and local disk persistence.
 */
public final class SyncCoordinator implements AutoCloseable {

    public enum SyncStatus {
        UNAUTHENTICATED("Sign in to sync"),
        SYNCING("Syncing…"),
        SYNCED("Synced \u2713"),
        SYNC_FAILED("Sync failed"),
        OFFLINE("Offline");

        private final String displayStatus;

        SyncStatus(String displayStatus) {
            this.displayStatus = displayStatus;
        }

        public String getDisplayStatus() {
            return displayStatus;
        }
    }

    public record PendingRecord(
            String id,
            long createdAtMillis,
            SystemHealthSnapshot snapshot,
            Path filePath) {
    }

    private static final int[] BACKOFF_INTERVALS = {5, 10, 20, 30, 60};
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AuthSession session;
    private final FirebaseAuthService authService;
    private final DiagnosticTaskManager taskManager;
    private final ServerHandover handover;
    private final Path queueDirectory;
    private final ScheduledExecutorService scheduler;
    private final BooleanSupplier internetChecker;

    private final List<Consumer<SyncStatus>> statusListeners = new CopyOnWriteArrayList<>();
    private final List<PendingRecord> inMemoryQueue = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean uploading = new AtomicBoolean(false);

    private volatile SyncStatus currentStatus = SyncStatus.UNAUTHENTICATED;
    private volatile int retryCount = 0;
    private volatile ScheduledFuture<?> nextRetryTask;
    private volatile CompletableFuture<UploadOutcome> activeFlight;
    private volatile boolean closed = false;

    public SyncCoordinator(
            AuthSession session,
            FirebaseAuthService authService,
            DiagnosticTaskManager taskManager,
            ServerHandover handover) {
        this(session, authService, taskManager, handover, resolveDefaultQueueDirectory(),
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "pc-doqtar-sync-scheduler");
                    t.setDaemon(true);
                    return t;
                }),
                SyncCoordinator::probeInternetConnectivity);
    }

    public SyncCoordinator(
            AuthSession session,
            FirebaseAuthService authService,
            DiagnosticTaskManager taskManager,
            ServerHandover handover,
            Path queueDirectory,
            ScheduledExecutorService scheduler,
            BooleanSupplier internetChecker) {

        this.session = Objects.requireNonNull(session, "session is required");
        this.authService = Objects.requireNonNull(authService, "authService is required");
        this.taskManager = Objects.requireNonNull(taskManager, "taskManager is required");
        this.handover = Objects.requireNonNull(handover, "handover is required");
        this.queueDirectory = Objects.requireNonNull(queueDirectory, "queueDirectory is required");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler is required");
        this.internetChecker = Objects.requireNonNull(internetChecker, "internetChecker is required");

        ensureQueueDirectory();
        loadPendingRecordsFromDisk();

        // Initial status check
        if (!session.isLoggedIn()) {
            setStatus(SyncStatus.UNAUTHENTICATED);
        } else if (!isInternetAvailable()) {
            setStatus(SyncStatus.OFFLINE);
        } else if (!inMemoryQueue.isEmpty()) {
            setStatus(SyncStatus.SYNC_FAILED);
        }
    }

    public void addStatusListener(Consumer<SyncStatus> listener) {
        if (listener != null) {
            statusListeners.add(listener);
            listener.accept(currentStatus);
        }
    }

    public void removeStatusListener(Consumer<SyncStatus> listener) {
        statusListeners.remove(listener);
    }

    public SyncStatus getCurrentStatus() {
        return currentStatus;
    }

    public int getPendingRecordCount() {
        return inMemoryQueue.size();
    }

    public boolean isInternetAvailable() {
        try {
            return internetChecker.getAsBoolean();
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Triggers synchronisation. If a sync is already running, returns the in-flight
     * future to prevent duplicate simultaneous uploads.
     */
    public synchronized CompletableFuture<UploadOutcome> syncNow() {
        if (closed) {
            return CompletableFuture.completedFuture(UploadOutcome.failed("Sync coordinator is closed."));
        }

        if (!session.isLoggedIn()) {
            setStatus(SyncStatus.UNAUTHENTICATED);
            return CompletableFuture.completedFuture(
                    UploadOutcome.failed("Sign in to sync your diagnostics."));
        }

        // Single-flight duplicate prevention
        if (activeFlight != null && !activeFlight.isDone()) {
            return activeFlight;
        }

        cancelScheduledRetry();

        CompletableFuture<UploadOutcome> flight = new CompletableFuture<>();
        activeFlight = flight;

        // If no pending records, collect one first then upload
        if (inMemoryQueue.isEmpty()) {
            setStatus(SyncStatus.SYNCING);
            taskManager.collectSnapshot()
                    .whenComplete((snapshot, error) -> {
                        if (error != null || snapshot == null) {
                            setStatus(SyncStatus.SYNC_FAILED);
                            activeFlight = null;
                            flight.complete(UploadOutcome.failed("Could not collect diagnostics."));
                        } else {
                            enqueueRecord(snapshot);
                            executePendingUploads(flight);
                        }
                    });
        } else {
            setStatus(SyncStatus.SYNCING);
            executePendingUploads(flight);
        }

        return flight;
    }

    private void executePendingUploads(CompletableFuture<UploadOutcome> completionFuture) {
        scheduler.execute(() -> {
            if (closed) {
                activeFlight = null;
                completionFuture.complete(UploadOutcome.failed("Coordinator closed."));
                return;
            }

            if (!uploading.compareAndSet(false, true)) {
                // Another thread is processing the queue
                return;
            }

            try {
                processQueue(completionFuture);
            } finally {
                uploading.set(false);
            }
        });
    }

    private void processQueue(CompletableFuture<UploadOutcome> completionFuture) {
        if (!session.isLoggedIn()) {
            setStatus(SyncStatus.UNAUTHENTICATED);
            activeFlight = null;
            completionFuture.complete(UploadOutcome.failed("User is not signed in."));
            return;
        }

        // Check internet connectivity
        if (!isInternetAvailable()) {
            setStatus(SyncStatus.OFFLINE);
            activeFlight = null;
            completionFuture.complete(UploadOutcome.failed("Device is offline.", -1, -1));
            return;
        }

        if (inMemoryQueue.isEmpty()) {
            setStatus(SyncStatus.SYNCED);
            activeFlight = null;
            completionFuture.complete(UploadOutcome.saved(0L));
            return;
        }

        PendingRecord record = inMemoryQueue.get(0);
        setStatus(SyncStatus.SYNCING);

        UploadOutcome outcome = handover.uploadSnapshot(record.snapshot());

        if (outcome.success()) {
            // Upload confirmed by server!
            deletePendingRecord(record);
            retryCount = 0;
            setStatus(SyncStatus.SYNCED);

            // If more records are pending, continue processing
            if (!inMemoryQueue.isEmpty()) {
                processQueue(completionFuture);
            } else {
                activeFlight = null;
                completionFuture.complete(outcome);
            }
        } else {
            handleUploadFailure(outcome, completionFuture);
        }
    }

    private void handleUploadFailure(UploadOutcome outcome, CompletableFuture<UploadOutcome> completionFuture) {
        activeFlight = null;
        int status = outcome.httpStatusCode();

        // 1. Internet lost during attempt
        if (!isInternetAvailable()) {
            setStatus(SyncStatus.OFFLINE);
            completionFuture.complete(outcome);
            return;
        }

        // 2. HTTP 400: Bad Request / Data corruption -> quarantine to avoid endless loop
        if (status == 400) {
            System.err.println("[PC DOQTAR] HTTP 400: Discarding corrupted pending record: " + outcome.message());
            if (!inMemoryQueue.isEmpty()) {
                deletePendingRecord(inMemoryQueue.get(0));
            }
            setStatus(SyncStatus.SYNC_FAILED);
            completionFuture.complete(outcome);
            return;
        }

        // 3. HTTP 401: Unauthorized -> attempt token refresh
        if (status == 401) {
            try {
                String refreshed = authService.getValidIdToken();
                if (refreshed == null || refreshed.isBlank()) {
                    System.err.println("[PC DOQTAR] HTTP 401: Session expired and refresh failed.");
                    setStatus(SyncStatus.SYNC_FAILED);
                    completionFuture.complete(outcome);
                    return;
                }
            } catch (Exception e) {
                setStatus(SyncStatus.SYNC_FAILED);
                completionFuture.complete(outcome);
                return;
            }
        }

        // 4. HTTP 403: Forbidden -> authentication or permission issue, avoid blind retry
        if (status == 403) {
            System.err.println("[PC DOQTAR] HTTP 403: Forbidden. Halting retries.");
            setStatus(SyncStatus.SYNC_FAILED);
            completionFuture.complete(outcome);
            return;
        }

        // 5. HTTP 429: Rate limited -> respect Retry-After header if provided
        long delaySeconds;
        if (status == 429 && outcome.retryAfterSeconds() > 0) {
            delaySeconds = outcome.retryAfterSeconds();
        } else {
            delaySeconds = calculateBackoffDelay(retryCount);
        }

        retryCount++;
        setStatus(SyncStatus.SYNC_FAILED);
        completionFuture.complete(outcome);

        scheduleRetry(delaySeconds);
    }

    public long calculateBackoffDelay(int count) {
        int index = Math.min(count, BACKOFF_INTERVALS.length - 1);
        int base = BACKOFF_INTERVALS[index];
        int jitter = ThreadLocalRandom.current().nextInt(0, 3);
        return Math.min(base + jitter, 60);
    }

    private synchronized void scheduleRetry(long delaySeconds) {
        if (closed) return;
        cancelScheduledRetry();
        System.err.println("[PC DOQTAR] Scheduling sync retry #" + retryCount + " in " + delaySeconds + " seconds.");
        nextRetryTask = scheduler.schedule(() -> {
            if (!closed && session.isLoggedIn()) {
                syncNow();
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private synchronized void cancelScheduledRetry() {
        if (nextRetryTask != null && !nextRetryTask.isDone()) {
            nextRetryTask.cancel(false);
            nextRetryTask = null;
        }
    }

    private void enqueueRecord(SystemHealthSnapshot snapshot) {
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        Path filePath = queueDirectory.resolve("pending-" + now + "-" + id + ".json");

        com.fasterxml.jackson.databind.node.ObjectNode node = MAPPER.createObjectNode();
        node.put("id", id);
        node.put("createdAt", now);
        node.put("timestamp", snapshot.getTimestamp() != null
                ? snapshot.getTimestamp().toString()
                : java.time.LocalDateTime.now().toString());
        node.put("cpuValue", snapshot.getCpu() != null ? snapshot.getCpu().getValue() : 0.0);
        node.put("cpuUnit", snapshot.getCpu() != null ? snapshot.getCpu().getUnit() : "%");
        node.put("memoryValue", snapshot.getMemory() != null ? snapshot.getMemory().getValue() : 0.0);
        node.put("memoryUnit", snapshot.getMemory() != null ? snapshot.getMemory().getUnit() : "%");

        Path temp = null;
        try {
            temp = Files.createTempFile(queueDirectory, "sync-", ".tmp");
            Files.writeString(temp, node.toString(), StandardCharsets.UTF_8);
            Files.move(temp, filePath, StandardCopyOption.ATOMIC_MOVE);
            temp = null;
        } catch (IOException e) {
            System.err.println("[PC DOQTAR] Warning: Failed to persist pending record to disk: " + e.getMessage());
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
            }
        }

        inMemoryQueue.add(new PendingRecord(id, now, snapshot, filePath));
    }

    private void deletePendingRecord(PendingRecord record) {
        inMemoryQueue.remove(record);
        if (record.filePath() != null) {
            try {
                Files.deleteIfExists(record.filePath());
            } catch (IOException e) {
                System.err.println("[PC DOQTAR] Warning: Could not delete synced record file: " + e.getMessage());
            }
        }
    }

    private void loadPendingRecordsFromDisk() {
        if (!Files.isDirectory(queueDirectory)) return;

        try (Stream<Path> stream = Files.list(queueDirectory)) {
            List<Path> files = stream
                    .filter(p -> p.getFileName().toString().startsWith("pending-")
                            && p.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .toList();

            for (Path file : files) {
                try {
                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    com.fasterxml.jackson.databind.JsonNode node = MAPPER.readTree(content);
                    String id = node.path("id").asText(file.getFileName().toString());
                    long createdAt = node.path("createdAt").asLong(Files.getLastModifiedTime(file).toMillis());
                    String tsStr = node.path("timestamp").asText();
                    java.time.LocalDateTime ts;
                    try {
                        ts = java.time.LocalDateTime.parse(tsStr);
                    } catch (Exception ex) {
                        ts = java.time.LocalDateTime.now();
                    }
                    DiagnosticResult cpu = new DiagnosticResult(
                            "CPU",
                            node.path("cpuValue").asDouble(0.0),
                            node.path("cpuUnit").asText("%"),
                            HealthStatus.NORMAL,
                            "OK");
                    DiagnosticResult mem = new DiagnosticResult(
                            "Memory",
                            node.path("memoryValue").asDouble(0.0),
                            node.path("memoryUnit").asText("%"),
                            HealthStatus.NORMAL,
                            "OK");
                    SystemHealthSnapshot snapshot = new SystemHealthSnapshot(ts, cpu, mem, List.of());
                    inMemoryQueue.add(new PendingRecord(id, createdAt, snapshot, file));
                } catch (Exception e) {
                    System.err.println("[PC DOQTAR] Skipping invalid pending record file " + file + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[PC DOQTAR] Error loading pending sync records from disk: " + e.getMessage());
        }
    }

    private void ensureQueueDirectory() {
        try {
            Files.createDirectories(queueDirectory);
        } catch (IOException e) {
            System.err.println("[PC DOQTAR] Could not create pending sync directory: " + e.getMessage());
        }
    }

    private void setStatus(SyncStatus status) {
        this.currentStatus = status;
        for (Consumer<SyncStatus> listener : statusListeners) {
            try {
                listener.accept(status);
            } catch (Exception ignored) {
            }
        }
    }

    public static Path resolveDefaultQueueDirectory() {
        String programData = System.getenv("PROGRAMDATA");
        if (programData != null && !programData.isBlank()) {
            Path path = Path.of(programData, "PCDOQTAR", "pending-sync");
            try {
                Files.createDirectories(path);
                return path;
            } catch (Exception ignored) {
            }
        }

        String userHome = System.getProperty("user.home");
        Path userPath = Path.of(userHome, ".pcdoqtar", "pending-sync");
        try {
            Files.createDirectories(userPath);
        } catch (Exception ignored) {
        }
        return userPath;
    }

    public static boolean probeInternetConnectivity() {
        // Probe server host first
        String serverUrl = ServerConnection.resolveServerUrl();
        try {
            URI uri = URI.create(serverUrl);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : (uri.getScheme().equalsIgnoreCase("https") ? 443 : 80);
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 2500);
                return true;
            }
        } catch (Exception ignored) {
        }

        // Secondary fallback to reliable public DNS resolvers
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 53), 2000);
            return true;
        } catch (Exception e1) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("1.1.1.1", 53), 2000);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        cancelScheduledRetry();
        scheduler.shutdownNow();
    }
}
