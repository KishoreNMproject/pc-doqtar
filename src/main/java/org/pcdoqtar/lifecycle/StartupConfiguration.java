package org.pcdoqtar.lifecycle;

import org.pcdoqtar.auth.FirebaseAuthService;

public final class StartupConfiguration {

    private StartupConfiguration() {
        // Utility class.
    }

    public static boolean hasFirebaseApiKey() {
        String value = FirebaseAuthService.resolveApiKey();
        return value != null && !value.isBlank();
    }

    public static String getFirebaseConfigurationError() {
        if (hasFirebaseApiKey()) {
            return "";
        }

        return "Firebase authentication is not configured.\n\n"
                + "Set the FIREBASE_WEB_API_KEY environment variable "
                + "before starting PC DOQTAR.";
    }
}