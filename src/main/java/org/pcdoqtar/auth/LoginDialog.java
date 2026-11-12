package org.pcdoqtar.auth;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.pcdoqtar.ui.theme.ThemeManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public final class LoginDialog {

    private static final String THEME_DARK = "/org/pcdoqtar/ui/theme/theme-dark.css";
    private static final String THEME_LIGHT = "/org/pcdoqtar/ui/theme/theme-light.css";
    private static final String LOGIN_CSS = "/org/pcdoqtar/auth/login.css";

    private LoginDialog() {
    }

    /** register == true means "create a new account" instead of "sign in". */
    public record Credentials(String email, String password, boolean register) {
    }

    // ------------------------------------------------------------------
    // Themed, optional sign-in. Signs in INSIDE the dialog so errors show
    // inline. Returns true when the user is now signed in.
    // ------------------------------------------------------------------
    public static boolean showAndSignIn(Window owner, FirebaseAuthService authService) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("PC DOQTAR — Sign in");
        dialog.initStyle(StageStyle.UNDECORATED);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        DialogPane pane = dialog.getDialogPane();
        applyTheme(pane);

        ButtonType loginType = new ButtonType("Sign in", ButtonBar.ButtonData.OK_DONE);
        ButtonType registerType = new ButtonType("Create account", ButtonBar.ButtonData.LEFT);
        ButtonType skipType = new ButtonType("Not now", ButtonBar.ButtonData.CANCEL_CLOSE);
        pane.getButtonTypes().addAll(registerType, loginType, skipType);

        Label title = new Label("Sign in to PC DOQTAR");
        title.getStyleClass().add("login-title");

        Label info = new Label("Optional. Everything works offline. Sign in only "
                + "if you want your results saved to your cloud history.");
        info.getStyleClass().add("page-description");
        info.setWrapText(true);
        info.setMaxWidth(360);

        TextField email = new TextField();
        email.setPromptText("name@example.com");
        PasswordField password = new PasswordField();
        password.setPromptText("At least 6 characters");

        Label emailLabel = new Label("EMAIL");
        emailLabel.getStyleClass().add("login-field-label");
        Label passwordLabel = new Label("PASSWORD");
        passwordLabel.getStyleClass().add("login-field-label");

        Hyperlink forgotPasswordLink = new Hyperlink("Forgot password?");
        forgotPasswordLink.getStyleClass().add("forgot-password-link");
        forgotPasswordLink.setFocusTraversable(true);

        HBox passwordHeader = new HBox();
        passwordHeader.setAlignment(Pos.CENTER_LEFT);
        Region passwordSpacer = new Region();
        HBox.setHgrow(passwordSpacer, Priority.ALWAYS);
        passwordHeader.getChildren().addAll(passwordLabel, passwordSpacer, forgotPasswordLink);

        GridPane form = new GridPane();
        form.setVgap(6);
        form.setHgap(10);
        form.add(emailLabel, 0, 0);
        form.add(email, 0, 1);
        form.add(passwordHeader, 0, 2);
        form.add(password, 0, 3);
        email.setPrefColumnCount(26);
        password.setPrefColumnCount(26);

        Label message = new Label(" ");
        message.setWrapText(true);
        message.setMaxWidth(360);
        message.setMinHeight(40);

        VBox content = new VBox(12, title, info, form, message);
        content.setPadding(new Insets(22, 22, 4, 22));
        pane.setContent(content);
        pane.setMinWidth(420);

        Button loginButton = (Button) pane.lookupButton(loginType);
        Button registerButton = (Button) pane.lookupButton(registerType);
        Button skipButton = (Button) pane.lookupButton(skipType);
        Button[] all = {loginButton, registerButton, skipButton};

        // Consume ACTION so the dialog stays open until Firebase answers.
        loginButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            attempt(false, dialog, authService, email, password, message, all, forgotPasswordLink);
        });
        registerButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            attempt(true, dialog, authService, email, password, message, all, forgotPasswordLink);
        });
        forgotPasswordLink.setOnAction(event -> {
            handleForgotPassword(authService, email, message, all, forgotPasswordLink);
        });

        // Closing with "Not now" / Esc simply means "stay offline".
        dialog.setResultConverter(button -> Boolean.FALSE);
        dialog.setOnShown(event -> email.requestFocus());

        return dialog.showAndWait().orElse(Boolean.FALSE);
    }

    private static void handleForgotPassword(
            FirebaseAuthService authService,
            TextField emailField,
            Label message,
            Button[] buttons,
            Hyperlink forgotPasswordLink) {

        String emailText = emailField.getText() == null ? "" : emailField.getText().trim();

        if (emailText.isEmpty()) {
            show(message, "Enter your email address to reset your password.", true);
            emailField.requestFocus();
            return;
        }

        if (!FirebaseAuthService.isValidEmail(emailText)) {
            show(message, "Enter a valid email address.", true);
            emailField.requestFocus();
            return;
        }

        setBusy(buttons, forgotPasswordLink, true);
        show(message, "Sending password reset email…", false);

        Thread worker = new Thread(() -> {
            String error = null;

            try {
                authService.sendPasswordResetEmail(emailText);
            } catch (IOException exception) {
                error = exception.getMessage() == null || exception.getMessage().isBlank()
                        ? "Unable to send reset email. Check your connection and try again."
                        : exception.getMessage();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                error = "Password reset request was interrupted.";
            } catch (RuntimeException exception) {
                error = "Password reset could not be completed.";
            }

            final String failure = error;
            Platform.runLater(() -> {
                setBusy(buttons, forgotPasswordLink, false);
                if (failure == null) {
                    show(message, "If this email is registered, a password reset link has been sent. Check your inbox.", false);
                } else {
                    show(message, failure, true);
                }
            });
        }, "pc-doqtar-forgot-password");

        worker.setDaemon(true);
        worker.start();
    }

    private static void attempt(
            boolean register,
            Dialog<Boolean> dialog,
            FirebaseAuthService authService,
            TextField emailField,
            PasswordField passwordField,
            Label message,
            Button[] buttons,
            Hyperlink forgotPasswordLink) {

        String emailText = emailField.getText().trim();
        String passwordText = passwordField.getText();

        if (emailText.isEmpty() || passwordText.isEmpty()) {
            show(message, "Enter your email address and password.", true);
            return;
        }

        setBusy(buttons, forgotPasswordLink, true);
        show(message, register ? "Creating your account…" : "Signing in…", false);

        Thread worker = new Thread(() -> {
            String error = null;

            try {
                if (register) {
                    authService.signUp(emailText, passwordText);
                } else {
                    authService.signIn(emailText, passwordText);
                }
            } catch (IOException exception) {
                error = exception.getMessage() == null || exception.getMessage().isBlank()
                        ? "Unable to sign in. Check your connection and try again."
                        : exception.getMessage();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                error = "Sign-in was interrupted.";
            } catch (RuntimeException exception) {
                error = "Sign-in could not be completed.";
            }

            final String failure = error;
            Platform.runLater(() -> {
                if (failure == null) {
                    dialog.setResult(Boolean.TRUE); // closes the dialog
                } else {
                    show(message, failure, true);
                    setBusy(buttons, forgotPasswordLink, false);
                }
            });
        }, "pc-doqtar-login");

        worker.setDaemon(true);
        worker.start();
    }

    private static void setBusy(Button[] buttons, Hyperlink link, boolean busy) {
        for (Button button : buttons) {
            button.setDisable(busy);
        }
        if (link != null) {
            link.setDisable(busy);
        }
    }

    private static void show(Label label, String text, boolean error) {
        label.getStyleClass().removeAll("login-error", "login-info");
        label.getStyleClass().add(error ? "login-error" : "login-info");
        label.setText(text);
    }

    /** Same theme sheet the main window uses, plus the login rules. */
    private static void applyTheme(DialogPane pane) {
        boolean light = "Light".equalsIgnoreCase(ThemeManager.getCurrentTheme());

        URL themeUrl = LoginDialog.class.getResource(light ? THEME_LIGHT : THEME_DARK);
        if (themeUrl != null) {
            pane.getStylesheets().add(themeUrl.toExternalForm());
        }

        URL loginUrl = LoginDialog.class.getResource(LOGIN_CSS);
        if (loginUrl != null) {
            pane.getStylesheets().add(loginUrl.toExternalForm());
        }

        pane.getStyleClass().add(light ? "theme-light" : "theme-dark");
    }

    // ------------------------------------------------------------------
    // Original dialog, kept so AuthenticationCoordinator still compiles.
    // ------------------------------------------------------------------
    public static Optional<Credentials> show(Window owner) {
        Dialog<Credentials> dialog = new Dialog<>();
        dialog.setTitle("PC DOQTAR — Sign in");
        dialog.setHeaderText("Sign in, or create an account to sync your diagnostics");

        if (owner != null) {
            dialog.initOwner(owner);
        }
        applyTheme(dialog.getDialogPane());

        ButtonType loginButton = new ButtonType("Sign in", ButtonBar.ButtonData.OK_DONE);
        ButtonType registerButton = new ButtonType("Create account", ButtonBar.ButtonData.LEFT);

        dialog.getDialogPane().getButtonTypes().addAll(
                registerButton, loginButton, ButtonType.CANCEL);

        TextField emailField = new TextField();
        emailField.setPromptText("Email address");
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

        dialog.setResultConverter(button -> {
            boolean register = button == registerButton;
            if (button == loginButton || register) {
                String email = emailField.getText().trim();
                String password = passwordField.getText();
                if (email.isBlank() || password.isBlank()) {
                    return new Credentials("", "", register);
                }
                return new Credentials(email, password, register);
            }
            return null;
        });

        return dialog.showAndWait();
    }
}
