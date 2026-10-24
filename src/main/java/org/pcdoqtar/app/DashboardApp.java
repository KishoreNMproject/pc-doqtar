package org.pcdoqtar.app;

import javafx.application.Application;
import javafx.stage.Stage;
import org.pcdoqtar.lifecycle.ApplicationLifecycle;

public final class DashboardApp extends Application {

    private final ApplicationLifecycle lifecycle =
            new ApplicationLifecycle();

    @Override
    public void start(Stage stage) {
        lifecycle.start(stage);
    }

    @Override
    public void stop() {
        lifecycle.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}