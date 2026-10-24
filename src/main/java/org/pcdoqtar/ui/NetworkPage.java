package org.pcdoqtar.ui;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Locale;

import org.pcdoqtar.diagnostics.network.NetworkDiagnostics;
import org.pcdoqtar.diagnostics.network.NetworkDiagnostics.AdapterInfo;
import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

@SuppressWarnings("unused")
public class NetworkPage extends VBox {

    private final NetworkDiagnostics networkDiagnostics;
    private final VBox interfaceContainer = new VBox(10);
    private final Label connectionStatus = new Label("Checking connectivity...");
    private final Label interfaceSummary = new Label("Waiting for interface scan...");
    private final Label transferSummary = new Label("Traffic totals will appear after scanning.");
    private final Button refreshButton = new Button("Run network diagnostics");
    private boolean refreshRunning;

    public NetworkPage() { this(new NetworkDiagnostics()); }

    public NetworkPage(NetworkDiagnostics networkDiagnostics) {
        if (networkDiagnostics == null) {
            throw new IllegalArgumentException("Network diagnostics must not be null.");
        }
        this.networkDiagnostics = networkDiagnostics;
        getStyleClass().add("network-page");
        setSpacing(18);
        setPadding(new Insets(28));

        Label eyebrow = new Label("SYSTEM MONITORING / NETWORK");
        eyebrow.getStyleClass().add("page-eyebrow");
        Label heading = new Label("Network diagnostics");
        heading.getStyleClass().add("page-heading");
        Label description = new Label(
                "Review connectivity, active non-virtual interfaces, IP addressing, link speed and traffic counters. "
                + "Windows filter, loopback and virtual adapters are omitted from the main list.");
        description.setWrapText(true);
        description.getStyleClass().add("page-description");

        GridPane summary = new GridPane();
        summary.getStyleClass().add("diagnostic-summary");
        summary.setHgap(24);
        summary.setVgap(10);
        Label connectionTitle = new Label("INTERNET REACHABILITY");
        Label interfaceTitle = new Label("ACTIVE INTERFACES");
        Label trafficTitle = new Label("TOTAL TRAFFIC");
        connectionTitle.getStyleClass().add("summary-caption");
        interfaceTitle.getStyleClass().add("summary-caption");
        trafficTitle.getStyleClass().add("summary-caption");
        connectionStatus.getStyleClass().add("summary-value");
        interfaceSummary.getStyleClass().add("summary-value");
        transferSummary.getStyleClass().add("summary-value");
        summary.addColumn(0, connectionTitle, connectionStatus);
        summary.addColumn(1, interfaceTitle, interfaceSummary);
        summary.addColumn(2, trafficTitle, transferSummary);
        GridPane.setHgrow(connectionStatus, Priority.ALWAYS);

        refreshButton.getStyleClass().add("primary-action");
        refreshButton.setOnAction(event -> refresh());
        Label section = new Label("Available network interfaces");
        section.getStyleClass().add("section-heading");
        getChildren().addAll(eyebrow, heading, description, summary,
                refreshButton, section, interfaceContainer);
        VBox.setVgrow(interfaceContainer, Priority.ALWAYS);
        refresh();
    }

    private void refresh() {
        if (refreshRunning) return;
        refreshRunning = true;
        refreshButton.setDisable(true);
        connectionStatus.setText("Testing...");
        interfaceSummary.setText("Scanning...");
        transferSummary.setText("Calculating...");
        interfaceContainer.getChildren().clear();

        Thread worker = new Thread(() -> {
            List<AdapterInfo> adapters;
            try {
                adapters = networkDiagnostics.collectAdapters();
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    interfaceContainer.getChildren().setAll(
                            new Label("Interface scan failed: " + ex.getMessage()));
                    connectionStatus.setText("Unavailable");
                    interfaceSummary.setText("Unavailable");
                    transferSummary.setText("Unavailable");
                    finishRefresh();
                });
                return;
            }
            boolean internet = testInternetReachability();
            Platform.runLater(() -> {
                List<AdapterInfo> visible = adapters.stream()
                        .filter(this::isUsefulInterface)
                        .filter(AdapterInfo::isUp)
                        .toList();
                if (visible.isEmpty()) {
                    visible = adapters.stream()
                            .filter(AdapterInfo::isUp)
                            .filter(a -> !a.isLoopback() && !a.isVirtual())
                            .toList();
                }
                connectionStatus.setText(internet ? "Reachable" : "No response");
                interfaceSummary.setText(visible.size() + " active");
                long rx = visible.stream().mapToLong(AdapterInfo::getReceivedBytes)
                        .filter(v -> v >= 0).sum();
                long tx = visible.stream().mapToLong(AdapterInfo::getSentBytes)
                        .filter(v -> v >= 0).sum();
                transferSummary.setText("↓ " + formatBytes(rx) + "  ↑ " + formatBytes(tx));
                interfaceContainer.getChildren().clear();
                if (visible.isEmpty()) {
                    Label empty = new Label("No active physical interfaces were found. Check that Wi-Fi or Ethernet is enabled.");
                    empty.getStyleClass().add("diagnostic-detail");
                    interfaceContainer.getChildren().add(empty);
                } else {
                    for (AdapterInfo adapter : visible) {
                        interfaceContainer.getChildren().add(interfaceCard(adapter));
                    }
                }
                finishRefresh();
            });
        }, "PC-DOQTAR-Network-Diagnostics");
        worker.setDaemon(true);
        worker.start();
    }

    private boolean isUsefulInterface(AdapterInfo adapter) {
        if (!adapter.isUp() || adapter.isLoopback() || adapter.isVirtual()) return false;
        String name = ((adapter.getDisplayName() == null ? "" : adapter.getDisplayName())
                + " " + adapter.getName()).toLowerCase(Locale.ROOT);
        return !(name.contains("filter") || name.contains("wfp")
                || name.contains("qos packet") || name.contains("ndis")
                || name.contains("loopback") || name.contains("virtualbox")
                || name.contains("hyper-v") || name.contains("hyperv")
                || name.contains("vethernet") || name.contains("pseudo"));
    }

    private boolean testInternetReachability() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("1.1.1.1", 443), 1200);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private VBox interfaceCard(AdapterInfo adapter) {
        String name = adapter.getDisplayName() == null || adapter.getDisplayName().isBlank()
                ? adapter.getName() : adapter.getDisplayName();
        Label title = new Label(name);
        title.getStyleClass().add("interface-title");
        Label state = new Label("● Connected  ·  " + formatSpeed(adapter.getLinkSpeedMbps()));
        state.getStyleClass().add("interface-state");
        Label details = new Label("IPv4: " + adapter.getIpv4()
                + "\nIPv6: " + adapter.getIpv6()
                + "\nMAC: " + adapter.getMacAddress()
                + "\nReceived: " + formatBytes(adapter.getReceivedBytes())
                + "\nSent: " + formatBytes(adapter.getSentBytes()));
        details.setWrapText(true);
        details.getStyleClass().add("diagnostic-detail");
        VBox card = new VBox(8, title, state, details);
        card.getStyleClass().add("interface-card");
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void finishRefresh() {
        refreshRunning = false;
        refreshButton.setDisable(false);
    }

    private String formatSpeed(long speed) {
        return speed < 0 ? "Unavailable" : speed + " Mbps";
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "Unavailable";
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
