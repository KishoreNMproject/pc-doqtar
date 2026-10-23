
package org.pcdoqtar.lifecycle;

import javafx.stage.Stage;

public final class ApplicationLifecycle {

    private final ShutdownManager shutdownManager =
            new ShutdownManager();

    private boolean started;

    public void start(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException(
                    "Primary stage must not be null."
            );
        }

        if (started) {
            throw new IllegalStateException(
                    "Application lifecycle has already started."
            );
        }

        started = true;

        shutdownManager.initialise(stage, this::performShutdown);
        initialiseApplication(stage);
    }

    private void initialiseApplication(Stage stage) {
        /*
         * Later:
         * 1. Initialise authentication.
         * 2. Initialise diagnostics services.
         * 3. Build navigation and page registry.
         * 4. Create the main scene.
         * 5. Display the primary stage.
         */
    }

    private void performShutdown() {
        /*
         * Later:
         * Stop scheduled diagnostics, network tests,
         * and other background resources safely.
         */
    }

    public void shutdown() {
        shutdownManager.shutdown();
    }
}
