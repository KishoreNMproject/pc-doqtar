package org.pcdoqtar.auth;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

public final class LoginDialog {

    private LoginDialog() {
    }

    public record Credentials(String email, String password) {
    }

    public static Optional<Credentials> show(Window owner) {
        Dialog<Credentials> dialog = new Dialog<>();
        dialog.setTitle("PC DOQTAR — Sign in");
        dialog.setHeaderText("Sign in with your Firebase account");

        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType loginButton = new ButtonType(
                "Sign in", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(
                loginButton, ButtonType.CANCEL);

        TextField emailField = new TextField();
        emailField.setPromptText("Email address");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(new Insets(18));

        form.add(new Label("Email"), 0, 0);
        form.add(emailField, 1, 0);
        form.add(new Label("Password"), 0, 1);
        form.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(button -> {
            if (button == loginButton) {
                String email = emailField.getText().trim();
                String password = passwordField.getText();

                if (email.isBlank() || password.isBlank()) {
                    return null;
                }

                return new Credentials(email, password);
            }

            return null;
        });

        return dialog.showAndWait();
    }
}