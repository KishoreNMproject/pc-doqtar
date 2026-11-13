package org.pcdoqtar.ui;

import java.io.IOException;
import java.util.Optional;

import org.pcdoqtar.auth.AuthSession;
import org.pcdoqtar.auth.AuthenticationCoordinator;
import org.pcdoqtar.auth.FirebaseAuthService;
import org.pcdoqtar.ota.AppVersion;
import org.pcdoqtar.ota.UpdateDialog;
import org.pcdoqtar.ota.UpdateInfo;
import org.pcdoqtar.ota.UpdateService;
import org.pcdoqtar.settings.AppSettings;
import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SettingsPage extends VBox {

    private final AuthSession session;
    private final FirebaseAuthService authService;
    private final AuthenticationCoordinator coordinator;

    private final ComboBox<String> themeSelector = new ComboBox<>();
    private final Slider warningSlider =
            new Slider(0, 100, AppSettings.getWarningThreshold());

    private final Label thresholdValue = new Label();
    private final Label statusLabel = new Label();

    private final Label accountDescription = new Label();
    private final Label accountEmailLabel = new Label();
    private final Button resetPasswordButton = new Button("Reset password");
    private final Label accountStatusLabel = new Label();

    public SettingsPage() {
        this(null, null, null);
    }

    public SettingsPage(
            AuthSession session,
            FirebaseAuthService authService,
            AuthenticationCoordinator coordinator) {

        this.session = session;
        this.authService = authService;
        this.coordinator = coordinator;

        getStyleClass().add("settings-page");
        setSpacing(20);
        setPadding(new Insets(28));
        setStyle("-fx-background-color: "
                + ThemeManager.getColour("background") + ";");

        Label eyebrow = new Label("PREFERENCES / CONFIGURATION");
        eyebrow.setStyle("-fx-text-fill: "
                + ThemeManager.getColour("accent") + ";");

        Label heading = new Label("Settings");
        heading.setStyle("-fx-text-fill: "
                + ThemeManager.getColour("text")
                + "; -fx-font-size: 30px; -fx-font-weight: bold;");

        Label description = new Label(
                "Configure appearance, diagnostic warning thresholds, and account security.");
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: "
                + ThemeManager.getColour("muted") + ";");

        // Account security card
        VBox accountCard = createCard();
        Label accountHeading = heading("Account security", 18);
        accountDescription.setWrapText(true);
        accountDescription.setStyle(mutedStyle());

        accountEmailLabel.setStyle(headingStyle(14));
        accountEmailLabel.setWrapText(true);

        resetPasswordButton.getStyleClass().add("primary-action");
        resetPasswordButton.setFocusTraversable(true);
        resetPasswordButton.setOnAction(event -> handleResetPassword());

        accountStatusLabel.setWrapText(true);
        accountStatusLabel.setStyle(mutedStyle());

        accountCard.getChildren().addAll(
                accountHeading, accountDescription, accountEmailLabel,
                resetPasswordButton, accountStatusLabel);

        refreshAccount();

        // Appearance card
        VBox themeCard = createCard();
        Label themeHeading = heading("Appearance", 18);
        Label themeDescription = new Label(
                "Choose the application-wide colour theme.");
        themeDescription.setWrapText(true);
        themeDescription.setStyle(mutedStyle());

        themeSelector.getItems().setAll("Dark", "Light");
        themeSelector.setValue(ThemeManager.getCurrentTheme());
        themeSelector.setMaxWidth(240);

        themeSelector.setOnAction(event -> {
            String selected = themeSelector.getValue();
            if (selected == null) {
                return;
            }

            ThemeManager.setTheme(selected);

            Scene scene = getScene();
            if (scene != null) {
                ThemeManager.apply(scene);
            }

            statusLabel.setText("Theme saved: " + selected);
        });

        themeCard.getChildren().addAll(
                themeHeading, themeDescription, themeSelector);

        // Warning threshold card
        VBox thresholdCard = createCard();
        Label thresholdHeading = heading("Warning threshold", 18);

        Label thresholdDescription = new Label(
                "Choose the utilisation percentage above which a warning "
                + "is reported. Critical status is evaluated separately.");
        thresholdDescription.setWrapText(true);
        thresholdDescription.setStyle(mutedStyle());

        warningSlider.setMin(0);
        warningSlider.setMax(100);
        warningSlider.setShowTickMarks(true);
        warningSlider.setShowTickLabels(true);
        warningSlider.setMajorTickUnit(10);
        warningSlider.setMinorTickCount(4);
        warningSlider.setBlockIncrement(1);
        warningSlider.setSnapToTicks(false);
        warningSlider.setMaxWidth(Double.MAX_VALUE);

        thresholdValue.setStyle(headingStyle(16));
        updateThresholdLabel();

        warningSlider.valueProperty().addListener(
                (observable, oldValue, newValue) -> updateThresholdLabel());

        Button saveButton = new Button("Save threshold");
        saveButton.setOnAction(event -> saveThreshold());
        saveButton.setStyle(
                "-fx-background-color: " + ThemeManager.getColour("accent")
                + "; -fx-text-fill: " + ThemeManager.getColour("background")
                + "; -fx-font-weight: bold; -fx-padding: 10 16 10 16;");

        thresholdCard.getChildren().addAll(
                thresholdHeading, thresholdDescription,
                thresholdValue, warningSlider, saveButton);

        // Navigation menu card
        VBox navigationCard = createCard();
        Label navigationHeading = heading("Navigation menu", 18);
        Label navigationDescription = new Label("Choose whether the hamburger navigation drawer starts collapsed when PC DOQTAR launches. The drawer starts expanded by default.");
        navigationDescription.setWrapText(true);
        navigationDescription.setStyle(mutedStyle());
        CheckBox collapsedByDefault = new CheckBox("Start with navigation collapsed");
        collapsedByDefault.setSelected(AppSettings.isNavigationCollapsedByDefault());
        collapsedByDefault.setOnAction(event -> AppSettings.setNavigationCollapsedByDefault(collapsedByDefault.isSelected()));
        navigationCard.getChildren().addAll(navigationHeading, navigationDescription, collapsedByDefault);

        // Software updates (OTA) card
        VBox updateCard = createCard();
        Label updateHeading = heading("Software updates (OTA)", 18);
        Label updateDescription = new Label(
                "Check for new releases, review release notes, and install updates over the air.");
        updateDescription.setWrapText(true);
        updateDescription.setStyle(mutedStyle());

        String currentVersionStr = AppVersion.get();
        Label currentVersionLabel = new Label("Current version: v" + currentVersionStr);
        currentVersionLabel.setStyle(headingStyle(14));

        CheckBox autoCheckUpdates = new CheckBox("Automatically check for updates on startup");
        autoCheckUpdates.setSelected(AppSettings.isAutoUpdateCheckEnabled());
        autoCheckUpdates.setOnAction(event -> AppSettings.setAutoUpdateCheckEnabled(autoCheckUpdates.isSelected()));

        Label updateStatusLabel = new Label();
        updateStatusLabel.setWrapText(true);
        updateStatusLabel.setStyle(mutedStyle());

        Button checkUpdatesButton = new Button("Check for updates");
        checkUpdatesButton.setStyle(
                "-fx-background-color: " + ThemeManager.getColour("accent")
                + "; -fx-text-fill: " + ThemeManager.getColour("background")
                + "; -fx-font-weight: bold; -fx-padding: 8 16 8 16;");

        HBox updateActionRow = new HBox(12, checkUpdatesButton, updateStatusLabel);
        updateActionRow.setAlignment(Pos.CENTER_LEFT);

        checkUpdatesButton.setOnAction(event -> {
            checkUpdatesButton.setDisable(true);
            updateStatusLabel.setText("Checking GitHub for updates…");

            UpdateService.getInstance().checkForUpdates().thenAccept(optionalInfo -> {
                Platform.runLater(() -> {
                    checkUpdatesButton.setDisable(false);
                    if (optionalInfo.isEmpty()) {
                        updateStatusLabel.setText("Unable to check for updates. Check your internet connection.");
                    } else {
                        UpdateInfo info = optionalInfo.get();
                        if (info.updateAvailable()) {
                            updateStatusLabel.setText("Update available: " + info.tagName() + " (" + info.getFormattedSize() + ")");
                            updateStatusLabel.setStyle("-fx-text-fill: " + ThemeManager.getColour("accent") + "; -fx-font-weight: bold;");
                            UpdateDialog.showUpdateAvailable(getScene().getWindow(), info, UpdateService.getInstance());
                        } else {
                            updateStatusLabel.setText("PC DOQTAR is up to date (v" + currentVersionStr + ").");
                            updateStatusLabel.setStyle("-fx-text-fill: " + ThemeManager.getColour("text") + ";");
                        }
                    }
                });
            }).exceptionally(ex -> {
                Platform.runLater(() -> {
                    checkUpdatesButton.setDisable(false);
                    updateStatusLabel.setText("Update check failed: " + ex.getMessage());
                });
                return null;
            });
        });

        updateCard.getChildren().addAll(
                updateHeading, updateDescription, currentVersionLabel,
                autoCheckUpdates, updateActionRow);

        statusLabel.setWrapText(true);
        statusLabel.setStyle(mutedStyle());

        getChildren().addAll(
                eyebrow, heading, description,
                accountCard, updateCard, themeCard, navigationCard, thresholdCard, statusLabel);
    }

    public void refreshAccount() {
        boolean signedIn = session != null && session.isLoggedIn();
        if (signedIn) {
            String email = session.getEmail();
            accountDescription.setText(
                    "Request a password reset email for your currently signed-in account without signing out.");
            accountEmailLabel.setText("Signed in email: " + (email != null ? email : ""));
            accountEmailLabel.setVisible(true);
            accountEmailLabel.setManaged(true);
            resetPasswordButton.setDisable(false);
            resetPasswordButton.setVisible(true);
            resetPasswordButton.setManaged(true);
        } else {
            accountDescription.setText(
                    "No account is currently signed in. You must sign in before resetting your password through Settings, or use the 'Forgot Password' link in the sign-in dialog.");
            accountEmailLabel.setText("");
            accountEmailLabel.setVisible(false);
            accountEmailLabel.setManaged(false);
            resetPasswordButton.setDisable(true);
            resetPasswordButton.setVisible(false);
            resetPasswordButton.setManaged(false);
        }
    }

    private void handleResetPassword() {
        if (session == null || !session.isLoggedIn() || session.getEmail() == null || session.getEmail().isBlank()) {
            accountStatusLabel.setText("You must sign in before resetting your password through Settings.");
            return;
        }

        String email = session.getEmail().trim();

        // Confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reset Password");
        confirm.setHeaderText("Send password reset link to " + email + "?");
        confirm.setContentText("A password reset link will be sent to your account email address (" + email + ").\n\nYour current session will remain active. Do you want to continue?");

        if (getScene() != null && getScene().getWindow() != null) {
            confirm.initOwner(getScene().getWindow());
        }

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            accountStatusLabel.setText("Password reset cancelled.");
            return;
        }

        // Prevent repeated clicks while processing
        resetPasswordButton.setDisable(true);
        accountStatusLabel.setText("Sending password reset email…");

        if (coordinator != null) {
            coordinator.resetPassword(email).thenAccept(authResult ->
                    Platform.runLater(() -> {
                        resetPasswordButton.setDisable(false);
                        accountStatusLabel.setText(authResult.message());
                    }));
        } else if (authService != null) {
            Thread worker = new Thread(() -> {
                String message;
                try {
                    authService.sendPasswordResetEmail(email);
                    message = "If this email is registered, a password reset link has been sent. Check your inbox.";
                } catch (IOException e) {
                    message = e.getMessage() == null ? "Password reset request failed." : e.getMessage();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    message = "Password reset request was interrupted.";
                } catch (RuntimeException e) {
                    message = "Password reset could not be completed.";
                }

                final String finalMsg = message;
                Platform.runLater(() -> {
                    resetPasswordButton.setDisable(false);
                    accountStatusLabel.setText(finalMsg);
                });
            }, "pc-doqtar-settings-reset-password");
            worker.setDaemon(true);
            worker.start();
        } else {
            resetPasswordButton.setDisable(false);
            accountStatusLabel.setText("Authentication service is not available.");
        }
    }

    private VBox createCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("app-tile");
        return card;
    }

    private Label heading(String text, int size) {
        Label label = new Label(text);
        label.setStyle(headingStyle(size));
        return label;
    }

    private String headingStyle(int size) {
        return "-fx-text-fill: " + ThemeManager.getColour("text")
                + "; -fx-font-size: " + size + "px;"
                + " -fx-font-weight: bold;";
    }

    private String mutedStyle() {
        return "-fx-text-fill: " + ThemeManager.getColour("muted") + ";";
    }

    private void updateThresholdLabel() {
        thresholdValue.setText(String.format(
                "Warning at %.0f%%", warningSlider.getValue()));
    }

    private void saveThreshold() {
        try {
            double value = Math.round(warningSlider.getValue());
            AppSettings.setWarningThreshold(value);
            statusLabel.setText(String.format(
                    "Warning threshold saved: %.0f%%", value));
        } catch (IllegalArgumentException exception) {
            statusLabel.setText("Unable to save: " + exception.getMessage());
        }
    }

    // Accessors for automated testing
    public Button getResetPasswordButton() {
        return resetPasswordButton;
    }

    public Label getAccountStatusLabel() {
        return accountStatusLabel;
    }

    public Label getAccountEmailLabel() {
        return accountEmailLabel;
    }
}
