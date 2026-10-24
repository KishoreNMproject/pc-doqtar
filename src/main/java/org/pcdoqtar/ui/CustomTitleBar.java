package org.pcdoqtar.ui;

import org.pcdoqtar.ui.theme.ThemeManager;

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
        if (stage == null) throw new IllegalArgumentException("Stage is required.");

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setPadding(new Insets(6, 8, 6, 14));
        setStyle("-fx-background-color: " + ThemeManager.getColour("panel")
                + ";-fx-border-color: " + ThemeManager.getColour("border")
                + ";-fx-border-width: 0 0 1 0;");

        Label title = new Label("PC DOQTAR  |  System Health");
        title.setStyle("-fx-text-fill: " + ThemeManager.getColour("text")
                + ";-fx-font-size: 13px;-fx-font-weight: bold;");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minimise = windowButton("—");
        minimise.setStyle(buttonStyle(false));
        minimise.setOnAction(event -> stage.setIconified(true));

        Button maximise = windowButton("□");
        maximise.setStyle(buttonStyle(false));
        maximise.setOnAction(event -> stage.setMaximized(!stage.isMaximized()));

        Button close = windowButton("×");
        close.setStyle(buttonStyle(false));
        close.setOnMouseEntered(event -> close.setStyle(buttonStyle(true)));
        close.setOnMouseExited(event -> close.setStyle(buttonStyle(false)));
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
            if (event.getClickCount() == 2 && event.getTarget() == this) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }

    private Button windowButton(String text) {
        Button button = new Button(text);
        button.setFocusTraversable(false);
        button.setAlignment(Pos.CENTER);
        button.setMinSize(40, 28);
        button.setPrefSize(40, 28);
        button.setMaxSize(40, 28);
        button.setStyle(buttonStyle(false));
        return button;
    }

    private String buttonStyle(boolean hovered) {
        String background = hovered ? ThemeManager.getColour("critical") : "transparent";
        return "-fx-background-color: " + background + ";-fx-text-fill: "
                + ThemeManager.getColour("text")
                + ";-fx-font-size: 14px;-fx-padding: 0;-fx-background-radius: 2;"
                + "-fx-cursor: hand;";
    }
}