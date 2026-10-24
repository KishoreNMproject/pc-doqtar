package org.pcdoqtar.ui;

import java.util.List;

import org.pcdoqtar.diagnostics.network.NetworkDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics.AdapterInfo;
import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class NetworkPage extends VBox {

    private final NetworkDiagnostics networkDiagnostics;
    private final VBox adapterContainer = new VBox(8);
    private final Label statusLabel = new Label("Ready");
    private boolean refreshRunning;

    public NetworkPage() { this(new NetworkDiagnostics()); }

    public NetworkPage(NetworkDiagnostics networkDiagnostics) {
        if (networkDiagnostics == null) {
            throw new IllegalArgumentException("Network diagnostics must not be null.");
        }
        this.networkDiagnostics = networkDiagnostics;
        setSpacing(18);
        setPadding(new Insets(28));
        setStyle("-fx-background-color: " + ThemeManager.getColour("background") + ";");

        Label eyebrow = new Label("SYSTEM MONITORING / NETWORK");
        eyebrow.setStyle("-fx-text-fill: " + ThemeManager.getColour("accent") + ";");
        Label heading = new Label("Network");
        heading.setStyle("-fx-text-fill: " + ThemeManager.getColour("text")
                + ";-fx-font-size: 30px;-fx-font-weight: bold;");
        Label description = new Label(
                "Inspect local network interfaces and their current status. "
                + "Expand an adapter to view its details.");
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: " + ThemeManager.getColour("muted") + ";");

        Button refreshButton = new Button("Refresh adapters");
        refreshButton.getStyleClass().add("primary-action");
        refreshButton.setOnAction(event -> refreshAdapters());

        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: " + ThemeManager.getColour("muted") + ";");
        adapterContainer.setFillWidth(true);

        getChildren().addAll(eyebrow, heading, description, refreshButton,
                statusLabel, new Separator(), adapterContainer);
        VBox.setVgrow(adapterContainer, Priority.ALWAYS);
        refreshAdapters();
    }

    public void displayAdapters(List<AdapterInfo> adapters) {
        adapterContainer.getChildren().clear();
        if (adapters == null || adapters.isEmpty()) {
            adapterContainer.getChildren().add(new Label("No network interfaces were detected."));
            statusLabel.setText("No adapter data available.");
            return;
        }

        long activeCount = adapters.stream()
                .filter(AdapterInfo::isUp).filter(adapter -> !adapter.isLoopback()).count();
        statusLabel.setText("Interfaces found: " + adapters.size()
                + " · Active non-loopback interfaces: " + activeCount);
        for (AdapterInfo adapter : adapters) {
            adapterContainer.getChildren().add(createAdapterPane(adapter));
        }
    }

    private TitledPane createAdapterPane(AdapterInfo adapter) {
        String name = adapter.getDisplayName() == null || adapter.getDisplayName().isBlank()
                ? adapter.getName() : adapter.getDisplayName();
        String state = adapter.isUp() ? "UP" : "DOWN";
        String kind = adapter.isVirtual() ? "Virtual"
                : adapter.isLoopback() ? "Loopback" : "Physical";

        Label summary = new Label(state + "  ·  " + kind + "  ·  " + adapter.getName());
        summary.setWrapText(true);
        summary.setStyle("-fx-text-fill: " + (adapter.isUp()
                ? ThemeManager.getColour("accent") : ThemeManager.getColour("warning")) + ";");

        Label details = new Label("Interface: " + adapter.getName()
                + "\nIPv4: " + adapter.getIpv4()
                + "\nIPv6: " + adapter.getIpv6()
                + "\nMAC address: " + adapter.getMacAddress()
                + "\nLink speed: " + formatSpeed(adapter.getLinkSpeedMbps())
                + "\nReceived: " + formatBytes(adapter.getReceivedBytes())
                + "\nSent: " + formatBytes(adapter.getSentBytes()));
        details.setWrapText(true);
        details.setStyle("-fx-text-fill: " + ThemeManager.getColour("muted") + ";");

        VBox content = new VBox(10, summary, details);
        content.setPadding(new Insets(12));
        content.setStyle("-fx-background-color: " + ThemeManager.getColour("panel") + ";");

        TitledPane pane = new TitledPane(name, content);
        pane.getStyleClass().add("network-adapter");
        pane.setExpanded(false);
        pane.setAnimated(false);
        pane.setMaxWidth(Double.MAX_VALUE);
        return pane;
    }

    private String formatSpeed(long speedMbps) {
        return speedMbps < 0 ? "Unavailable" : speedMbps + " Mbps";
    }

    private String formatBytes(long bytes) {
        return bytes < 0 ? "Unavailable"
                : String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private void refreshAdapters() {
        if (refreshRunning) return;
        refreshRunning = true;
        statusLabel.setText("Inspecting network interfaces...");
        adapterContainer.getChildren().clear();

        Thread worker = new Thread(() -> {
            try {
                List<AdapterInfo> adapters = networkDiagnostics.collectAdapters();
                Platform.runLater(() -> displayAdapters(adapters));
            } catch (Exception exception) {
                Platform.runLater(() -> {
                    adapterContainer.getChildren().clear();
                    Label error = new Label("Unable to inspect interfaces: " + exception.getMessage());
                    error.setWrapText(true);
                    adapterContainer.getChildren().add(error);
                    statusLabel.setText("Network inspection failed.");
                });
            } finally {
                Platform.runLater(() -> refreshRunning = false);
            }
        }, "PC-DOQTAR-Network-Scan");
        worker.setDaemon(true);
        worker.start();
    }
}