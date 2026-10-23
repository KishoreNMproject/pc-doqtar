package org.pcdoqtar.ui.components;

import javafx.scene.control.Button;
import javafx.scene.control.Label;

public final class DiagnosticPageSupport {

    private DiagnosticPageSupport() {
        // Utility class.
    }

    public static void setRunning(
            Button button,
            Label status,
            boolean running,
            String runningMessage,
            String idleMessage) {

        if (button != null) {
            button.setDisable(running);
        }

        if (status != null) {
            status.setText(running ? runningMessage : idleMessage);
        }
    }

    public static void setFailure(Label status, String message) {
        if (status != null) {
            status.setText(
                    "Operation failed: "
                    + (message == null || message.isBlank()
                            ? "No additional details are available."
                            : message));
        }
    }
}