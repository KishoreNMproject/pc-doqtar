package org.pcdoqtar.ota;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Modern themed dialog prompting the user with release notes,
 * download progress, and one-click installation for updates.
 */
public final class UpdateDialog extends Stage {

    private static final Logger LOGGER = Logger.getLogger(UpdateDialog.class.getName());

    private final UpdateInfo updateInfo;
    private final UpdateService updateService;

    private final ProgressBar progressBar = new ProgressBar(0.0);
    private final Label progressLabel = new Label();
    private final Button actionButton = new Button("Update Now");
    private final Button secondaryButton = new Button("Remind Me Later");
    private final Button githubButton = new Button("View on GitHub");

    private final AtomicBoolean cancelFlag = new AtomicBoolean(false);
    private Path downloadedInstaller = null;

    public static void showUpdateAvailable(Window owner, UpdateInfo info, UpdateService service) {
        UpdateDialog dialog = new UpdateDialog(owner, info, service);
        dialog.show();
    }

    public UpdateDialog(Window owner, UpdateInfo info, UpdateService service) {
        this.updateInfo = info;
        this.updateService = service;

        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            initOwner(owner);
        }

        setTitle("PC DOQTAR — Update Available");
        setMinWidth(520);
        setMinHeight(460);
        setMaxWidth(600);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: " + ThemeManager.getColour("background")
                + "; -fx-border-color: " + ThemeManager.getColour("border")
                + "; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        // Eyebrow
        Label eyebrow = new Label("SOFTWARE UPDATE AVAILABLE");
        eyebrow.setStyle("-fx-text-fill: " + ThemeManager.getColour("accent")
                + "; -fx-font-size: 11px; -fx-font-weight: bold;");

        // Header Title
        Label title = new Label("PC DOQTAR " + info.tagName());
        title.setStyle("-fx-text-fill: " + ThemeManager.getColour("text")
                + "; -fx-font-size: 22px; -fx-font-weight: bold;");

        // Version badge line
        String currentVer = AppVersion.get();
        Label versionCompare = new Label(
                String.format("Installed: v%s   ➔   Latest: v%s   (%s)",
                        currentVer, info.version(), info.getFormattedSize()));
        versionCompare.setStyle("-fx-text-fill: " + ThemeManager.getColour("muted")
                + "; -fx-font-size: 13px;");

        // Release notes box
        Label notesHeader = new Label("What's New:");
        notesHeader.setStyle("-fx-text-fill: " + ThemeManager.getColour("text")
                + "; -fx-font-size: 13px; -fx-font-weight: bold;");

        TextArea notesArea = new TextArea();
        notesArea.setEditable(false);
        notesArea.setWrapText(true);
        String notes = info.releaseNotes();
        notesArea.setText((notes != null && !notes.isBlank()) ? notes.trim() : "Performance improvements and bug fixes.");
        notesArea.setPrefRowCount(7);
        notesArea.setStyle("-fx-control-inner-background: " + ThemeManager.getColour("panel")
                + "; -fx-text-fill: " + ThemeManager.getColour("text")
                + "; -fx-border-color: " + ThemeManager.getColour("border")
                + "; -fx-border-radius: 4px;");

        // Progress section (hidden initially)
        VBox progressBox = new VBox(8);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: " + ThemeManager.getColour("accent") + ";");
        progressLabel.setStyle("-fx-text-fill: " + ThemeManager.getColour("muted") + "; -fx-font-size: 12px;");
        progressBox.getChildren().addAll(progressBar, progressLabel);
        progressBox.setVisible(false);
        progressBox.setManaged(false);

        // Buttons row
        HBox buttonBar = new HBox(12);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        githubButton.setStyle("-fx-background-color: transparent; -fx-text-fill: "
                + ThemeManager.getColour("muted") + "; -fx-border-color: "
                + ThemeManager.getColour("border") + "; -fx-border-radius: 4px; -fx-padding: 8 14;");
        githubButton.setOnAction(e -> openBrowser(info.htmlUrl()));

        secondaryButton.setStyle("-fx-background-color: transparent; -fx-text-fill: "
                + ThemeManager.getColour("text") + "; -fx-border-color: "
                + ThemeManager.getColour("border") + "; -fx-border-radius: 4px; -fx-padding: 8 16;");
        secondaryButton.setOnAction(e -> {
            cancelFlag.set(true);
            close();
        });

        actionButton.setStyle("-fx-background-color: " + ThemeManager.getColour("accent")
                + "; -fx-text-fill: " + ThemeManager.getColour("background")
                + "; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 8 20;");
        actionButton.setOnAction(e -> handleActionClick(progressBox));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        buttonBar.getChildren().addAll(githubButton, spacer, secondaryButton, actionButton);

        root.getChildren().addAll(
                eyebrow, title, versionCompare,
                notesHeader, notesArea,
                progressBox, buttonBar);

        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        setScene(scene);

        // Center on owner window
        if (owner != null) {
            setOnShown(ev -> {
                setX(owner.getX() + (owner.getWidth() - getWidth()) / 2);
                setY(owner.getY() + (owner.getHeight() - getHeight()) / 2);
            });
        }
    }

    private void handleActionClick(VBox progressBox) {
        if (downloadedInstaller != null) {
            // Already downloaded, execute installer
            try {
                updateService.installAndRestart(downloadedInstaller);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to launch installer: {0}", ex.getMessage());
                progressLabel.setText("Installation failed: " + ex.getMessage());
            }
            return;
        }

        // Start download
        progressBox.setVisible(true);
        progressBox.setManaged(true);
        actionButton.setDisable(true);
        actionButton.setText("Downloading…");
        secondaryButton.setText("Cancel");
        progressLabel.setText("Connecting to server…");
        cancelFlag.set(false);

        updateService.downloadUpdate(
                updateInfo,
                progressFraction -> Platform.runLater(() -> {
                    progressBar.setProgress(progressFraction);
                    int pct = (int) Math.round(progressFraction * 100);
                    progressLabel.setText(String.format("Downloading: %d%% (%s total)", pct, updateInfo.getFormattedSize()));
                }),
                cancelFlag
        ).thenAccept(installerPath -> Platform.runLater(() -> {
            downloadedInstaller = installerPath;
            progressBar.setProgress(1.0);
            progressLabel.setText("✅ Download complete! Click below to install and restart.");
            progressLabel.setStyle("-fx-text-fill: " + ThemeManager.getColour("accent") + "; -fx-font-weight: bold;");

            actionButton.setDisable(false);
            actionButton.setText("Install & Restart");
            secondaryButton.setText("Close");
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                progressLabel.setText("Download failed: " + ex.getMessage());
                progressLabel.setStyle("-fx-text-fill: " + ThemeManager.getColour("critical") + ";");
                actionButton.setDisable(false);
                actionButton.setText("Retry");
                secondaryButton.setText("Close");
            });
            return null;
        });
    }

    private void openBrowser(String url) {
        if (url == null || url.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to open release URL in browser: {0}", ex.getMessage());
        }
    }
}
