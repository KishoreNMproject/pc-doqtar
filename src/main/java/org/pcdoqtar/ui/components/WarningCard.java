
package org.pcdoqtar.ui.components;

import org.pcdoqtar.model.HealthStatus;
import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class WarningCard extends VBox {

    private final Label headingLabel = new Label();
    private final Label messageLabel = new Label();
    private HealthStatus status;

    public WarningCard(
            String heading,
            String message,
            HealthStatus status) {

        setSpacing(8);
        setPadding(new Insets(16));
        setMaxWidth(Double.MAX_VALUE);

        headingLabel.setText(safe(heading));
        headingLabel.setWrapText(true);
        headingLabel.setStyle(
                "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
        );

        messageLabel.setText(safe(message));
        messageLabel.setWrapText(true);
        messageLabel.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("text") + ";"
                + "-fx-font-size: 12px;"
        );

        getChildren().addAll(headingLabel, messageLabel);
        setStatus(status);
    }

    public WarningCard(String heading, String message) {
        this(heading, message, HealthStatus.WARNING);
    }

    public void setHeading(String heading) {
        headingLabel.setText(safe(heading));
    }

    public void setMessage(String message) {
        messageLabel.setText(safe(message));
    }

    public void setStatus(HealthStatus status) {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Health status must not be null."
            );
        }

        this.status = status;

        String colour = switch (status) {
            case NORMAL -> ThemeManager.getColour("accent");
            case WARNING -> ThemeManager.getColour("warning");
            case CRITICAL -> ThemeManager.getColour("critical");
            case UNAVAILABLE -> ThemeManager.getColour("muted");
        };

        headingLabel.setStyle(
                "-fx-text-fill: " + colour + ";"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
        );

        setStyle(
                "-fx-background-color: "
                + ThemeManager.getColour("panel") + ";"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: " + colour + ";"
                + "-fx-border-radius: 8;"
                + "-fx-border-width: 1;"
        );
    }

    public HealthStatus getStatus() {
        return status;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
