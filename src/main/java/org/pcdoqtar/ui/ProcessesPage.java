
package org.pcdoqtar.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.pcdoqtar.ui.theme.ThemeManager;

public class ProcessesPage extends VBox {

    private static final Set<String> PROTECTED_PROCESS_NAMES = Set.of(
            "system", "registry", "smss.exe", "csrss.exe", "wininit.exe",
            "services.exe", "lsass.exe", "winlogon.exe", "svchost.exe",
            "fontdrvhost.exe", "secure system", "memory compression");

    private final OperatingSystem operatingSystem =
            new SystemInfo().getOperatingSystem();

    private final ObservableList<ProcessRow> allProcesses =
            FXCollections.observableArrayList();

    private final TableView<ProcessRow> table = new TableView<>();
    private final Label processCount = new Label("--");
    private final Label topConsumer = new Label("Waiting for scan");
    private final Label status = new Label("Ready");
    private final TextField searchField = new TextField();
    private final Button scanButton = new Button("Scan processes");
    private final Button endTaskButton = new Button("End task");

    public ProcessesPage() {
        getStyleClass().add("processes-page");
        setSpacing(20);
        setPadding(new Insets(28));
        setStyle("-fx-background-color: " + ThemeManager.getColour("background") + ";");

        Label eyebrow = new Label("SYSTEM MONITORING / PROCESSES");
        eyebrow.setStyle(
                "-fx-text-fill: " + ThemeManager.getColour("accent") + ";" +
                "-fx-font-size: 10px;"
        );

        Label heading = new Label("Process Manager");
        heading.setStyle(
                "-fx-text-fill: " + ThemeManager.getColour("text") + ";" +
                "-fx-font-size: 30px;" +
                "-fx-font-weight: bold;"
        );

        Label description = new Label(
                "Inspect running processes and identify resource-heavy applications."
        );
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: " + ThemeManager.getColour("muted") + ";");

        VBox titleBlock = new VBox(7, eyebrow, heading, description);

        HBox summaryCards = new HBox(12,
                summaryCard("PROCESSES FOUND", processCount),
                summaryCard("HIGHEST RAM CONSUMER", topConsumer)
        );

        HBox.setHgrow(summaryCards.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(summaryCards.getChildren().get(1), Priority.ALWAYS);

        searchField.setPromptText("Search by process name or PID...");
        searchField.setStyle(
                "-fx-background-color: #101010;" +
                "-fx-text-fill: " + ThemeManager.getColour("text") + ";" +
                "-fx-prompt-text-fill: #666666;" +
                "-fx-border-color: " + ThemeManager.getColour("border") + ";" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;" +
                "-fx-padding: 10;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener(
                (obs, oldValue, newValue) -> filterProcesses()
        );

        scanButton.setStyle(
                "-fx-background-color: " + ThemeManager.getColour("accent") + ";" +
                "-fx-text-fill: #101010;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 5;" +
                "-fx-padding: 10 15 10 15;" +
                "-fx-cursor: hand;"
        );
        scanButton.setOnAction(event -> scanProcesses());

        endTaskButton.setOnAction(event -> endSelectedTask());
        endTaskButton.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) ->
                endTaskButton.setDisable(newRow == null));
        HBox toolbar = new HBox(12, searchField, scanButton, endTaskButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        configureTable();

        VBox tablePanel = new VBox(14,
                toolbar, table, status
        );
        tablePanel.setPadding(new Insets(18));
        tablePanel.getStyleClass().add("app-tile");

        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        table.setMinHeight(300);
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        getChildren().addAll(titleBlock, summaryCards, tablePanel);

        scanProcesses();
    }

    private VBox summaryCard(String title, Label value) {
        Label caption = new Label(title);
        caption.setStyle(
                "-fx-text-fill: " + ThemeManager.getColour("muted") + ";" +
                "-fx-font-size: 9px;" +
                "-fx-font-weight: bold;"
        );

        value.setStyle(
                "-fx-text-fill: " + ThemeManager.getColour("text") + ";" +
                "-fx-font-size: 20px;" +
                "-fx-font-weight: bold;"
        );
        value.setWrapText(true);

        VBox card = new VBox(12, caption, value);
        card.setPadding(new Insets(17));
        card.setMinHeight(90);
        card.getStyleClass().add("app-tile");

        return card;
    }

    @SuppressWarnings("unchecked")
	private void configureTable() {
        TableColumn<ProcessRow, Integer> pidColumn =
                new TableColumn<>("PID");
        pidColumn.setCellValueFactory(
                new PropertyValueFactory<>("pid")
        );
        pidColumn.setMaxWidth(100);

        TableColumn<ProcessRow, String> nameColumn =
                new TableColumn<>("PROCESS");
        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );
        nameColumn.setMinWidth(180);

        TableColumn<ProcessRow, String> ramColumn =
                new TableColumn<>("MEMORY");
        ramColumn.setCellValueFactory(
                new PropertyValueFactory<>("ram")
        );
        ramColumn.setMinWidth(110);

        TableColumn<ProcessRow, String> cpuColumn =
                new TableColumn<>("CPU");
        cpuColumn.setCellValueFactory(
                new PropertyValueFactory<>("cpu")
        );
        cpuColumn.setMinWidth(90);

        table.getColumns().setAll(
                pidColumn, nameColumn, ramColumn, cpuColumn
        );

        table.setItems(allProcesses);
        table.setPlaceholder(new Label("No matching processes."));
        table.setStyle(
                "-fx-background-color: " + ThemeManager.getColour("panel") + ";" +
                "-fx-control-inner-background: #101010;" +
                "-fx-control-inner-background-alt: #151515;" +
                "-fx-text-background-color: " + ThemeManager.getColour("text") + ";" +
                "-fx-background-radius: 5;"
        );

        table.setFixedCellSize(39);
    }

    private void endSelectedTask() {
        ProcessRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status.setText("Select a process before choosing End task.");
            return;
        }
        long pid = selected.getPid();
        String normalizedName = selected.getName().toLowerCase(Locale.ROOT);
        if (pid <= 4 || pid == ProcessHandle.current().pid()
                || PROTECTED_PROCESS_NAMES.contains(normalizedName)) {
            status.setText("For safety, PC DOQTAR will not terminate this protected Windows process.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm end task");
        confirmation.setHeaderText("End " + selected.getName() + "?");
        confirmation.setContentText("This requests process termination for PID " + pid
                + ". Unsaved work in that application may be lost.");
        confirmation.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        confirmation.showAndWait().filter(button -> button == ButtonType.OK)
                .ifPresent(button -> {
                    endTaskButton.setDisable(true);
                    status.setText("Requesting termination of " + selected.getName() + "...");
                    Thread worker = new Thread(() -> {
                        String result;
                        try {
                            ProcessHandle process = ProcessHandle.of(pid).orElse(null);
                            if (process == null || !process.isAlive()) {
                                result = "Process is no longer running.";
                            } else if (process.destroy()) {
                                result = "Termination requested for PID " + pid
                                        + ". Rescan to confirm it has exited.";
                            } else {
                                result = "Windows denied termination. Try running PC DOQTAR with appropriate permissions.";
                            }
                        } catch (SecurityException exception) {
                            result = "Termination denied by the operating system: "
                                    + exception.getMessage();
                        } catch (Exception exception) {
                            result = "Unable to end task: " + exception.getMessage();
                        }
                        String finalResult = result;
                        Platform.runLater(() -> {
                            status.setText(finalResult);
                            endTaskButton.setDisable(table.getSelectionModel().getSelectedItem() == null);
                        });
                    }, "PC-DOQTAR-End-Task");
                    worker.setDaemon(true);
                    worker.start();
                });
    }

    private void scanProcesses() {
        scanButton.setDisable(true);
        status.setText("Scanning local processes...");
        status.setStyle("-fx-text-fill: " + ThemeManager.getColour("accent") + ";");

        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(18, 18);
        progress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        scanButton.setGraphic(progress);

        Thread worker = new Thread(() -> {
            try {
                List<OSProcess> processes = operatingSystem.getProcesses(
                        null,
                        Comparator.comparingLong(
                                OSProcess::getResidentMemory
                        ).reversed(),
                        200
                );

                List<ProcessRow> rows = processes.stream()
                        .map(ProcessRow::from)
                        .toList();

                Platform.runLater(() -> {
                    allProcesses.setAll(rows);
                    filterProcesses();

                    processCount.setText(
                            Integer.toString(rows.size())
                    );

                    if (rows.isEmpty()) {
                        topConsumer.setText("No data");
                    } else {
                        ProcessRow largest = rows.get(0);
                        topConsumer.setText(
                                largest.getName() + " · " + largest.getRam()
                        );
                    }

                    status.setText(
                            "Scan complete · " + rows.size()
                                    + " processes retrieved · sorted by RAM"
                    );
                    status.setStyle("-fx-text-fill: " + ThemeManager.getColour("muted") + ";");

                    scanButton.setDisable(false);
                    scanButton.setGraphic(null);
                });

            } catch (Exception exception) {
                Platform.runLater(() -> {
                    status.setText(
                            "Scan failed: " + exception.getMessage()
                    );
                    status.setStyle("-fx-text-fill: " + ThemeManager.getColour("critical") + ";");
                    scanButton.setDisable(false);
                    scanButton.setGraphic(null);
                });
            }
        }, "PC-DOQTAR-Process-Scan");

        worker.setDaemon(true);
        worker.start();
    }

    private void filterProcesses() {
        String query = searchField.getText();

        if (query == null || query.isBlank()) {
            table.setItems(FXCollections.observableArrayList(allProcesses));
            return;
        }

        String normalized = query.trim().toLowerCase(Locale.ROOT);

        ObservableList<ProcessRow> filtered =
                FXCollections.observableArrayList();

        for (ProcessRow process : allProcesses) {
            if (process.getName().toLowerCase(Locale.ROOT)
                    .contains(normalized)
                    || Integer.toString(process.getPid())
                    .contains(normalized)) {
                filtered.add(process);
            }
        }

        table.setItems(filtered);
    }

    public static class ProcessRow {
        private final int pid;
        private final String name;
        private final String ram;
        private final String cpu;

        public ProcessRow(int pid, String name, String ram, String cpu) {
            this.pid = pid;
            this.name = name;
            this.ram = ram;
            this.cpu = cpu;
        }

        public static ProcessRow from(OSProcess process) {
            double ramMb =
                    process.getResidentMemory() / (1024.0 * 1024.0);

            // OSHI cumulative process CPU can exceed 100% on multicore
            // systems. Normalise it to the whole-machine CPU capacity.
            double cpuPercent =
                    process.getProcessCpuLoadCumulative()
                            * 100.0
                            / Math.max(1,
                                    Runtime.getRuntime()
                                            .availableProcessors());

            cpuPercent = Math.max(0, Math.min(100, cpuPercent));

            return new ProcessRow(
                    process.getProcessID(),
                    process.getName() == null
                            ? "Unknown" : process.getName(),
                    String.format("%.1f MB", ramMb),
                    String.format("%.1f%%", cpuPercent)
            );
        }

        public int getPid() {
            return pid;
        }

        public String getName() {
            return name;
        }

        public String getRam() {
            return ram;
        }

        public String getCpu() {
            return cpu;
        }
    }
}
