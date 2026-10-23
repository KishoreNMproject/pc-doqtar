
package org.pcdoqtar.lifecycle;

import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import javafx.application.Platform;

public final class JavaFxTaskBridge {

    private JavaFxTaskBridge() {
        // Utility class.
    }

    /**
     * Runs the completion callback on the JavaFX Application Thread.
     * The callback receives either the result or the unwrapped error.
     */
    public static <T> void whenCompleteOnFxThread(
            CompletableFuture<T> future,
            BiConsumer<? super T, ? super Throwable> callback) {

        Objects.requireNonNull(future, "Future is required.");
        Objects.requireNonNull(callback, "Callback is required.");

        future.whenComplete((result, error) -> {
            Throwable cause = unwrap(error);

            Runnable callbackTask = () -> callback.accept(result, cause);

            if (Platform.isFxApplicationThread()) {
                callbackTask.run();
            } else {
                try {
                    Platform.runLater(callbackTask);
                } catch (IllegalStateException toolkitStopped) {
                    // The JavaFX toolkit has already shut down.
                    // There is no UI thread left on which to invoke
                    // the callback safely.
                }
            }
        });
    }

    /**
     * Executes a UI action on the JavaFX Application Thread.
     */
    public static void runOnFxThread(Runnable action) {
        Objects.requireNonNull(action, "Action is required.");

        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;

        while (current instanceof CompletionException
                && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }
}
