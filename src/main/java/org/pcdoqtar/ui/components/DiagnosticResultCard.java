
package org.pcdoqtar.ui.components;

import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.HealthStatus;
import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DiagnosticResultCard extends VBox {

    private final Label metricLabel = new Label();
    private final Label valueLabel = new Label();
    private final Label statusLabel = new Label();
    private final Label messageLabel = new Label();

    public DiagnosticResultCard(DiagnosticResult result) {
        setSpacing(8);
        setPadding(new Insets(16));
        setMaxWidth(Double.MAX_VALUE);

        for (Label label : new Label[] {
                metricLabel, valueLabel, statusLabel, messageLabel
        }) {
            label.setWrapText(true);
        }

        metricLabel.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("text") + ";"
                + "-fx-font-size: 15px;"
                + "-fx-font-weight: bold;"
        );

        valueLabel.setStyle(
                "-fx-font-size: 20px;"
                + "-fx-font-weight: bold;"
        );

        messageLabel.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("muted") + ";"
                + "-fx-font-size: 12px;"
        );

        getChildren().addAll(
                metricLabel, valueLabel, statusLabel, messageLabel
        );

        setResult(result);
    }

    public void setResult(DiagnosticResult result) {
        if (result == null) {
            throw new IllegalArgumentException(
                    "Diagnostic result must not be null."
            );
        }

        HealthStatus status = result.getStatus();

        metricLabel.setText(safe(result.getMetric()));

        String value = Double.isFinite(result.getValue())
                ? String.format(
                        "%.2f %s",
                        result.getValue(),
                        safe(result.getUnit())
                )
                : "Unavailable";

        valueLabel.setText(value);
        statusLabel.setText("Status: " + status);
        messageLabel.setText(safe(result.getMessage()));

        String colour = switch (status) {
            case NORMAL -> ThemeManager.getColour("accent");
            case WARNING -> ThemeManager.getColour("warning");
            case CRITICAL -> ThemeManager.getColour("critical");
            case UNAVAILABLE -> ThemeManager.getColour("muted");
        };

        valueLabel.setStyle(
                "-fx-text-fill: " + colour + ";"
                + "-fx-font-size: 20px;"
                + "-fx-font-weight: bold;"
        );

        statusLabel.setStyle(
                "-fx-text-fill: " + colour + ";"
                + "-fx-font-weight: bold;"
        );

        setStyle(
                "-fx-background-color: "
                + ThemeManager.getColour("panel") + ";"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: " + colour + ";"
                + "-fx-border-radius: 8;"
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
