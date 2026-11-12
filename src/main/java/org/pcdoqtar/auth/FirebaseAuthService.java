package org.pcdoqtar.auth;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

public class FirebaseAuthService {

    private static final String SIGN_IN_URL =
            "https://identitytoolkit.googleapis.com/v1/"
            + "accounts:signInWithPassword?key=";

    private static final String SIGN_UP_URL =
            "https://identitytoolkit.googleapis.com/v1/"
            + "accounts:signUp?key=";

    private static final String REFRESH_URL =
            "https://securetoken.googleapis.com/v1/token?key=";

    private static final String RESET_PASSWORD_URL =
            "https://identitytoolkit.googleapis.com/v1/"
            + "accounts:sendOobCode?key=";

    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");

    private final String explicitApiKey;
    private final boolean explicitKeyProvided;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthSession session;

    public FirebaseAuthService(AuthSession session) {
        this.session = session;
        this.explicitApiKey = null;
        this.explicitKeyProvided = false;
    }

    public FirebaseAuthService(AuthSession session, String apiKey) {
        this.session = session;
        this.explicitApiKey = apiKey != null && !apiKey.isBlank() ? apiKey.trim() : null;
        this.explicitKeyProvided = true;
    }

    /**
     * Resolves the Firebase Web API key across multiple sources:
     * 1. Java system properties (-DFIREBASE_WEB_API_KEY=..., -Dfirebase.web.api.key=..., -DFIREBASE_API_KEY=...)
     * 2. Process environment variables (FIREBASE_WEB_API_KEY, FIREBASE_API_KEY)
     * 3. Windows User/System Registry (HKCU\Environment, HKLM\...\Environment) so that keys
     *    configured via Windows Settings are resolved even if the current IDE/process was launched
     *    before the environment variable was broadcast.
     */
    public static String resolveApiKey() {
        String key = System.getProperty("FIREBASE_WEB_API_KEY");
        if (isNonEmpty(key)) return key.trim();

        key = System.getProperty("firebase.web.api.key");
        if (isNonEmpty(key)) return key.trim();

        key = System.getProperty("FIREBASE_API_KEY");
        if (isNonEmpty(key)) return key.trim();

        key = System.getenv("FIREBASE_WEB_API_KEY");
        if (isNonEmpty(key)) return key.trim();

        key = System.getenv("FIREBASE_API_KEY");
        if (isNonEmpty(key)) return key.trim();

        if (isWindows()) {
            key = readWindowsRegistryEnvironment("FIREBASE_WEB_API_KEY");
            if (isNonEmpty(key)) return key.trim();

            key = readWindowsRegistryEnvironment("FIREBASE_API_KEY");
            if (isNonEmpty(key)) return key.trim();
        }

        return null;
    }

    public String getApiKey() {
        if (explicitKeyProvided) {
            return explicitApiKey;
        }
        return resolveApiKey();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean isNonEmpty(String value) {
        return value != null && !value.isBlank();
    }

    private static String readWindowsRegistryEnvironment(String variableName) {
        try {
            if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, "Environment", variableName)) {
                String val = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, "Environment", variableName);
                if (isNonEmpty(val)) return val.trim();
            }
        } catch (Throwable ignored) {
        }

        try {
            if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE,
                    "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment", variableName)) {
                String val = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE,
                        "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment", variableName);
                if (isNonEmpty(val)) return val.trim();
            }
        } catch (Throwable ignored) {
        }

        // Fallback: query via reg command
        try {
            Process process = new ProcessBuilder("reg", "query", "HKCU\\Environment", "/v", variableName)
                    .redirectErrorStream(true)
                    .start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(variableName)) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 3) {
                            return parts[2].trim();
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Throwable ignored) {
        }

        return null;
    }

    public void signIn(String email, String password)
            throws IOException, InterruptedException {
        requestTokens(SIGN_IN_URL, email, password);
    }

    /** Creates a new Firebase account and signs in with it. */
    public void signUp(String email, String password)
            throws IOException, InterruptedException {
        requestTokens(SIGN_UP_URL, email, password);
    }

    private void requestTokens(String endpoint, String email, String password)
            throws IOException, InterruptedException {

        String effectiveKey = getApiKey();
        if (effectiveKey == null || effectiveKey.isBlank()) {
            throw new IOException(
                    "Optional sign-in is unavailable because FIREBASE_WEB_API_KEY is not configured.");
        }

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IOException("Enter both email and password.");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("returnSecureToken", true);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint + effectiveKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        mapper.writeValueAsString(body),
                        StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        JsonNode json = mapper.readTree(response.body());

        if (response.statusCode() != 200) {
            String message = json.path("error")
                    .path("message").asText("Sign-in failed.");

            throw new IOException(toUserMessage(message));
        }

        session.setTokens(
                json.path("localId").asText(),
                json.path("email").asText(email),
                json.path("idToken").asText(),
                json.path("refreshToken").asText(),
                json.path("expiresIn").asLong(3600));
    }

    public synchronized String getValidIdToken()
            throws IOException, InterruptedException {

        String effectiveKey = getApiKey();
        if (effectiveKey == null || effectiveKey.isBlank() || !session.isLoggedIn()) {
            return null;
        }

        // Refresh the ID token one minute before its expiry.
        if (System.currentTimeMillis()
                < session.getExpiresAtMillis() - 60_000L) {
            return session.getIdToken();
        }

        String refreshToken = session.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            session.clear();
            return null;
        }

        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "refresh_token");
        body.put("refresh_token", refreshToken);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(REFRESH_URL + effectiveKey))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "grant_type=refresh_token&refresh_token="
                        + java.net.URLEncoder.encode(
                                refreshToken, StandardCharsets.UTF_8)))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        JsonNode json = mapper.readTree(response.body());

        if (response.statusCode() != 200) {
            session.clear();
            return null;
        }

        session.setTokens(
                json.path("user_id").asText(session.getUid()),
                session.getEmail(),
                json.path("id_token").asText(),
                json.path("refresh_token").asText(refreshToken),
                json.path("expires_in").asLong(3600));

        return session.getIdToken();
    }

    public void logout() {
        session.clear();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Sends a password reset email using the Firebase Authentication REST API (accounts:sendOobCode).
     * Privacy-preserving: does not disclose whether the email address is registered.
     */
    public void sendPasswordResetEmail(String email)
            throws IOException, InterruptedException {

        String effectiveKey = getApiKey();
        if (effectiveKey == null || effectiveKey.isBlank()) {
            throw new IOException(
                    "Password reset is unavailable because FIREBASE_WEB_API_KEY is not configured.");
        }

        if (email == null || email.isBlank()) {
            throw new IOException("Enter your email address.");
        }

        String trimmedEmail = email.trim();
        if (!isValidEmail(trimmedEmail)) {
            throw new IOException("Enter a valid email address.");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("requestType", "PASSWORD_RESET");
        body.put("email", trimmedEmail);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESET_PASSWORD_URL + effectiveKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        mapper.writeValueAsString(body),
                        StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return;
        }

        JsonNode json = mapper.readTree(response.body());
        String errorCode = json.path("error").path("message").asText("");

        // Privacy-preserving: if the email is not found, do not reveal it to the caller
        if ("EMAIL_NOT_FOUND".equalsIgnoreCase(errorCode)) {
            return;
        }

        throw new IOException(toPasswordResetUserMessage(errorCode));
    }

    public static String toPasswordResetUserMessage(String firebaseError) {
        if (firebaseError == null || firebaseError.isBlank()) {
            return "Unable to send password reset email. Check your connection and try again.";
        }
        if (firebaseError.startsWith("INVALID_EMAIL") || firebaseError.startsWith("MISSING_EMAIL")) {
            return "Enter a valid email address.";
        }
        if (firebaseError.startsWith("TOO_MANY_ATTEMPTS_TRY_LATER")) {
            return "Too many attempts. Try again later.";
        }
        if (firebaseError.startsWith("API_KEY_INVALID")) {
            return "The Firebase Web API key is invalid.";
        }
        if (firebaseError.startsWith("OPERATION_NOT_ALLOWED")) {
            return "Password reset is not enabled in Firebase.";
        }
        return "Password reset request failed. Please try again later.";
    }

    private String toUserMessage(String firebaseError) {
        if (firebaseError.startsWith("WEAK_PASSWORD")) {
            return "Password must be at least 6 characters.";
        }
        return switch (firebaseError) {
            case "EMAIL_NOT_FOUND",
                 "INVALID_LOGIN_CREDENTIALS",
                 "INVALID_PASSWORD" ->
                    "Email or password is incorrect.";
            case "EMAIL_EXISTS" ->
                    "An account with this email already exists. Sign in instead.";
            case "USER_DISABLED" ->
                    "This account has been disabled.";
            case "INVALID_EMAIL" ->
                    "Enter a valid email address.";
            case "TOO_MANY_ATTEMPTS_TRY_LATER" ->
                    "Too many attempts. Try again later.";
            case "API_KEY_INVALID" ->
                    "The Firebase Web API key is invalid.";
            case "OPERATION_NOT_ALLOWED" ->
                    "Email/password sign-in is not enabled in Firebase.";
            default -> "Firebase sign-in failed: " + firebaseError;
        };
    }
}