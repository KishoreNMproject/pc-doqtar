
package org.pcdoqtar.ui;

import org.pcdoqtar.settings.AppSettings;
import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;

public class SettingsPage extends VBox {

    private final ComboBox<String> themeSelector =
            new ComboBox<>();

    private final Slider warningSlider =
            new Slider(10, 89, AppSettings.getWarningThreshold());

    private final Label thresholdValue = new Label();
    private final Label statusLabel = new Label();

    public SettingsPage() {
        setSpacing(20);
        setPadding(new Insets(28));
        setStyle("-fx-background-color: "
                + ThemeManager.getColour("background") + ";");

        Label eyebrow = new Label("PREFERENCES / CONFIGURATION");
        eyebrow.setStyle("-fx-text-fill: "
                + ThemeManager.getColour("accent") + ";");

        Label heading = new Label("Settings");
        heading.setStyle(
                "-fx-text-fill: " + ThemeManager.getColour("text") + ";"
                + "-fx-font-size: 30px;"
                + "-fx-font-weight: bold;"
        );

        Label description = new Label(
                "Configure the application appearance and warning threshold."
        );
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: "
                + ThemeManager.getColour("muted") + ";");

        VBox themeCard = createCard();
        Label themeHeading = new Label("Appearance");
        themeHeading.setStyle(headingStyle(18));

        Label themeDescription = new Label(
                "Choose the theme used by theme-aware screens."
        );
        themeDescription.setWrapText(true);
        themeDescription.setStyle(mutedStyle());

        themeSelector.getItems().setAll("Dark", "Light");
        themeSelector.setValue(ThemeManager.getCurrentTheme());

        themeSelector.setOnAction(event -> {
            String selectedTheme = themeSelector.getValue();

            if (selectedTheme != null) {
                ThemeManager.setTheme(selectedTheme);
                statusLabel.setText(
                        "Theme preference saved. "
                        + "Some screens may require refresh "
                        + "until global theme application is connected."
                );
            }
        });

        themeCard.getChildren().addAll(
                themeHeading, themeDescription, themeSelector
        );

        VBox thresholdCard = createCard();
        Label thresholdHeading = new Label("Warning threshold");
        thresholdHeading.setStyle(headingStyle(18));

        Label thresholdDescription = new Label(
                "Set the utilisation percentage at which a warning begins. "
                + "Critical status remains governed by the diagnostic engine."
        );
        thresholdDescription.setWrapText(true);
        thresholdDescription.setStyle(mutedStyle());

        warningSlider.setShowTickMarks(true);
        warningSlider.setShowTickLabels(true);
        warningSlider.setMajorTickUnit(20);
        warningSlider.setMinorTickCount(1);
        warningSlider.setBlockIncrement(1);
        warningSlider.setSnapToTicks(false);
        warningSlider.setMaxWidth(Double.MAX_VALUE);

        thresholdValue.setStyle(headingStyle(16));
        updateThresholdLabel();

        warningSlider.valueProperty().addListener(
                (observable, oldValue, newValue) ->
                        updateThresholdLabel()
        );

        Button saveButton = new Button("Save threshold");
        saveButton.setOnAction(event -> saveThreshold());

        saveButton.setStyle(
                "-fx-background-color: "
                + ThemeManager.getColour("accent") + ";"
                + "-fx-text-fill: "
                + ThemeManager.getColour("background") + ";"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 10 16 10 16;"
        );

        thresholdCard.getChildren().addAll(
                thresholdHeading,
                thresholdDescription,
                thresholdValue,
                warningSlider,
                saveButton
        );

        statusLabel.setWrapText(true);
        statusLabel.setStyle(mutedStyle());

        getChildren().addAll(
                eyebrow,
                heading,
                description,
                themeCard,
                thresholdCard,
                statusLabel
        );
    }

    private VBox createCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: "
                + ThemeManager.getColour("panel") + ";"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: "
                + ThemeManager.getColour("border") + ";"
                + "-fx-border-radius: 8;"
        );
        return card;
    }

    private String headingStyle(int size) {
        return "-fx-text-fill: "
                + ThemeManager.getColour("text") + ";"
                + "-fx-font-size: " + size + "px;"
                + "-fx-font-weight: bold;";
    }

    private String mutedStyle() {
        return "-fx-text-fill: "
                + ThemeManager.getColour("muted") + ";";
    }

    private void updateThresholdLabel() {
        thresholdValue.setText(
                String.format("Warning at %.0f%%", warningSlider.getValue())
        );
    }

    private void saveThreshold() {
        try {
            double threshold = Math.round(warningSlider.getValue());
            AppSettings.setWarningThreshold(threshold);

            statusLabel.setText(
                    String.format("Warning threshold saved: %.0f%%", threshold)
            );
        } catch (IllegalArgumentException exception) {
            statusLabel.setText("Unable to save: " + exception.getMessage());
        }
    }
}
