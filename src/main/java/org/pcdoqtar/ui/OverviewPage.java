package org.pcdoqtar.ui;

import org.pcdoqtar.diagnostics.SystemDiagnostics;
import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.HealthStatus;
import org.pcdoqtar.model.SystemHealthSnapshot;
import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class OverviewPage extends VBox {

    private final SystemDiagnostics diagnostics;
    private final Label overallStatus = new Label("Waiting for scan");
    private final Label lastUpdated = new Label("No snapshot available");
    private final Label warningCount = new Label("--");
    private final GridPane metricsGrid = new GridPane();
    private final GridPane chartsGrid = new GridPane();
    private final Label message = new Label("Ready");

    private boolean scanRunning;

    public OverviewPage() {
        this(new SystemDiagnostics());
    }

    public OverviewPage(SystemDiagnostics diagnostics) {
        if (diagnostics == null) {
            throw new IllegalArgumentException("Diagnostics service must not be null.");
        }
        this.diagnostics = diagnostics;

        setSpacing(20);
        setPadding(new Insets(28));
        setStyle(backgroundStyle());

        Label eyebrow = new Label("SYSTEM MONITORING / OVERVIEW");
        eyebrow.setStyle(accentStyle());

        Label heading = new Label("System Overview");
        heading.setStyle(textStyle() + "-fx-font-size: 30px;-fx-font-weight: bold;");

        Label description = new Label(
                "Review the latest CPU, memory and storage health results.");
        description.setWrapText(true);
        description.setStyle(mutedStyle());

        VBox statusCard = createCard();
        Label statusHeading = new Label("Overall health");
        statusHeading.setStyle(textStyle() + "-fx-font-size: 12px;");
        overallStatus.setStyle(textStyle() + "-fx-font-size: 24px;-fx-font-weight: bold;");
        warningCount.setStyle(textStyle() + "-fx-font-size: 15px;-fx-font-weight: bold;");
        statusCard.getChildren().addAll(statusHeading, overallStatus, warningCount);

        metricsGrid.setHgap(12);
        metricsGrid.setVgap(12);
        chartsGrid.setHgap(12);
        chartsGrid.setVgap(12);

        Button refreshButton = new Button("Run health check");
        refreshButton.setStyle(
                "-fx-background-color: " + ThemeManager.getColour("accent") + ";"
                + "-fx-text-fill: " + ThemeManager.getColour("background") + ";"
                + "-fx-font-weight: bold;-fx-padding: 10 16 10 16;");
        refreshButton.setOnAction(event -> refresh());

        message.setWrapText(true);
        message.setStyle(mutedStyle());

        getChildren().addAll(
                eyebrow, heading, description, statusCard,
                metricsGrid, chartsGrid, refreshButton, lastUpdated, message);
        refresh();
    }

    public void displaySnapshot(SystemHealthSnapshot snapshot) {
        if (snapshot == null) {
            message.setText("No diagnostic snapshot was provided.");
            return;
        }

        HealthStatus status = snapshot.getOverallStatus();
        overallStatus.setText(status.toString());
        overallStatus.setStyle(
                "-fx-text-fill: " + statusColour(status) + ";"
                + "-fx-font-size: 24px;-fx-font-weight: bold;");
        warningCount.setText("Warnings / critical results: " + snapshot.getWarningCount());

        metricsGrid.getChildren().clear();
        chartsGrid.getChildren().clear();

        addMetric("CPU", snapshot.getCpu(), 0, 0);
        addMetric("Memory", snapshot.getMemory(), 1, 0);
        addUsageChart("CPU utilisation", snapshot.getCpu(), 0, 0);
        addUsageChart("Memory utilisation", snapshot.getMemory(), 1, 0);

        int index = 0;
        for (DiagnosticResult result : snapshot.getStorage()) {
            addMetric(result.getMetric(), result, index % 2, 1 + index / 2);
            addUsageChart(result.getMetric(), result, index % 3, 1 + index / 3);
            index++;
        }

        lastUpdated.setText("Last updated: " + snapshot.getTimestamp());
        message.setText(snapshot.hasUnavailableResults()
                ? "Some readings are unavailable. Check the detailed diagnostics."
                : "Snapshot updated successfully.");
    }

    public void refresh() {
        if (scanRunning) return;
        scanRunning = true;
        message.setText("Collecting health readings...");
        Thread worker = new Thread(() -> {
            try {
                SystemHealthSnapshot snapshot = diagnostics.collectSnapshot();
                Platform.runLater(() -> displaySnapshot(snapshot));
            } catch (Exception exception) {
                Platform.runLater(() -> message.setText(
                        "Health check failed: " + exception.getMessage()));
            } finally {
                Platform.runLater(() -> scanRunning = false);
            }
        }, "PC-DOQTAR-Overview-Scan");
        worker.setDaemon(true);
        worker.start();
    }

    private void addMetric(String title, DiagnosticResult result, int column, int row) {
        VBox card = createCard();
        Label metricTitle = new Label(title);
        metricTitle.setWrapText(true);
        metricTitle.setStyle(mutedStyle());

        String value = Double.isFinite(result.getValue())
                ? String.format("%.1f %s", result.getValue(), result.getUnit())
                : "Unavailable";
        Label metricValue = new Label(value);
        metricValue.setWrapText(true);
        metricValue.setStyle("-fx-text-fill: " + statusColour(result.getStatus())
                + ";-fx-font-size: 20px;-fx-font-weight: bold;");

        Label detail = new Label(result.getMessage());
        detail.setWrapText(true);
        detail.setStyle(mutedStyle());
        card.getChildren().addAll(metricTitle, metricValue, detail);
        metricsGrid.add(card, column, row);
        GridPane.setHgrow(card, Priority.ALWAYS);
    }

    private void addUsageChart(String title, DiagnosticResult result, int column, int row) {
        double raw = result.getValue();
        if (!Double.isFinite(raw)) return;
        double used = Math.max(0.0, Math.min(100.0, raw));

        VBox card = createCard();
        Label chartHeading = new Label(title);
        chartHeading.setWrapText(true);
        chartHeading.setStyle(textStyle() + "-fx-font-size: 15px;-fx-font-weight: bold;");

        PieChart chart = new PieChart();
        chart.getData().setAll(
                new PieChart.Data("Used", used),
                new PieChart.Data("Available", 100.0 - used));
        chart.setTitle(null);
        chart.setLegendVisible(true);
        chart.setLabelsVisible(false);
        chart.setAnimated(false);
        chart.setMinHeight(180);
        chart.setPrefHeight(210);
        chart.setMinWidth(190);
        chart.getStyleClass().add("usage-chart");
        card.getChildren().addAll(chartHeading, chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
        chartsGrid.add(card, column, row);
        GridPane.setHgrow(card, Priority.ALWAYS);
    }

    private VBox createCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: " + ThemeManager.getColour("panel")
                + ";-fx-background-radius: 8;-fx-border-color: "
                + ThemeManager.getColour("border") + ";-fx-border-radius: 8;");
        return card;
    }

    private String statusColour(HealthStatus status) {
        return switch (status) {
            case NORMAL -> ThemeManager.getColour("accent");
            case WARNING -> ThemeManager.getColour("warning");
            case CRITICAL -> ThemeManager.getColour("critical");
            case UNAVAILABLE -> ThemeManager.getColour("muted");
        };
    }

    private String backgroundStyle() {
        return "-fx-background-color: " + ThemeManager.getColour("background") + ";";
    }
    private String textStyle() {
        return "-fx-text-fill: " + ThemeManager.getColour("text") + ";";
    }
    private String mutedStyle() {
        return "-fx-text-fill: " + ThemeManager.getColour("muted") + ";";
    }
    private String accentStyle() {
        return "-fx-text-fill: " + ThemeManager.getColour("accent") + ";";
    }
}