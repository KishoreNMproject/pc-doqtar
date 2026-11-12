package org.pcdoqtar.connection;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pcdoqtar.auth.AuthSession;
import org.pcdoqtar.auth.FirebaseAuthService;
import org.pcdoqtar.connection.ServerHandover.UploadOutcome;
import org.pcdoqtar.connection.SyncCoordinator.SyncStatus;
import org.pcdoqtar.diagnostics.DiagnosticTaskManager;
import org.pcdoqtar.diagnostics.SystemDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics;
import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.HealthStatus;
import org.pcdoqtar.model.SystemHealthSnapshot;

class SyncCoordinatorTest {

    @TempDir
    Path tempDir;

    private AuthSession session;
    private FirebaseAuthService authService;
    private DiagnosticTaskManager taskManager;
    private ScheduledExecutorService scheduler;
    private AtomicBoolean mockInternetOnline;
    private AtomicInteger uploadCallCount;
    private volatile UploadOutcome mockUploadOutcome;

    private SystemHealthSnapshot createSampleSnapshot() {
        return new SystemHealthSnapshot(
                java.time.LocalDateTime.now(),
                new DiagnosticResult("CPU", 15.0, "%", HealthStatus.NORMAL, "OK"),
                new DiagnosticResult("Memory", 40.0, "%", HealthStatus.NORMAL, "OK"),
                java.util.List.of());
    }

    @BeforeEach
    void setUp() {
        session = new AuthSession();
        session.setTokens("u1", "user@pcdoqtar.health", "token_val", "ref_val", 3600);

        authService = new FirebaseAuthService(session);
        taskManager = new DiagnosticTaskManager(new SystemDiagnostics(), new NetworkDiagnostics()) {
            @Override
            public CompletableFuture<SystemHealthSnapshot> collectSnapshot() {
                return CompletableFuture.completedFuture(createSampleSnapshot());
            }
        };

        scheduler = Executors.newSingleThreadScheduledExecutor();
        mockInternetOnline = new AtomicBoolean(true);
        uploadCallCount = new AtomicInteger(0);
        mockUploadOutcome = UploadOutcome.saved(101L);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdownNow();
    }

    private ServerHandover createMockHandover() {
        return new ServerHandover(authService) {
            @Override
            public UploadOutcome uploadSnapshot(SystemHealthSnapshot snapshot) {
                uploadCallCount.incrementAndGet();
                return mockUploadOutcome;
            }
        };
    }

    private SyncCoordinator createCoordinator(ServerHandover handover) {
        return new SyncCoordinator(
                session,
                authService,
                taskManager,
                handover,
                tempDir,
                scheduler,
                mockInternetOnline::get);
    }

    @Test
    @DisplayName("Successful upload transitions state to Synced and removes pending records from disk")
    void testSuccessfulUploadTransition() throws Exception {
        mockUploadOutcome = UploadOutcome.saved(42L);
        ServerHandover handover = createMockHandover();

        try (SyncCoordinator coordinator = createCoordinator(handover)) {
            UploadOutcome outcome = coordinator.syncNow().get(5, TimeUnit.SECONDS);

            assertTrue(outcome.success());
            assertEquals(SyncStatus.SYNCED, coordinator.getCurrentStatus());
            assertEquals(0, coordinator.getPendingRecordCount(),
                    "Pending queue must be empty after confirmed success.");

            // Verify disk directory has no remaining pending files
            try (var stream = Files.list(tempDir)) {
                assertEquals(0, stream.count(), "All pending files must be deleted from disk on success.");
            }
        }
    }

    @Test
    @DisplayName("Offline state pauses retries and sets status to Offline without discarding pending records")
    void testOfflineSyncPause() throws Exception {
        mockInternetOnline.set(false); // Simulate offline
        ServerHandover handover = createMockHandover();

        try (SyncCoordinator coordinator = createCoordinator(handover)) {
            UploadOutcome outcome = coordinator.syncNow().get(5, TimeUnit.SECONDS);

            assertFalse(outcome.success());
            assertEquals(SyncStatus.OFFLINE, coordinator.getCurrentStatus());
            assertEquals(0, uploadCallCount.get(), "No upload attempts should be made while offline.");
            assertEquals(1, coordinator.getPendingRecordCount(), "Pending record must be preserved locally.");
        }
    }

    @Test
    @DisplayName("Retry triggered on request timeout while online")
    void testRetryOnRequestTimeout() throws Exception {
        mockUploadOutcome = UploadOutcome.failed("Request timed out", 408, -1);
        ServerHandover handover = createMockHandover();

        try (SyncCoordinator coordinator = createCoordinator(handover)) {
            UploadOutcome outcome = coordinator.syncNow().get(5, TimeUnit.SECONDS);

            assertFalse(outcome.success());
            assertEquals(SyncStatus.SYNC_FAILED, coordinator.getCurrentStatus());
            assertEquals(1, coordinator.getPendingRecordCount(), "Pending record must be preserved for retry.");
            assertTrue(coordinator.calculateBackoffDelay(0) >= 5, "First retry backoff must be at least 5s");
        }
    }

    @Test
    @DisplayName("Retry triggered on HTTP 5xx error while online")
    void testRetryOnHttp5xx() throws Exception {
        mockUploadOutcome = UploadOutcome.failed("Internal Server Error", 503, -1);
        ServerHandover handover = createMockHandover();

        try (SyncCoordinator coordinator = createCoordinator(handover)) {
            UploadOutcome outcome = coordinator.syncNow().get(5, TimeUnit.SECONDS);

            assertFalse(outcome.success());
            assertEquals(SyncStatus.SYNC_FAILED, coordinator.getCurrentStatus());
            assertEquals(1, coordinator.getPendingRecordCount(), "Pending record must be preserved for retry.");
        }
    }

    @Test
    @DisplayName("HTTP 429 respects Retry-After header delay")
    void testHttp429RetryAfterHandling() throws Exception {
        mockUploadOutcome = UploadOutcome.failed("Rate limit reached", 429, 45); // Retry-After: 45s
        ServerHandover handover = createMockHandover();

        try (SyncCoordinator coordinator = createCoordinator(handover)) {
            UploadOutcome outcome = coordinator.syncNow().get(5, TimeUnit.SECONDS);

            assertFalse(outcome.success());
            assertEquals(429, outcome.httpStatusCode());
            assertEquals(45, outcome.retryAfterSeconds());
            assertEquals(SyncStatus.SYNC_FAILED, coordinator.getCurrentStatus());
        }
    }

    @Test
    @DisplayName("HTTP 401 triggers token refresh and does not loop indefinitely on invalid session")
    void testHttp401TokenRefresh() throws Exception {
        AtomicBoolean refreshAttempted = new AtomicBoolean(false);
        FirebaseAuthService mockAuth = new FirebaseAuthService(session) {
            @Override
            public String getValidIdToken() {
                refreshAttempted.set(true);
                return null; // Session cannot be refreshed
            }
        };

        mockUploadOutcome = UploadOutcome.failed("Unauthorized", 401, -1);
        ServerHandover handover = new ServerHandover(mockAuth) {
            @Override
            public UploadOutcome uploadSnapshot(SystemHealthSnapshot snapshot) {
                return mockUploadOutcome;
            }
        };

        try (SyncCoordinator coordinator = new SyncCoordinator(
                session, mockAuth, taskManager, handover, tempDir, scheduler, () -> true)) {

            UploadOutcome outcome = coordinator.syncNow().get(5, TimeUnit.SECONDS);
            assertFalse(outcome.success());
            assertTrue(refreshAttempted.get(), "Token refresh must be attempted on 401.");
            assertEquals(SyncStatus.SYNC_FAILED, coordinator.getCurrentStatus());
        }
    }

    @Test
    @DisplayName("No duplicate uploads for the same record and single-flight mechanism")
    void testNoDuplicateSimultaneousUploads() throws Exception {
        CompletableFuture<UploadOutcome> uploadBlocker = new CompletableFuture<>();
        ServerHandover slowHandover = new ServerHandover(authService) {
            @Override
            public UploadOutcome uploadSnapshot(SystemHealthSnapshot snapshot) {
                uploadCallCount.incrementAndGet();
                try {
                    return uploadBlocker.get(3, TimeUnit.SECONDS);
                } catch (Exception e) {
                    return UploadOutcome.failed("Timeout");
                }
            }
        };

        try (SyncCoordinator coordinator = createCoordinator(slowHandover)) {
            // Start first upload
            CompletableFuture<UploadOutcome> flight1 = coordinator.syncNow();

            // Trigger second upload immediately while first is in-flight
            CompletableFuture<UploadOutcome> flight2 = coordinator.syncNow();

            assertSame(flight1, flight2, "Second sync call while flight is in progress must return identical in-flight future.");

            // Complete the flight
            uploadBlocker.complete(UploadOutcome.saved(555L));

            UploadOutcome outcome1 = flight1.get(5, TimeUnit.SECONDS);
            UploadOutcome outcome2 = flight2.get(5, TimeUnit.SECONDS);

            assertTrue(outcome1.success());
            assertTrue(outcome2.success());
            assertEquals(1, uploadCallCount.get(), "Upload method must be called exactly once.");
        }
    }

    @Test
    @DisplayName("Pending records survive application restart and are reloaded from disk")
    void testPendingRecordsSurviveRestart() throws Exception {
        mockUploadOutcome = UploadOutcome.failed("Server timeout", 504, -1);
        ServerHandover handover = createMockHandover();

        // 1. First application instance creates and persists a pending record
        try (SyncCoordinator appInstance1 = createCoordinator(handover)) {
            appInstance1.syncNow().get(5, TimeUnit.SECONDS);
            assertEquals(1, appInstance1.getPendingRecordCount(), "Instance 1 must have 1 pending record.");
        } // appInstance1 is closed, simulating app exit

        // Verify file exists on disk
        try (var stream = Files.list(tempDir)) {
            assertTrue(stream.count() > 0, "Pending file must be on disk.");
        }

        // 2. New application instance starts and loads the pending record from disk
        scheduler = Executors.newSingleThreadScheduledExecutor();
        mockUploadOutcome = UploadOutcome.saved(888L); // Server is now available
        try (SyncCoordinator appInstance2 = createCoordinator(handover)) {
            assertEquals(1, appInstance2.getPendingRecordCount(), "Instance 2 must reload pending record on launch.");

            // Now sync succeeds
            UploadOutcome outcome = appInstance2.syncNow().get(5, TimeUnit.SECONDS);
            assertTrue(outcome.success());
            assertEquals(0, appInstance2.getPendingRecordCount(), "Queue cleared after success.");
        }
    }

    @Test
    @DisplayName("Scheduler shuts down cleanly on close")
    void testSchedulerShutdown() {
        ScheduledExecutorService testScheduler = Executors.newSingleThreadScheduledExecutor();
        SyncCoordinator coordinator = new SyncCoordinator(
                session, authService, taskManager, createMockHandover(), tempDir, testScheduler, () -> true);

        assertFalse(testScheduler.isShutdown());
        coordinator.close();
        assertTrue(testScheduler.isShutdown(), "Executor service must be shut down upon close.");
    }
}
