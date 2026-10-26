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

    /** register == true means "create a new account" instead of "sign in". */
    public record Credentials(String email, String password, boolean register) {
    }

    public static Optional<Credentials> show(Window owner) {
        Dialog<Credentials> dialog = new Dialog<>();
        dialog.setTitle("PC DOQTAR — Sign in");
        dialog.setHeaderText("Sign in, or create an account to sync your diagnostics");

        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType loginButton = new ButtonType(
                "Sign in", ButtonBar.ButtonData.OK_DONE);
        ButtonType registerButton = new ButtonType(
                "Create account", ButtonBar.ButtonData.LEFT);

        dialog.getDialogPane().getButtonTypes().addAll(
                registerButton, loginButton, ButtonType.CANCEL);

        TextField emailField = new TextField();
        emailField.setPromptText("Email address");
        emailField.setPrefColumnCount(22);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password (min 6 characters)");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(new Insets(18));

        form.add(new Label("Email"), 0, 0);
        form.add(emailField, 1, 0);
        form.add(new Label("Password"), 0, 1);
        form.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(form);
        emailField.requestFocus();

        dialog.setResultConverter(button -> {
            boolean register = button == registerButton;

            if (button == loginButton || register) {
                String email = emailField.getText().trim();
                String password = passwordField.getText();

                if (email.isBlank() || password.isBlank()) {
                    // The coordinator reports "Enter both your email..."
                    return new Credentials("", "", register);
                }

                return new Credentials(email, password, register);
            }

            return null;
        });

        return dialog.showAndWait();
    }
}
