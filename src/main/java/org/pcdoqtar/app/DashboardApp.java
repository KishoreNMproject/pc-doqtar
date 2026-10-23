
package org.pcdoqtar.app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.pcdoqtar.diagnostics.SystemDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkPerformanceTest;
import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.HealthStatus;
import org.pcdoqtar.model.SystemHealthSnapshot;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.pcdoqtar.ui.ProcessesPage;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics.AdapterInfo;
import org.pcdoqtar.diagnostics.network.NetworkPerformanceTest.LatencyResult;


public class DashboardApp extends Application {

    private static final String BG = "#090909";
    private static final String PANEL = "#141414";
    private static final String BORDER = "#292929";
    private static final String TEXT = "#F5F5F5";
    private static final String MUTED = "#929292";
    private static final String LIME = "#B8FF3B";
    private static final String AMBER = "#FFB020";
    private static final String RED = "#FF5555";
    private static final String GREY = "#454545";

    private final SystemDiagnostics diagnostics = new SystemDiagnostics();
    private final NetworkPerformanceTest networkTest = new NetworkPerformanceTest();
    private final NetworkDiagnostics networkDiagnostics =
            new NetworkDiagnostics();

    private List<AdapterInfo> latestAdapters = List.of();

    private LatencyResult latestLatency;

    private Label latencyValue;
    private VBox adapterList;
    private VBox trafficList;
    private Label networkTestSummary;

    private final Set<String> ignoredWarnings = new HashSet<>();

    private BorderPane root;
    private String currentPage = "Overview";

    private Label cpuValue;
    private Label memoryValue;
    private Label downloadValue;
    private Label uploadValue;
    private Label reportMessage;
    private Label reportTimestamp;

    private PieChart cpuChart;
    private PieChart memoryChart;

    private VBox warningList;
    private VBox storageList;

    private SystemHealthSnapshot latestSnapshot;

    private double downloadMbps = -1;
    private double uploadMbps = -1;
    private String networkMessage = "Network test pending";
    

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");
        root.setLeft(createSidebar());
        root.setCenter(createScrollable(createDashboard()));

        Scene scene = new Scene(root, 1280, 850);

        var css = getClass().getResource("dashboard.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("PC DOQTAR | System Health");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();

        runDiagnosticsOnce();
    }

    private ScrollPane createScrollable(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
                "-fx-background: " + BG + ";" +
                "-fx-background-color: " + BG + ";"
        );
        return scroll;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(25);
        sidebar.setPadding(new Insets(28, 16, 22, 16));
        sidebar.setPrefWidth(215);
        sidebar.setMinWidth(215);
        sidebar.setStyle(
                "-fx-background-color: " + PANEL + ";" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-width: 0 1 0 0;"
        );

        Label brand = new Label("PC DOQTAR");
        brand.setStyle(
                "-fx-font-size: 23px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + LIME + ";"
        );

        Label subtitle = new Label("SYSTEM INTELLIGENCE");
        subtitle.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        VBox branding = new VBox(6, brand, subtitle);

        Label section = new Label("WORKSPACE");
        section.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #666666;"
        );

        VBox navigation = new VBox(8,
                navButton("01   Overview", "Overview"),
                navButton("02   Diagnostics", "Diagnostics"),
                navButton("03   Processes", "Processes"),
                navButton("04   Network", "Network"),
                navButton("05   Settings", "Settings")
        );

        VBox navBlock = new VBox(12, section, navigation);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label local = new Label("●  LOCAL DIAGNOSTICS");
        local.setStyle("-fx-text-fill: " + LIME + "; -fx-font-size: 9px;");

        Label footer = new Label("SINGLE-RUN HEALTH CHECK");
        footer.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 9px;");

        sidebar.getChildren().addAll(
                branding, navBlock, spacer, local, footer
        );
        return sidebar;
    }

    private Button navButton(String text, String page) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setPadding(new Insets(12));
        button.setFocusTraversable(false);

        button.setStyle(
                "-fx-background-color: "
                        + (page.equals(currentPage) ? "#292929" : "transparent")
                        + ";" +
                "-fx-text-fill: "
                        + (page.equals(currentPage) ? LIME : MUTED) + ";" +
                "-fx-font-size: 12px;" +
                "-fx-background-radius: 5;"
        );

        button.setOnAction(event -> showPage(page));
        return button;
    }

    private void showPage(String page) {
        currentPage = page;
        root.setLeft(createSidebar());

        Node content = switch (page) {
            case "Overview" -> createDashboard();
            case "Diagnostics" -> createDiagnosticsPage();
            case "Processes" ->  new ProcessesPage();
            case "Network" -> createNetworkPage();
            case "Settings" -> placeholderPage(
                    "Settings", "Application preferences and thresholds."
            );
            default -> createDashboard();
        };

        root.setCenter(createScrollable(content));
    }

    private VBox createDashboard() {
        VBox content = pageContainer();

        Label eyebrow = new Label("SYSTEM MONITORING / OVERVIEW");
        eyebrow.setStyle("-fx-text-fill: " + LIME + "; -fx-font-size: 10px;");

        Label title = new Label("System Overview");
        title.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;"
        );
        Label subtitle = muted(
                "Current health, resource utilisation and network performance."
        );

        VBox heading = new VBox(7, eyebrow, title, subtitle);

        cpuValue = valueLabel("--");
        memoryValue = valueLabel("--");
        downloadValue = valueLabel("--");
        uploadValue = valueLabel("--");

        cpuChart = miniChart();
        memoryChart = miniChart();

        HBox cards = new HBox(12,
                metricCard("CPU UTILISATION", cpuValue, cpuChart),
                metricCard("MEMORY USAGE", memoryValue, memoryChart),
                speedCard("INTERNET DOWNLOAD", downloadValue, "↓", LIME),
                speedCard("INTERNET UPLOAD", uploadValue, "↑", "#42B8FF")
        );

        for (Node card : cards.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        VBox warningPanel = createWarningPanel();
        VBox storagePanel = createStoragePanel();
        VBox reportPanel = createReportPanel();

        content.getChildren().addAll(
                heading, cards, warningPanel, storagePanel, reportPanel
        );

        if (latestSnapshot != null) {
            Platform.runLater(() -> displaySnapshot(latestSnapshot));
        }

        updateSpeedCards();
        return content;
    }

    private VBox metricCard(String title, Label value, PieChart chart) {
        Label caption = caption(title);

        chart.setPrefSize(94, 94);
        chart.setMinSize(82, 82);
        chart.setMaxSize(100, 100);

        HBox visual = new HBox(8, chart, value);
        visual.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, caption, visual);
        card.setPadding(new Insets(15));
        card.setMinWidth(130);
        card.setPrefHeight(150);
        card.setStyle(panelStyle());
        return card;
    }

    private VBox speedCard(
            String title, Label value, String arrow, String colour) {

        Label caption = caption(title);

        Label arrowLabel = new Label(arrow);
        arrowLabel.setStyle(
                "-fx-font-size: 43px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + colour + ";"
        );

        value.setStyle(
                "-fx-font-size: 23px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );

        Label unit = new Label("Mbps");
        unit.setStyle(
                "-fx-font-size: 10px;" +
                "-fx-text-fill: " + MUTED + ";"
        );

        HBox reading = new HBox(12, arrowLabel, value);
        reading.setAlignment(Pos.CENTER_LEFT);

        VBox valueBlock = new VBox(3, reading, unit);

        VBox card = new VBox(12, caption, valueBlock);
        card.setPadding(new Insets(15));
        card.setMinWidth(130);
        card.setPrefHeight(150);
        card.setStyle(panelStyle());
        return card;
    }

    private PieChart miniChart() {
        PieChart chart = new PieChart();
        chart.setLegendVisible(false);
        chart.setLabelsVisible(false);
        chart.setAnimated(false);
        chart.setStartAngle(90);
        chart.setClockwise(true);
        chart.setPrefSize(94, 94);
        chart.setMinSize(82, 82);
        chart.setMaxSize(100, 100);
        chart.setStyle("-fx-background-color: transparent;");
        return chart;
    }

    private void setDonut(
            PieChart chart, double used, String usedColour) {

        double safeUsed = Math.max(0, Math.min(100, used));
        double remaining = 100 - safeUsed;

        chart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Used", safeUsed),
                new PieChart.Data("Remaining", remaining)
        ));

        applyPieColours(chart, List.of(usedColour, GREY));
    }

    private void applyPieColours(PieChart chart, List<String> colours) {
        for (int i = 0; i < chart.getData().size(); i++) {
            PieChart.Data slice = chart.getData().get(i);
            String colour = colours.get(i % colours.size());

            if (slice.getNode() != null) {
                slice.getNode().setStyle("-fx-pie-color: " + colour + ";");
            }

            slice.nodeProperty().addListener(
                    (observable, oldNode, newNode) -> {
                        if (newNode != null) {
                            newNode.setStyle(
                                    "-fx-pie-color: " + colour + ";"
                            );
                        }
                    }
            );
        }
    }

    private VBox createWarningPanel() {
        Label title = headingLabel("Warning centre");
        Label description = muted(
                "Suggested actions for metrics requiring attention."
        );

        warningList = new VBox(10);

        VBox panel = new VBox(14, title, description, warningList);
        panel.setPadding(new Insets(20));
        panel.setStyle(panelStyle());
        return panel;
    }

    private VBox createStoragePanel() {
        Label title = headingLabel("Storage utilisation");
        Label description = muted("Usage by detected volume");

        storageList = new VBox(10);

        VBox panel = new VBox(14, title, description, storageList);
        panel.setPadding(new Insets(20));
        panel.setStyle(panelStyle());
        return panel;
    }

    private VBox createReportPanel() {
        reportMessage = muted("Waiting for diagnostic results...");
        reportTimestamp = muted("No completed check yet");

        VBox panel = new VBox(
                12, headingLabel("Latest diagnostic"),
                reportMessage, reportTimestamp
        );
        panel.setPadding(new Insets(20));
        panel.setStyle(panelStyle());
        return panel;
    }

    private VBox createDiagnosticsPage() {
        VBox content = pageContainer();
        content.getChildren().addAll(
                headingLabel("Diagnostics"),
                muted("Results from the latest Master PC Check.")
        );

        if (latestSnapshot == null) {
            content.getChildren().add(muted("Results are not available yet."));
            return content;
        }

        content.getChildren().add(
                resultCard("CPU", latestSnapshot.getCpu())
        );
        content.getChildren().add(
                resultCard("Memory", latestSnapshot.getMemory())
        );

        for (DiagnosticResult result : latestSnapshot.getStorage()) {
            content.getChildren().add(resultCard(result.getMetric(), result));
        }

        return content;
    }


    private VBox createNetworkPage() {
        VBox content = pageContainer();
        content.getChildren().addAll(
                headingLabel("Network diagnostics"),
                muted("Internet performance and local network adapter details.")
        );

        latencyValue = valueLabel(formatLatency(latestLatency));
        VBox latencyCard = networkMetricCard(
                "AVERAGE HTTP LATENCY", latencyValue, "Response time"
        );
        Label download = valueLabel(formatSpeed(downloadMbps));
        VBox downloadCard = networkMetricCard(
                "DOWNLOAD SPEED", download, "Best successful test · Mbps"
        );
        Label upload = valueLabel(formatSpeed(uploadMbps));
        VBox uploadCard = networkMetricCard(
                "UPLOAD SPEED", upload, "Best successful test · Mbps"
        );

        HBox performanceCards = new HBox(12, latencyCard, downloadCard, uploadCard);
        for (Node card : performanceCards.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        networkTestSummary = muted(networkMessage);

        VBox latencyDetails = createLatencyDetails();
        VBox latencySection = collapsibleSection("HTTP latency samples", latencyDetails);
        VBox performancePanel = new VBox(
                14, headingLabel("Internet performance"), performanceCards,
                networkTestSummary, latencySection
        );
        performancePanel.setPadding(new Insets(20));
        performancePanel.setStyle(panelStyle());

        trafficList = new VBox(10);
        VBox trafficSection = collapsibleSection("Adapter traffic counters", trafficList);
        VBox trafficPanel = new VBox(
                14,
                headingLabel("Adapter traffic counters"),
                muted("Cumulative bytes received and sent, as reported by the OS."),
                trafficSection
        );
        trafficPanel.setPadding(new Insets(20));
        trafficPanel.setStyle(panelStyle());

        adapterList = new VBox(12);
        VBox adaptersSection = collapsibleSection("Detected network adapters", adapterList);
        VBox adapterPanel = new VBox(
                14,
                headingLabel("Network adapters"),
                muted("Includes physical and virtual interfaces detected by the OS."),
                adaptersSection
        );
        adapterPanel.setPadding(new Insets(20));
        adapterPanel.setStyle(panelStyle());

        content.getChildren().addAll(performancePanel, trafficPanel, adapterPanel);
        displayNetworkDetails();
        return content;
    }

    /** Creates a dark accordion row with a left chevron, like a modern settings list. */
    private VBox collapsibleSection(String titleText, Node body) {
        Label arrow = new Label("›");
        arrow.setMinWidth(18);
        arrow.setAlignment(Pos.CENTER);
        arrow.setStyle(
                "-fx-text-fill: " + LIME + ";" +
                "-fx-font-size: 25px;" +
                "-fx-font-weight: bold;"
        );

        Label title = new Label(titleText);
        title.setStyle(
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-font-size: 15px;" +
                "-fx-font-weight: bold;"
        );

        HBox header = new HBox(12, arrow, title);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(13, 16, 13, 16));
        header.setMaxWidth(Double.MAX_VALUE);
        header.setMinHeight(52);
        header.setStyle(
                "-fx-background-color: #202020;" +
                "-fx-background-radius: 7;" +
                "-fx-border-color: " + BORDER + ";" +
                "-fx-border-radius: 7;" +
                "-fx-cursor: hand;"
        );

        VBox bodyContainer = new VBox(body);
        bodyContainer.setPadding(new Insets(14, 4, 4, 4));
        bodyContainer.setVisible(false);
        bodyContainer.setManaged(false);

        Runnable toggle = () -> {
            boolean expand = !bodyContainer.isVisible();
            bodyContainer.setVisible(expand);
            bodyContainer.setManaged(expand);
            arrow.setText(expand ? "⌄" : "›");
            header.setStyle(
                    "-fx-background-color: " + (expand ? "#292929" : "#202020") + ";" +
                    "-fx-background-radius: 7;" +
                    "-fx-border-color: " + BORDER + ";" +
                    "-fx-border-radius: 7;" +
                    "-fx-cursor: hand;"
            );
        };

        header.setOnMouseClicked(event -> toggle.run());
        header.setOnMouseEntered(event -> {
            if (!bodyContainer.isVisible()) {
                header.setStyle(
                        "-fx-background-color: #292929;" +
                        "-fx-background-radius: 7;" +
                        "-fx-border-color: " + LIME + ";" +
                        "-fx-border-radius: 7;" +
                        "-fx-cursor: hand;"
                );
            }
        });
        header.setOnMouseExited(event -> {
            header.setStyle(
                    "-fx-background-color: " + (bodyContainer.isVisible() ? "#292929" : "#202020") + ";" +
                    "-fx-background-radius: 7;" +
                    "-fx-border-color: " + BORDER + ";" +
                    "-fx-border-radius: 7;" +
                    "-fx-cursor: hand;"
            );
        });

        VBox section = new VBox(8, header, bodyContainer);
        section.setFillWidth(true);
        return section;
    }

        private VBox networkMetricCard(
                String title, Label value, String description) {

            VBox card = new VBox(
                    10,
                    caption(title),
                    value,
                    muted(description)
            );

            card.setPadding(new Insets(16));
            card.setMinWidth(150);
            card.setStyle(panelStyle());

            return card;
        }

        private VBox createLatencyDetails() {
            VBox details = new VBox(7);

            details.getChildren().add(
                    headingLabel("HTTP latency samples")
            );

            if (latestLatency == null) {
                details.getChildren().add(
                        muted("Latency test has not completed.")
                );
                return details;
            }

            if (latestLatency.getSamples().isEmpty()) {
                details.getChildren().add(
                        muted("No successful latency measurements.")
                );
            } else {
                List<Long> samples = latestLatency.getSamples();

                for (int i = 0; i < samples.size(); i++) {
                    details.getChildren().add(
                            muted("Successful sample "
                                    + (i + 1) + ": " + samples.get(i) + " ms")
                    );
                }

                details.getChildren().add(
                        muted("Minimum: " + latestLatency.getMinimumMs()
                                + " ms  |  Maximum: "
                                + latestLatency.getMaximumMs() + " ms")
                );
            }

            if (!latestLatency.getFailures().isEmpty()) {
                details.getChildren().add(
                        muted("Failed requests: "
                                + latestLatency.getFailedCount())
                );
            }

            return details;
        }

        private void displayNetworkDetails() {
            if (latencyValue == null
                    || adapterList == null
                    || trafficList == null) {
                return;
            }

            latencyValue.setText(formatLatency(latestLatency));

            if (networkTestSummary != null) {
                networkTestSummary.setText(networkMessage);
            }

            adapterList.getChildren().clear();
            trafficList.getChildren().clear();

            if (latestAdapters.isEmpty()) {
                adapterList.getChildren().add(
                        muted("No adapter information available yet.")
                );
                trafficList.getChildren().add(
                        muted("No traffic counters available yet.")
                );
            } else {
                for (AdapterInfo adapter : latestAdapters) {
                    adapterList.getChildren().add(createAdapterCard(adapter));
                    trafficList.getChildren().add(createTrafficCard(adapter));
                }
            }
        }

        private VBox createAdapterCard(AdapterInfo adapter) {
            String status = adapter.isUp() ? "UP" : "DOWN";
            String statusColour = adapter.isUp() ? LIME : MUTED;

            VBox card = new VBox(
                    8,
                    headingLabel(adapter.getDisplayName()),
                    muted("Interface: " + adapter.getName()
                            + "  |  Status: " + status),
                    muted("Type: " + (adapter.isLoopback()
                            ? "Loopback"
                            : adapter.isVirtual() ? "Virtual / software"
                            : "Non-virtual interface")),
                    muted("MAC address: " + adapter.getMacAddress()),
                    muted("IPv4: " + adapter.getIpv4()),
                    muted("IPv6: " + adapter.getIpv6()),
                    muted("Reported link speed: "
                            + formatSpeed(adapter.getLinkSpeedMbps()))
            );

            card.getChildren().get(1).setStyle(
                    "-fx-text-fill: " + statusColour + "; -fx-font-size: 11px;"
            );

            card.setPadding(new Insets(15));
            card.setStyle(panelStyle());

            return card;
        }

        private VBox createTrafficCard(AdapterInfo adapter) {
            VBox card = new VBox(
                    7,
                    headingLabel(adapter.getDisplayName()),
                    muted("Received: "
                            + formatBytes(adapter.getReceivedBytes())),
                    muted("Sent: "
                            + formatBytes(adapter.getSentBytes()))
            );

            card.setPadding(new Insets(12));
            card.setStyle(panelStyle());

            return card;
        }

        private String formatLatency(LatencyResult result) {
            if (result == null || result.getAverageMs() < 0) {
                return "Unavailable";
            }

            return String.format("%.2f ms", result.getAverageMs());
        }

        private String formatSpeed(double speed) {
            if (!Double.isFinite(speed) || speed < 0) {
                return "Unavailable";
            }

            return String.format("%.2f Mbps", speed);
        }

        private String formatSpeed(long speedMbps) {
            return speedMbps < 0
                    ? "Unavailable"
                    : speedMbps + " Mbps";
        }

        private String formatBytes(long bytes) {
            if (bytes < 0) {
                return "Unavailable";
            }

            double value = bytes;
            String[] units = {"B", "KB", "MB", "GB", "TB"};
            int unit = 0;

            while (value >= 1024 && unit < units.length - 1) {
                value /= 1024;
                unit++;
            }

            return String.format("%.2f %s", value, units[unit]);
        }

    @SuppressWarnings("unused")
	private VBox speedDetail(
            String arrow, String title, double speed, String colour) {

        Label arrowLabel = new Label(arrow);
        arrowLabel.setStyle(
                "-fx-font-size: 36px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + colour + ";"
        );

        Label name = headingLabel(title);

        Label value = valueLabel(
                speed < 0 ? "Unavailable" : String.format("%.2f Mbps", speed)
        );

        VBox text = new VBox(7, name, value);
        HBox row = new HBox(18, arrowLabel, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(20));
        row.setStyle(panelStyle());

        VBox wrapper = new VBox(row);
        return wrapper;
    }

    private VBox resultCard(String title, DiagnosticResult result) {
        String reading = result.getStatus() == HealthStatus.UNAVAILABLE
                ? "Unavailable"
                : String.format("%.2f %s",
                        result.getValue(), result.getUnit());

        Label value = valueLabel(reading);
        Label status = muted(
                result.getStatus() + " — " + result.getMessage()
        );
        status.setStyle(
                "-fx-text-fill: " + statusColour(result.getStatus()) + ";"
        );

        VBox card = new VBox(10, headingLabel(title), value, status);
        card.setPadding(new Insets(18));
        card.setStyle(panelStyle());
        return card;
    }

    private VBox placeholderPage(String title, String description) {
        VBox content = pageContainer();
        content.getChildren().addAll(
                headingLabel(title), muted(description),
                muted("This section will be implemented in a later step.")
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
                    if (reportMessage != null) {
                        reportMessage.setText(
                                "Diagnostics failed: " + exception.getMessage()
                        );
                    }
                });
            }
        }, "PC-DOQTAR-Initial-Check");

        thread.setDaemon(true);
        thread.start();

     // Run adapter discovery, HTTP latency and the speed test once.
        Thread speedThread = new Thread(() -> {

            // Collect local network-interface information.
            try {
                List<AdapterInfo> adapters =
                        networkDiagnostics.collectAdapters();

                Platform.runLater(() -> {
                    latestAdapters = adapters;
                    displayNetworkDetails();
                });

            } catch (Exception exception) {
                Platform.runLater(() -> {
                    networkMessage = "Adapter discovery failed: "
                            + exception.getMessage();
                    displayNetworkDetails();
                });
            }

            // Measure HTTP latency separately so a speed-test failure
            // does not discard successful latency results.
            try {
                LatencyResult latency =
                        networkTest.measureLatencySamples();

                Platform.runLater(() -> {
                    latestLatency = latency;
                    displayNetworkDetails();
                });

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                Platform.runLater(() -> {
                    networkMessage = "Latency test interrupted.";
                    displayNetworkDetails();
                });
                return;
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    networkMessage = "Latency test failed: "
                            + exception.getMessage();
                    displayNetworkDetails();
                });
            }

            // Run the existing best-of-five download/upload measurements.
            try {
                double[] speeds = networkTest.measureSpeeds();

                Platform.runLater(() -> {
                    downloadMbps = speeds[0];
                    uploadMbps = speeds[1];
                    networkMessage = "Internet performance test completed.";
                    updateSpeedCards();
                    displayNetworkDetails();
                });

            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                Platform.runLater(() -> {
                    networkMessage = "Speed test interrupted.";
                    displayNetworkDetails();
                });

            } catch (Exception exception) {
                Platform.runLater(() -> {
                    networkMessage = "Speed test failed: "
                            + exception.getMessage();
                    displayNetworkDetails();
                });
            }

        }, "PC-DOQTAR-Network-Test");

        speedThread.setDaemon(true);
        speedThread.start();

    }

    private void updateSpeedCards() {
        if (downloadValue != null) {
            downloadValue.setText(
                    downloadMbps < 0 ? "--"
                            : String.format("%.1f", downloadMbps)
            );
        }

        if (uploadValue != null) {
            uploadValue.setText(
                    uploadMbps < 0 ? "--"
                            : String.format("%.1f", uploadMbps)
            );
        }
    }

    private void displaySnapshot(SystemHealthSnapshot snapshot) {
        if (cpuValue == null || warningList == null || storageList == null) {
            return;
        }

        DiagnosticResult cpu = snapshot.getCpu();
        DiagnosticResult memory = snapshot.getMemory();
        List<DiagnosticResult> storage = snapshot.getStorage();

        cpuValue.setText(formatPercentage(cpu));
        memoryValue.setText(formatPercentage(memory));

        setDonut(cpuChart, cpu.getValue(), statusColour(cpu.getStatus()));
        setDonut(memoryChart, memory.getValue(),
                statusColour(memory.getStatus()));

        populateWarnings(snapshot);
        populateStorage(storage);

        reportMessage.setText(
                "Overall status: " + snapshot.getOverallStatus()
                + "   |   Storage volumes: " + storage.size()
                + (snapshot.hasUnavailableResults()
                        ? "\nSome readings were unavailable." : "")
        );

        reportMessage.setStyle(
                "-fx-text-fill: "
                        + statusColour(snapshot.getOverallStatus()) + ";"
        );

        reportTimestamp.setText(
                "Completed  •  " + snapshot.getTimestamp()
        );
    }

    private void populateWarnings(SystemHealthSnapshot snapshot) {
        warningList.getChildren().clear();

        List<DiagnosticResult> results = new ArrayList<>();
        results.add(snapshot.getCpu());
        results.add(snapshot.getMemory());
        results.addAll(snapshot.getStorage());

        int visible = 0;

        for (DiagnosticResult result : results) {
            if (!needsAttention(result)
                    || ignoredWarnings.contains(result.getMetric())) {
                continue;
            }

            warningList.getChildren().add(createWarningCard(result));
            visible++;
        }

        if (visible == 0) {
            warningList.getChildren().add(muted(
                    ignoredWarnings.isEmpty()
                            ? "No active warnings. Checked metrics look good."
                            : "No visible warnings. Some have been ignored for this session."
            ));
        }
    }

    private VBox createWarningCard(DiagnosticResult result) {
        String colour = statusColour(result.getStatus());

        Label name = headingLabel(result.getMetric());
        Label value = new Label(
                result.getStatus() + "  •  "
                + String.format("%.2f %s", result.getValue(), result.getUnit())
        );
        value.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + colour + ";"
        );

        Label message = muted(result.getMessage());

        Button fix = new Button("How to fix");
        fix.setStyle(
                "-fx-background-color: " + LIME + ";" +
                "-fx-text-fill: #101010;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 5;" +
                "-fx-padding: 8 12 8 12;"
        );
        fix.setOnAction(event -> showSuggestion(result));

        Button ignore = new Button("Ignore warning");
        ignore.setStyle(
                "-fx-background-color: #242424;" +
                "-fx-text-fill: " + TEXT + ";" +
                "-fx-border-color: #414141;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;" +
                "-fx-padding: 8 12 8 12;"
        );
        ignore.setOnAction(event -> {
            ignoredWarnings.add(result.getMetric());
            if (latestSnapshot != null) {
                populateWarnings(latestSnapshot);
            }
        });

        HBox buttons = new HBox(9, fix, ignore);

        VBox card = new VBox(10, name, value, message, buttons);
        card.setPadding(new Insets(15));
        card.setStyle(
                "-fx-background-color: #101010;" +
                "-fx-background-radius: 7;" +
                "-fx-border-color: " + panelTint(colour) + ";" +
                "-fx-border-radius: 7;"
        );
        return card;
    }

    private void showSuggestion(DiagnosticResult result) {
        String metric = result.getMetric().toLowerCase();
        String suggestion;

        if (metric.contains("cpu")) {
            suggestion =
                    "1. Open Task Manager and sort by CPU usage.\n"
                    + "2. Close applications you do not need.\n"
                    + "3. Let intensive tasks finish, then check again.\n"
                    + "4. If usage remains high while idle, inspect background processes.";
        } else if (metric.contains("memory") || metric.contains("ram")) {
            suggestion =
                    "1. Open Task Manager and sort by memory usage.\n"
                    + "2. Save work and close unnecessary applications or tabs.\n"
                    + "3. Check for applications using unusually large amounts of RAM.\n"
                    + "4. Run the check again after usage settles.";
        } else if (metric.contains("storage")) {
            suggestion =
                    "1. Open the affected volume in File Explorer.\n"
                    + "2. Use Windows Storage settings to find large or temporary files.\n"
                    + "3. Move personal files or uninstall unused applications.\n"
                    + "4. Check Recycle Bin contents before emptying it.\n"
                    + "5. Run the diagnostic again.";
        } else {
            suggestion =
                    "Review the diagnostic result, inspect the affected component, "
                    + "apply a suitable corrective action and test again.";
        }

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("PC DOQTAR — Recommended action");
        dialog.setHeaderText(
                result.getMetric() + " — " + result.getStatus()
        );
        dialog.setContentText(
                "Current reading: "
                + result.getValue() + " " + result.getUnit()
                + "\n\nSuggested steps:\n" + suggestion
                + "\n\nIgnoring a warning hides the card for this session only. "
                + "It does not change the actual health result."
        );
        dialog.getDialogPane().setMinWidth(480);
        dialog.showAndWait();
    }

    private void populateStorage(List<DiagnosticResult> storage) {
        storageList.getChildren().clear();

        for (DiagnosticResult result : storage) {
            if (result.getStatus() == HealthStatus.UNAVAILABLE) {
                continue;
            }

            Label name = muted(result.getMetric());
            Label percentage = new Label(
                    String.format("%.1f%%", result.getValue())
            );
            percentage.setStyle(
                    "-fx-text-fill: " + statusColour(result.getStatus()) + ";" +
                    "-fx-font-weight: bold;"
            );

            HBox header = new HBox(10, name, new Region(), percentage);
            header.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

            Region fill = new Region();
            fill.setPrefHeight(6);
            fill.setMaxHeight(6);
            fill.setStyle(
                    "-fx-background-color: " + statusColour(result.getStatus()) + ";" +
                    "-fx-background-radius: 4;"
            );

            HBox bar = new HBox(fill);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setPrefHeight(6);
            HBox.setHgrow(bar, Priority.ALWAYS);

            fill.prefWidthProperty().bind(
                    bar.widthProperty().multiply(
                            Math.max(0, Math.min(100, result.getValue())) / 100.0
                    )
            );

            VBox item = new VBox(8, header, bar);
            item.setPadding(new Insets(12));
            item.setStyle(
                    "-fx-background-color: #101010;" +
                    "-fx-background-radius: 6;"
            );

            storageList.getChildren().add(item);
        }
    }

    private boolean needsAttention(DiagnosticResult result) {
        return result.getStatus() == HealthStatus.WARNING
                || result.getStatus() == HealthStatus.CRITICAL;
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

    private String panelTint(String colour) {
        if (RED.equals(colour)) return "#321919";
        if (AMBER.equals(colour)) return "#332713";
        if (LIME.equals(colour)) return "#222C14";
        return "#242424";
    }

    private VBox pageContainer() {
        VBox box = new VBox(18);
        box.setPadding(new Insets(28));
        box.setStyle("-fx-background-color: " + BG + ";");
        return box;
    }

    private Label headingLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );
        return label;
    }

    private Label valueLabel(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + TEXT + ";"
        );
        return label;
    }

    private Label caption(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 12px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + MUTED + ";"
        );
        return label;
    }

    private Label muted(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: " + MUTED + ";"
        );
        return label;
    }

    private String panelStyle() {
        return "-fx-background-color: " + PANEL + ";" +
               "-fx-background-radius: 8;" +
               "-fx-border-color: " + BORDER + ";" +
               "-fx-border-radius: 8;";
    }

    @Override
    public void stop() {
        // No recurring monitor is started.
    }

    public static void main(String[] args) {
        launch(args);
    }
}
