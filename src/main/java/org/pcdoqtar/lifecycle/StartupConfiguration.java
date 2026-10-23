package org.pcdoqtar.lifecycle;

public final class StartupConfiguration {

    private static final String FIREBASE_API_KEY =
            "FIREBASE_WEB_API_KEY";

    private StartupConfiguration() {
        // Utility class.
    }

    public static boolean hasFirebaseApiKey() {
        String value = System.getenv(FIREBASE_API_KEY);
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