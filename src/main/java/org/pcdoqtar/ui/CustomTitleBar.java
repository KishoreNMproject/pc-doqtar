package org.pcdoqtar.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

public final class CustomTitleBar extends HBox {

    private double dragOffsetX;
    private double dragOffsetY;

    public CustomTitleBar(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage is required.");
        }

        getStyleClass().add("custom-title-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);
        setPadding(new Insets(0, 0, 0, 14));
        setMinHeight(48);
        setPrefHeight(48);
        setMaxHeight(48);

        Label title = new Label("PC DOQTAR  |  System Health");
        title.getStyleClass().add("title-bar-title");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimise = windowButton("—", "window-button", 42);
        minimise.setOnAction(event -> stage.setIconified(true));

        Button maximise = windowButton("□", "window-button", 42);
        maximise.setOnAction(event -> stage.setMaximized(!stage.isMaximized()));

        Button close = windowButton("×", "window-close-button", 46);
        close.setOnAction(event -> stage.close());

        getChildren().addAll(title, spacer, minimise, maximise, close);

        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getTarget() instanceof javafx.scene.Node node
                    && (node == this || node == title || node == spacer)) {
                dragOffsetX = event.getScreenX() - stage.getX();
                dragOffsetY = event.getScreenY() - stage.getY();
            }
        });

        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!stage.isMaximized()
                    && !(event.getTarget() instanceof Button)) {
                stage.setX(event.getScreenX() - dragOffsetX);
                stage.setY(event.getScreenY() - dragOffsetY);
            }
        });

        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && (event.getTarget() == this || event.getTarget() == title)) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }

    private Button windowButton(String text, String styleClass, double width) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setFocusTraversable(false);
        button.setMinSize(width, 48);
        button.setPrefSize(width, 48);
        button.setMaxSize(width, 48);
        button.setPadding(Insets.EMPTY);
        return button;
    }
}
