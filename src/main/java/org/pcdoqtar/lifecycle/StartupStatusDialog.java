package org.pcdoqtar.lifecycle;

import java.util.Objects;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class StartupStatusDialog {

    private final Stage stage;
    private final Label statusLabel;

    private StartupStatusDialog(
            Stage stage,
            Label statusLabel) {

        this.stage = stage;
        this.statusLabel = statusLabel;
    }

    public static StartupStatusDialog create(Window owner) {
        if (!javafx.application.Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "Create the startup dialog on the JavaFX thread.");
        }

        Stage stage = new Stage();
        stage.setTitle("PC DOQTAR");
        stage.initModality(Modality.APPLICATION_MODAL);

        if (owner != null) {
            stage.initOwner(owner);
        }

        Label heading = new Label("Preparing PC DOQTAR");
        heading.setStyle(
                "-fx-font-size: 17px;"
                + "-fx-font-weight: bold;"
        );

        Label status = new Label("Initialising...");
        status.setWrapText(true);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(28, 28);

        HBox progressRow = new HBox(12, progress, status);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(16, heading, progressRow);
        content.setPadding(new Insets(24));
        content.setPrefWidth(380);

        stage.setScene(new Scene(content));
        stage.setResizable(false);

        return new StartupStatusDialog(stage, status);
    }

    public void show() {
        requireFxThread();
        stage.show();
    }

    public void updateMessage(String message) {
        Objects.requireNonNull(message, "Status message is required.");
        requireFxThread();
        statusLabel.setText(message);
    }

    public void close() {
        requireFxThread();
        stage.close();
    }

    public boolean isShowing() {
        return stage.isShowing();
    }

    private void requireFxThread() {
        if (!javafx.application.Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                    "Update the startup dialog on the JavaFX thread.");
        }
    }
}