
package org.pcdoqtar.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public final class CustomTitleBar extends HBox {

    private double dragOffsetX;
    private double dragOffsetY;

    public CustomTitleBar(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage is required.");
        }

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setPadding(new Insets(8, 10, 8, 14));
        setStyle(
                "-fx-background-color: #141821;"
                + "-fx-border-color: #2B3240;"
                + "-fx-border-width: 0 0 1 0;");

        Label title = new Label("PC DOQTAR  |  System Health");
        title.setStyle(
                "-fx-text-fill: #F2F5FA;"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button minimise = windowButton("—");
        minimise.setOnAction(event -> stage.setIconified(true));

        Button maximise = windowButton("□");
        maximise.setOnAction(event -> stage.setMaximized(!stage.isMaximized()));

        Button close = windowButton("×");
        close.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-text-fill: #F2F5FA;"
                + "-fx-font-size: 17px;"
                + "-fx-padding: 2 12 2 12;");
        close.setOnMouseEntered(event -> close.setStyle(
                "-fx-background-color: #C62828;"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 17px;"
                + "-fx-padding: 2 12 2 12;"));
        close.setOnMouseExited(event -> close.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-text-fill: #F2F5FA;"
                + "-fx-font-size: 17px;"
                + "-fx-padding: 2 12 2 12;"));
        close.setOnAction(event -> stage.close());

        getChildren().addAll(title, spacer, minimise, maximise, close);

        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            dragOffsetX = event.getScreenX() - stage.getX();
            dragOffsetY = event.getScreenY() - stage.getY();
        });

        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!stage.isMaximized()) {
                stage.setX(event.getScreenX() - dragOffsetX);
                stage.setY(event.getScreenY() - dragOffsetY);
            }
        });

        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && event.getTarget() == this) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }

    private Button windowButton(String text) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        button.setStyle(
                "-fx-background-color: transparent;"
                + "-fx-text-fill: #F2F5FA;"
                + "-fx-font-size: 14px;"
                + "-fx-padding: 4 12 4 12;"
                + "-fx-cursor: hand;");
        return button;
    }
}
