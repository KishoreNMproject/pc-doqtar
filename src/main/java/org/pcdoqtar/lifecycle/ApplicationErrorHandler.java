package org.pcdoqtar.lifecycle;

import java.util.Objects;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public final class ApplicationErrorHandler {

    private ApplicationErrorHandler() {
        // Utility class.
    }

    public static void showStartupError(
            Window owner,
            String message) {

        show(
                owner,
                Alert.AlertType.ERROR,
                "PC DOQTAR — Startup Error",
                "The application could not start.",
                message);
    }

    public static void showError(
            Window owner,
            String title,
            String message) {

        show(
                owner,
                Alert.AlertType.ERROR,
                title,
                "An operation could not be completed.",
                message);
    }

    public static void showWarning(
            Window owner,
            String title,
            String message) {

        show(
                owner,
                Alert.AlertType.WARNING,
                title,
                "Please review the following information.",
                message);
    }

    private static void show(
            Window owner,
            Alert.AlertType type,
            String title,
            String header,
            String message) {

        Objects.requireNonNull(type, "Alert type is required.");

        Runnable display = () -> {
            Alert alert = new Alert(type, "", ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(
                    message == null || message.isBlank()
                            ? "No additional details are available."
                            : message);

            if (owner != null) {
                alert.initOwner(owner);
            }

            alert.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            display.run();
        } else {
            Platform.runLater(display);
        }
    }
}