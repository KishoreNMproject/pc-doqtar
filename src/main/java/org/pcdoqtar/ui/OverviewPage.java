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
    private final HBox chartsRow = new HBox(12);
    private final PieChart cpuChart = createPieChart("CPU utilisation");
    private final PieChart memoryChart = createPieChart("Memory utilisation");
    private final PieChart storageChart = createPieChart("Storage utilisation");
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
        getStyleClass().add("overview-page");
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
        chartsRow.setFillHeight(true);
        chartsRow.getChildren().addAll(chartCard(cpuChart), chartCard(memoryChart), chartCard(storageChart));
        HBox.setHgrow(chartsRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(chartsRow.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(chartsRow.getChildren().get(2), Priority.ALWAYS);

        Button refreshButton = new Button("Run health check");
        refreshButton.setStyle("-fx-background-color: " + ThemeManager.getColour("accent")
                + ";-fx-text-fill: " + ThemeManager.getColour("background")
                + ";-fx-font-weight: bold;-fx-padding: 10 16 10 16;");
        refreshButton.setOnAction(event -> refresh());

        message.setWrapText(true);
        message.setStyle(mutedStyle());
        lastUpdated.setStyle(mutedStyle());

        getChildren().addAll(eyebrow, heading, description, statusCard,
                chartsRow, metricsGrid, refreshButton, lastUpdated, message);
        refresh();
    }

    private PieChart createPieChart(String title) {
        PieChart chart = new PieChart();
        chart.setTitle(title);
        chart.getStyleClass().add("usage-chart");
        chart.setLegendVisible(true);
        chart.setLabelsVisible(false);
        chart.setAnimated(false);
        chart.setMinWidth(180);
        chart.setMinHeight(220);
        chart.setPrefHeight(240);
        chart.setMaxHeight(260);
        chart.setData(javafx.collections.FXCollections.observableArrayList(
                new PieChart.Data("Used", 0.1), new PieChart.Data("Free", 99.9)));
        return chart;
    }

    private VBox chartCard(PieChart chart) {
        VBox card = createCard();
        card.getChildren().add(chart);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMinWidth(180);
        return card;
    }

    private void updateChart(PieChart chart, String usedLabel, double used) {
        double bounded = Double.isFinite(used) ? Math.max(0, Math.min(100, used)) : 0;
        chart.setData(javafx.collections.FXCollections.observableArrayList(
                new PieChart.Data(usedLabel, Math.max(0.001, bounded)),
                new PieChart.Data("Available", Math.max(0.001, 100 - bounded))));
    }

    public void displaySnapshot(SystemHealthSnapshot snapshot) {
        if (snapshot == null) {
            message.setText("No diagnostic snapshot was provided.");
            return;
        }

        HealthStatus status = snapshot.getOverallStatus();
        overallStatus.setText(status.toString());
        overallStatus.setStyle("-fx-text-fill: " + statusColour(status)
                + ";-fx-font-size: 24px;-fx-font-weight: bold;");
        warningCount.setText("Warnings / critical results: " + snapshot.getWarningCount());

        metricsGrid.getChildren().clear();
        addMetric("CPU", snapshot.getCpu(), 0, 0);
        addMetric("Memory", snapshot.getMemory(), 1, 0);

        int index = 0;
        double storageTotal = 0;
        int storageCount = 0;
        for (DiagnosticResult result : snapshot.getStorage()) {
            addMetric(result.getMetric(), result, index % 2, 1 + index / 2);
            if (Double.isFinite(result.getValue())) {
                storageTotal += result.getValue();
                storageCount++;
            }
            index++;
        }

        updateChart(cpuChart, "CPU used", snapshot.getCpu().getValue());
        updateChart(memoryChart, "Memory used", snapshot.getMemory().getValue());
        updateChart(storageChart, "Storage used",
                storageCount == 0 ? 0 : storageTotal / storageCount);

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

    private VBox createCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("app-tile");
        card.setPadding(new Insets(18));
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
