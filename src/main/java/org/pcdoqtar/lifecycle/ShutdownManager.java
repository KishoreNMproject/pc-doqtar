
package org.pcdoqtar.lifecycle;

import javafx.application.Platform;
import javafx.stage.Stage;

public final class ShutdownManager {

    private Stage primaryStage;
    private Runnable shutdownAction = () -> { };

    public void initialise(Stage primaryStage, Runnable shutdownAction) {
        if (primaryStage == null) {
            throw new IllegalArgumentException(
                    "Primary stage must not be null."
            );
        }

        this.primaryStage = primaryStage;
        this.shutdownAction = shutdownAction != null
                ? shutdownAction
                : () -> { };

        this.primaryStage.setOnCloseRequest(event -> shutdown());
    }

    public void shutdown() {
        try {
            shutdownAction.run();
        } finally {
            Platform.exit();
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
