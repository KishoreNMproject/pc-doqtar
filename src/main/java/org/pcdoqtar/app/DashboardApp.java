
package org.pcdoqtar.app;

import java.util.List;

import org.pcdoqtar.diagnostics.SystemDiagnostics;
import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.HealthStatus;
import org.pcdoqtar.model.SystemHealthSnapshot;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DashboardApp extends Application {

    private static final String BG = "#090909";
    private static final String PANEL = "#131313";
    private static final String PANEL_HOVER = "#1D1D1D";
    private static final String BORDER = "#292929";
    private static final String TEXT = "#F5F5F5";
    private static final String MUTED = "#888888";
    private static final String LIME = "#B8FF3B";
    private static final String AMBER = "#FFB020";
    private static final String RED = "#FF5555";

    private final SystemDiagnostics diagnostics =
            new SystemDiagnostics();

    private BorderPane root;
    private VBox sidebar;
    private String currentPage = "Overview";

    private Label cpuValue;
    private Label memoryValue;
    private Label storageAlertsValue;
    private Label overallValue;
    private Label statusMessage;
    private Label timestampLabel;

    private BarChart<String, Number> storageChart;
    private SystemHealthSnapshot latestSnapshot;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");

        sidebar = createSidebar();
        root.setLeft(sidebar);
        root.setCenter(createScrollable(createDashboard()));

        Scene scene = new Scene(root, 1280, 820);

        var stylesheet = getClass().getResource("dashboard.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("PC DOQTAR | System Health");
        stage.setMinWidth(1000);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();

        runDiagnosticsOnce();
    }

    private ScrollPane createScrollable(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPannable(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background: " + BG + ";" +
                "-fx-background-color: " + BG + ";"
        );
        return scroll;
    }

    private VBox createSidebar() {
        VBox panel = new VBox(28);
        panel.setPadding(new Insets(30, 17, 22, 17));
        panel.setPrefWidth(220);
        panel.setMinWidth(220);
        panel.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-width: 0 1 0 0;"
        );

        Label brand = new Label("PC DOQTAR");
        brand.setStyle(
                "-fx-font-size: 23px;" +
                "-fx-font-weight: 900;" +
                "-fx-text-fill: " + LIME + ";"
        );

        Label tagline = new Label("SYSTEM INTELLIGENCE");
        tagline.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-letter-spacing: 1.4px;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        VBox branding = new VBox(7, brand, tagline);

        Label sectionLabel = new Label("WORKSPACE");
        sectionLabel.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #626262;"
        );

        VBox navigation = new VBox(8,
                navItem("01", "Overview"),
                navItem("02", "Diagnostics"),
                navItem("03", "Processes"),
                navItem("04", "Network"),
                navItem("05", "Settings")
        );

        VBox navBlock = new VBox(13, sectionLabel, navigation);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label localStatus = new Label("●  LOCAL DIAGNOSTICS");
        localStatus.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-text-fill: " + LIME + ";"
        );

        Label version = new Label("PC DOQTAR  •  DESKTOP");
        version.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        VBox footer = new VBox(8, localStatus, version);

        panel.getChildren().addAll(
                branding, navBlock, spacer, footer
        );

        return panel;
    }

    private Button navItem(String number, String page) {
        Button button = new Button(number + "    " + page);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(13, 12, 13, 12));
        button.setFocusTraversable(false);

        updateNavStyle(button, page.equals(currentPage));
        button.setOnAction(event -> showPage(page));

        return button;
    }

    private void updateNavStyle(Button button, boolean selected) {
        button.setStyle(
                "-fx-background-color: "
                        + (selected ? PANEL_HOVER : "transparent") + ";" +
                "-fx-text-fill: "
                        + (selected ? LIME : "#A0A0A0") + ";" +
                "-fx-font-size: 12px;" +
                "-fx-background-radius: 6;" +
                "-fx-border-radius: 6;" +
                "-fx-border-color: "
                        + (selected ? "#3A3A3A" : "transparent") + ";" +
                "-fx-border-width: 1;" +
                "-fx-cursor: hand;"
        );
    }

    private void showPage(String page) {
        currentPage = page;
        sidebar = createSidebar();
        root.setLeft(sidebar);

        Node content;

        switch (page) {
            case "Overview" -> content = createDashboard();
            case "Diagnostics" -> content = createDiagnosticsPage();
            case "Processes" -> content = createPlaceholderPage(
                    "Processes",
                    "Process-level CPU and memory information."
            );
            case "Network" -> content = createPlaceholderPage(
                    "Network",
                    "Network interfaces and internet performance."
            );
            case "Settings" -> content = createPlaceholderPage(
                    "Settings",
                    "Application preferences and diagnostic thresholds."
            );
            default -> content = createDashboard();
        }

        root.setCenter(createScrollable(content));
    }

    private VBox createDashboard() {
        VBox content = pageContainer();

        Label eyebrow = new Label("SYSTEM MONITORING  /  OVERVIEW");
        eyebrow.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-text-fill: " + LIME + ";"
        );

        Label title = new Label("System Overview");
        title.setStyle(
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        Label subtitle = new Label(
                "A clear picture of your PC's current health."
        );
        subtitle.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        VBox heading = new VBox(7, eyebrow, title, subtitle);

        cpuValue = new Label("--");
        memoryValue = new Label("--");
        storageAlertsValue = new Label("--");
        overallValue = new Label("PENDING");

        HBox cards = new HBox(12,
                metricCard("CPU LOAD", cpuValue, "Processor utilisation"),
                metricCard("MEMORY", memoryValue, "Physical RAM usage"),
                metricCard("STORAGE ALERTS", storageAlertsValue,
                        "Volumes over threshold"),
                metricCard("SYSTEM HEALTH", overallValue,
                        "Overall diagnostic status")
        );

        for (Node card : cards.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        VBox chartPanel = createStoragePanel();

        Label reportHeading = new Label("Latest diagnostic");
        reportHeading.setStyle(
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        Label reportTag = new Label("SINGLE RUN");
        reportTag.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-text-fill: " + LIME + ";" +
                "-fx-background-color: #252D1A;" +
                "-fx-padding: 6 9 6 9;" +
                "-fx-background-radius: 4;"
        );

        HBox reportHeader = new HBox(reportHeading, reportTag);
        reportHeader.setAlignment(Pos.CENTER_LEFT);
        reportHeader.setSpacing(12);
        HBox.setHgrow(reportHeading, Priority.ALWAYS);

        statusMessage = new Label("Waiting for diagnostic results...");
        statusMessage.setWrapText(true);
        statusMessage.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        timestampLabel = new Label("No completed check yet");
        timestampLabel.setStyle(
                "-fx-font-size: 10px;" +
                "-fx-text-fill: #666666;"
        );

        VBox report = new VBox(
                13, reportHeader, statusMessage, timestampLabel
        );
        report.setPadding(new Insets(20));
        report.setStyle(panelStyle());

        content.getChildren().addAll(
                heading, cards, chartPanel, report
        );

        if (latestSnapshot != null) {
            Platform.runLater(() -> displaySnapshot(latestSnapshot));
        }

        return content;
    }

    private VBox metricCard(
            String title, Label value, String description) {

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        value.setStyle(
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        Label detail = new Label(description);
        detail.setWrapText(true);
        detail.setStyle(
                "-fx-font-size: 10px;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(12, titleLabel, value, spacer, detail);
        card.setPadding(new Insets(17));
        card.setMinWidth(120);
        card.setPrefHeight(132);
        card.setStyle(panelStyle());

        return card;
    }

    private VBox createStoragePanel() {
        Label title = new Label("Storage utilisation");
        title.setStyle(
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        Label subtitle = new Label(
                "Used capacity by detected volume"
        );
        subtitle.setStyle(
                "-fx-font-size: 10px;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        VBox chartHeading = new VBox(5, title, subtitle);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("VOLUME");

        NumberAxis yAxis = new NumberAxis(0, 100, 25);
        yAxis.setLabel("USED SPACE (%)");
        yAxis.setForceZeroInRange(true);

        storageChart = new BarChart<>(xAxis, yAxis);
        storageChart.setLegendVisible(false);
        storageChart.setAnimated(false);
        storageChart.setCategoryGap(25);
        storageChart.setBarGap(5);
        storageChart.setPrefHeight(290);
        storageChart.setMinHeight(250);
        storageChart.setAlternativeColumnFillVisible(false);
        storageChart.setAlternativeRowFillVisible(false);
        storageChart.setHorizontalGridLinesVisible(true);
        storageChart.setVerticalGridLinesVisible(true);

        VBox panel = new VBox(14, chartHeading, storageChart);
        panel.setPadding(new Insets(20));
        panel.setStyle(panelStyle());

        return panel;
    }

    private VBox createDiagnosticsPage() {
        VBox content = pageContainer();

        content.getChildren().addAll(
                eyebrow("SYSTEM MONITORING  /  REPORT"),
                heading("Diagnostics"),
                muted("Detailed results from the latest Master PC Check.")
        );

        if (latestSnapshot == null) {
            content.getChildren().add(
                    muted("Diagnostic results are not available yet.")
            );
            return content;
        }

        content.getChildren().add(resultCard(
                "CPU", latestSnapshot.getCpu()
        ));

        content.getChildren().add(resultCard(
                "Memory", latestSnapshot.getMemory()
        ));

        for (DiagnosticResult result : latestSnapshot.getStorage()) {
            content.getChildren().add(
                    resultCard(result.getMetric(), result)
            );
        }

        Label overall = new Label(
                "OVERALL STATUS     "
                        + latestSnapshot.getOverallStatus()
        );
        overall.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: "
                        + statusColour(latestSnapshot.getOverallStatus())
                        + ";"
        );

        content.getChildren().add(overall);
        return content;
    }

    private VBox resultCard(
            String title, DiagnosticResult result) {

        Label name = new Label(title);
        name.setStyle(
                "-fx-font-size: 14px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        String value = result.getStatus() == HealthStatus.UNAVAILABLE
                ? "Unavailable"
                : String.format("%.2f %s",
                        result.getValue(), result.getUnit());

        Label reading = new Label(value);
        reading.setStyle(
                "-fx-font-size: 21px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        Label status = new Label(
                result.getStatus() + "  /  " + result.getMessage()
        );
        status.setWrapText(true);
        status.setStyle(
                "-fx-font-size: 11px;" +
                "-fx-text-fill: " + statusColour(result.getStatus()) + ";"
        );

        VBox card = new VBox(10, name, reading, status);
        card.setPadding(new Insets(18));
        card.setStyle(panelStyle());

        return card;
    }

    private VBox createPlaceholderPage(
            String title, String description) {

        VBox content = pageContainer();

        Label notice = new Label(
                "This section has not been implemented yet."
        );
        notice.setWrapText(true);
        notice.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + AMBER + ";" +
                "-fx-background-color: " + PANEL + ";" +
                "-fx-padding: 18;" +
                "-fx-background-radius: 8;"
        );

        content.getChildren().addAll(
                eyebrow("SYSTEM MONITORING  /  " + title.toUpperCase()),
                heading(title),
                muted(description),
                notice
        );

        return content;
    }

    private void runDiagnosticsOnce() {
        Thread thread = new Thread(() -> {
            try {
                SystemHealthSnapshot snapshot =
                        diagnostics.collectSnapshot();

                Platform.runLater(() -> {
                    latestSnapshot = snapshot;

                    if ("Overview".equals(currentPage)) {
                        displaySnapshot(snapshot);
                    }
                });

            } catch (Exception exception) {
                Platform.runLater(() -> {
                    if (statusMessage != null) {
                        statusMessage.setText(
                                "Diagnostic collection failed: "
                                        + exception.getMessage()
                        );
                        statusMessage.setStyle(
                                "-fx-text-fill: " + RED + ";"
                        );
                    }
                });
            }
        }, "PC-DOQTAR-Initial-Check");

        thread.setDaemon(true);
        thread.start();
    }

    private void displaySnapshot(SystemHealthSnapshot snapshot) {
        if (cpuValue == null || memoryValue == null
                || storageAlertsValue == null || overallValue == null
                || storageChart == null || statusMessage == null) {
            return;
        }

        cpuValue.setText(formatPercentage(snapshot.getCpu()));
        memoryValue.setText(formatPercentage(snapshot.getMemory()));

        List<DiagnosticResult> storage = snapshot.getStorage();

        long alerts = storage.stream()
                .filter(result ->
                        result.getStatus() == HealthStatus.WARNING
                        || result.getStatus() == HealthStatus.CRITICAL)
                .count();

        storageAlertsValue.setText(Long.toString(alerts));

        HealthStatus overall = snapshot.getOverallStatus();
        overallValue.setText(overall.toString());
        overallValue.setStyle(
                "-fx-font-size: 18px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + statusColour(overall) + ";"
        );

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (DiagnosticResult result : storage) {
            if (result.getStatus() == HealthStatus.UNAVAILABLE) {
                continue;
            }

            String volume = result.getMetric()
                    .replace("Storage utilisation (", "")
                    .replace(")", "");

            XYChart.Data<String, Number> point =
                    new XYChart.Data<>(volume, result.getValue());

            series.getData().add(point);

            point.nodeProperty().addListener(
                    (observable, oldNode, newNode) -> {
                        if (newNode != null) {
                            newNode.setStyle(
                                    "-fx-bar-fill: "
                                            + statusColour(result.getStatus())
                                            + ";"
                            );
                        }
                    }
            );
        }

        storageChart.getData().setAll(series);

        statusMessage.setText(
                "Overall status: " + overall
                + "     |     Volumes requiring attention: " + alerts
                + (snapshot.hasUnavailableResults()
                        ? "\nSome readings were unavailable."
                        : "")
        );

        statusMessage.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + statusColour(overall) + ";"
        );

        if (timestampLabel != null) {
            timestampLabel.setText(
                    "Completed  •  " + snapshot.getTimestamp()
            );
        }
    }

    private String formatPercentage(DiagnosticResult result) {
        if (result.getStatus() == HealthStatus.UNAVAILABLE) {
            return "N/A";
        }

        return String.format("%.1f%%", result.getValue());
    }

    private String statusColour(HealthStatus status) {
        return switch (status) {
            case NORMAL -> LIME;
            case WARNING -> AMBER;
            case CRITICAL -> RED;
            case UNAVAILABLE -> MUTED;
        };
    }

    private VBox pageContainer() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: " + BG + ";");
        return content;
    }

    private Label heading(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );
        return label;
    }

    private Label eyebrow(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-text-fill: " + LIME + ";"
        );
        return label;
    }

    private Label muted(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-text-fill: " + MUTED + ";"
        );
        return label;
    }

    private String panelStyle() {
        return "-fx-background-color: " + PANEL + ";" +
               "-fx-background-radius: 9;" +
               "-fx-border-color: " + BORDER + ";" +
               "-fx-border-radius: 9;";
    }

    @Override
    public void stop() {
        // No recurring monitoring or scheduled tasks are started here.
    }

    public static void main(String[] args) {
        launch(args);
    }
}
