
package org.pcdoqtar.ui.components;

import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class MetricCard extends VBox {

    private final Label titleLabel = new Label();
    private final Label valueLabel = new Label();
    private final Label detailLabel = new Label();

    public MetricCard(String title, String value, String detail) {
        setSpacing(10);
        setPadding(new Insets(18));
        setMinWidth(150);
        setMaxWidth(Double.MAX_VALUE);

        setStyle(
                "-fx-background-color: "
                + ThemeManager.getColour("panel") + ";"
                + "-fx-background-radius: 8;"
                + "-fx-border-color: "
                + ThemeManager.getColour("border") + ";"
                + "-fx-border-radius: 8;"
        );

        titleLabel.setText(safe(title));
        titleLabel.setWrapText(true);
        titleLabel.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("muted") + ";"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: bold;"
        );

        valueLabel.setText(safe(value));
        valueLabel.setWrapText(true);
        valueLabel.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("text") + ";"
                + "-fx-font-size: 24px;"
                + "-fx-font-weight: bold;"
        );

        detailLabel.setText(safe(detail));
        detailLabel.setWrapText(true);
        detailLabel.setStyle(
                "-fx-text-fill: "
                + ThemeManager.getColour("muted") + ";"
                + "-fx-font-size: 12px;"
        );

        VBox.setVgrow(detailLabel, Priority.NEVER);
        getChildren().addAll(titleLabel, valueLabel, detailLabel);
    }

    public MetricCard(String title, String value) {
        this(title, value, "");
    }

    public void setTitle(String title) {
        titleLabel.setText(safe(title));
    }

    public void setValue(String value) {
        valueLabel.setText(safe(value));
    }

    public void setDetail(String detail) {
        detailLabel.setText(safe(detail));
    }

    public void setValueColour(String colour) {
        if (colour == null || !colour.matches("#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException(
                    "Colour must be a six-digit hexadecimal colour."
            );
        }

        valueLabel.setStyle(
                "-fx-text-fill: " + colour + ";"
                + "-fx-font-size: 24px;"
                + "-fx-font-weight: bold;"
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
