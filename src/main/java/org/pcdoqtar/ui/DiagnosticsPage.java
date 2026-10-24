
package org.pcdoqtar.ui;

import java.util.ArrayList;
import java.util.List;

import org.pcdoqtar.diagnostics.SystemDiagnostics;
import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.SystemHealthSnapshot;
import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DiagnosticsPage extends VBox {

    private final SystemDiagnostics diagnostics;
    private final VBox resultsContainer = new VBox(10);
    private final Label statusLabel = new Label("Ready");
    private boolean scanRunning;

    public DiagnosticsPage() {
        this(new SystemDiagnostics());
    }

    public DiagnosticsPage(SystemDiagnostics diagnostics) {
        if (diagnostics == null) {
            throw new IllegalArgumentException(
                    "Diagnostics service must not be null."
            );
        }

        this.diagnostics = diagnostics;
        getStyleClass().add("diagnostics-page");

        setSpacing(20);
        setPadding(new Insets(28));
        setStyle(
                "-fx-background-color: "
                + ThemeManager.getColour("background") + ";"
        );

        Label eyebrow = new Label("SYSTEM MONITORING / DIAGNOSTICS");
        eyebrow.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("accent") + ";"
        );

        Label heading = new Label("Diagnostics");
        heading.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("text") + ";"
                + "-fx-font-size: 30px;"
                + "-fx-font-weight: bold;"
        );

        Label description = new Label(
                "Run a fresh CPU, memory and storage health assessment."
        );
        description.setWrapText(true);
        description.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("muted") + ";"
        );

        Button runButton = new Button("Run diagnostics");
        runButton.setStyle(
                "-fx-background-color: "
                + ThemeManager.getColour("accent") + ";"
                + "-fx-text-fill: "
                + ThemeManager.getColour("background") + ";"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 10 16 10 16;"
        );
        runButton.setOnAction(event -> runDiagnostics());

        statusLabel.setWrapText(true);
        statusLabel.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("muted") + ";"
        );

        resultsContainer.setFillWidth(true);

        getChildren().addAll(
                eyebrow,
                heading,
                description,
                runButton,
                statusLabel,
                new Separator(),
                resultsContainer
        );

        VBox.setVgrow(resultsContainer, Priority.ALWAYS);
        runDiagnostics();
    }

    public void displaySnapshot(SystemHealthSnapshot snapshot) {
        resultsContainer.getChildren().clear();

        if (snapshot == null) {
            statusLabel.setText("No diagnostic results are available.");
            return;
        }

        List<DiagnosticResult> results = new ArrayList<>();
        results.add(snapshot.getCpu());
        results.add(snapshot.getMemory());
        results.addAll(snapshot.getStorage());

        for (DiagnosticResult result : results) {
            resultsContainer.getChildren().add(createResultRow(result));
        }

        statusLabel.setText(
                "Assessment complete · Overall status: "
                + snapshot.getOverallStatus()
                + " · Warnings: " + snapshot.getWarningCount()
                + " · " + snapshot.getTimestamp()
        );
    }

    private VBox createResultRow(DiagnosticResult result) {
        VBox card = new VBox(7);
        card.getStyleClass().add("diagnostic-result-card");
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: " + ThemeManager.getColour("panel") + ";"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: " + ThemeManager.getColour("border") + ";"
                + "-fx-border-radius: 8;"
        );

        Label metric = new Label(result.getMetric());
        metric.setWrapText(true);
        metric.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("text") + ";"
                + "-fx-font-size: 15px;"
                + "-fx-font-weight: bold;"
        );

        String value = Double.isFinite(result.getValue())
                ? String.format("%.2f %s",
                        result.getValue(), result.getUnit())
                : "Unavailable";

        Label reading = new Label(
                value + "  ·  " + result.getStatus()
        );
        reading.setWrapText(true);
        reading.setStyle(
                "-fx-text-fill: " + colourFor(result.getStatus()) + ";"
                + "-fx-font-weight: bold;"
        );

        Label detail = new Label(result.getMessage());
        detail.setWrapText(true);
        detail.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("muted") + ";"
        );

        card.getChildren().addAll(metric, reading, detail);
        return card;
    }

    private String colourFor(
            org.pcdoqtar.model.HealthStatus status) {

        return switch (status) {
            case NORMAL -> ThemeManager.getColour("accent");
            case WARNING -> ThemeManager.getColour("warning");
            case CRITICAL -> ThemeManager.getColour("critical");
            case UNAVAILABLE -> ThemeManager.getColour("muted");
        };
    }

    private void runDiagnostics() {
        if (scanRunning) {
            return;
        }

        scanRunning = true;
        statusLabel.setText("Running diagnostics...");
        resultsContainer.getChildren().clear();

        Thread worker = new Thread(() -> {
            try {
                SystemHealthSnapshot snapshot =
                        diagnostics.collectSnapshot();

                Platform.runLater(() -> displaySnapshot(snapshot));
            } catch (Exception exception) {
                Platform.runLater(() -> statusLabel.setText(
                        "Diagnostics failed: " + exception.getMessage()
                ));
            } finally {
                Platform.runLater(() -> scanRunning = false);
            }
        }, "PC-DOQTAR-Diagnostics-Scan");

        worker.setDaemon(true);
        worker.start();
    }
}
