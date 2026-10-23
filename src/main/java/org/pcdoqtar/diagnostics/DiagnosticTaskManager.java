
package org.pcdoqtar.diagnostics;

import java.net.SocketException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.pcdoqtar.diagnostics.network.AdapterInfo;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics;
import org.pcdoqtar.model.SystemHealthSnapshot;

public final class DiagnosticTaskManager implements AutoCloseable {

    private static final int WORKER_COUNT = 3;
    private static final int QUEUE_CAPACITY = 24;

    private final SystemDiagnostics systemDiagnostics;
    private final NetworkDiagnostics networkDiagnostics;
    private final ExecutorService executor;

    private volatile boolean closed;

    public DiagnosticTaskManager(
            SystemDiagnostics systemDiagnostics,
            NetworkDiagnostics networkDiagnostics) {

        this.systemDiagnostics = Objects.requireNonNull(
                systemDiagnostics, "System diagnostics is required.");

        this.networkDiagnostics = Objects.requireNonNull(
                networkDiagnostics, "Network diagnostics is required.");

        this.executor = new ThreadPoolExecutor(
                WORKER_COUNT,
                WORKER_COUNT,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                createThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /**
     * Collects a CPU, memory and storage snapshot in the background.
     */
    public CompletableFuture<SystemHealthSnapshot> collectSnapshot() {
        return submit(systemDiagnostics::collectSnapshot);
    }

    /**
     * Collects network adapter information in the background.
     */
    public CompletableFuture<List<AdapterInfo>> collectNetworkAdapters() {
        return submit(() -> {
            try {
                return List.copyOf(networkDiagnostics.collectAdapters());
            } catch (SocketException exception) {
                throw new DiagnosticTaskException(
                        "Unable to collect network adapter information.",
                        exception);
            }
        });
    }

    /**
     * Runs a diagnostic operation using the shared worker pool.
     *
     * Do not use this method for JavaFX UI updates. Apply results
     * to controls on the JavaFX Application Thread.
     */
    public <T> CompletableFuture<T> submit(Supplier<T> operation) {
        Objects.requireNonNull(operation, "Operation is required.");

        if (closed) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException(
                            "Diagnostic task manager is closed."));
        }

        try {
            return CompletableFuture.supplyAsync(() -> {
                if (closed) {
                    throw new IllegalStateException(
                            "Diagnostic task manager is closed.");
                }

                return operation.get();
            }, executor);

        } catch (RejectedExecutionException exception) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException(
                            "Diagnostic queue is full or shutting down.",
                            exception));
        }
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        executor.shutdownNow();
    }

    private ThreadFactory createThreadFactory() {
        AtomicInteger counter = new AtomicInteger(1);

        return task -> {
            Thread thread = new Thread(
                    task,
                    "pc-doqtar-diagnostics-" + counter.getAndIncrement());

            thread.setDaemon(true);
            return thread;
        };
    }

    public static final class DiagnosticTaskException
            extends RuntimeException {

        public DiagnosticTaskException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
