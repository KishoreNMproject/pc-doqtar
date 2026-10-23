package org.pcdoqtar.ui.components;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.pcdoqtar.lifecycle.JavaFxTaskBridge;

import javafx.application.Platform;

public final class DiagnosticPageRefresh implements AutoCloseable {

    private boolean running;
    private boolean closed;
    private long generation;

    public <T> boolean run(
            Supplier<CompletableFuture<T>> operation,
            Consumer<T> onSuccess,
            Consumer<String> onFailure,
            Consumer<Boolean> onRunningChanged) {

        requireFxThread();

        Objects.requireNonNull(operation, "Operation is required.");
        Objects.requireNonNull(onSuccess, "Success callback is required.");
        Objects.requireNonNull(onFailure, "Failure callback is required.");
        Objects.requireNonNull(
                onRunningChanged, "Running-state callback is required.");

        if (closed || running) {
            return false;
        }

        running = true;
        long currentGeneration = ++generation;
        onRunningChanged.accept(true);

        final CompletableFuture<T> future;

        try {
            future = Objects.requireNonNull(
                    operation.get(),
                    "Operation returned a null future.");
        } catch (RuntimeException exception) {
            finishFailure(
                    currentGeneration,
                    exception,
                    onFailure,
                    onRunningChanged);
            return true;
        }

        JavaFxTaskBridge.whenCompleteOnFxThread(future, (result, error) -> {
            if (closed || currentGeneration != generation) {
                return;
            }

            running = false;
            onRunningChanged.accept(false);

            if (error != null) {
                onFailure.accept(safeMessage(error));
            } else {
                onSuccess.accept(result);
            }
        });

        return true;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        requireFxThread();
        closed = true;
        running = false;
        generation++;
    }

    private void finishFailure(
            long currentGeneration,
            RuntimeException exception,
            Consumer<String> onFailure,
            Consumer<Boolean> onRunningChanged) {

        if (closed || currentGeneration != generation) {
            return;
        }

        running = false;
        onRunningChanged.accept(false);
        onFailure.accept(safeMessage(exception));
    }

    private static String safeMessage(Throwable error) {
        Throwable cause = error;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();

        return message == null || message.isBlank()
                ? cause.getClass().getSimpleName()
                : message;
    }

    private static void requireFxThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "This operation must run on the JavaFX Application Thread.");
        }
    }
}