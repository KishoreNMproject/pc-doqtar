package org.pcdoqtar.connection;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.regex.*;

public final class FirebaseAuthClient {
    // Firebase Console > Project settings > Web API key
    private static final String API_KEY = System.getenv("FIREBASE_API_KEY");
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public record AuthSession(String email, String idToken, String refreshToken, long expiresAtMillis) {
        public boolean isExpiring() { return System.currentTimeMillis() > expiresAtMillis - 60_000; }
    }

    public static AuthSession signIn(String email, String password) throws IOException, InterruptedException {
        return credentialCall("signInWithPassword", email, password);
    }

    public static AuthSession signUp(String email, String password) throws IOException, InterruptedException {
        return credentialCall("signUp", email, password);
    }

    /** Call before every upload: returns a fresh session if the token is about to expire. */
    public static AuthSession refreshIfNeeded(AuthSession s) throws IOException, InterruptedException {
        if (!s.isExpiring()) return s;
        String body = "grant_type=refresh_token&refresh_token=" + s.refreshToken();
        HttpRequest req = HttpRequest.newBuilder(
                URI.create("https://securetoken.googleapis.com/v1/token?key=" + API_KEY))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        String json = send(req);
        return new AuthSession(s.email(), field(json, "id_token"), field(json, "refresh_token"),
                System.currentTimeMillis() + Long.parseLong(field(json, "expires_in")) * 1000);
    }

    private static AuthSession credentialCall(String op, String email, String password)
            throws IOException, InterruptedException {
        if (API_KEY == null || API_KEY.isBlank())
            throw new IOException("FIREBASE_API_KEY environment variable is not set");
        String body = "{\"email\":\"" + esc(email) + "\",\"password\":\"" + esc(password)
                + "\",\"returnSecureToken\":true}";
        HttpRequest req = HttpRequest.newBuilder(
                URI.create("https://identitytoolkit.googleapis.com/v1/accounts:" + op + "?key=" + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        String json = send(req);
        return new AuthSession(email, field(json, "idToken"), field(json, "refreshToken"),
                System.currentTimeMillis() + Long.parseLong(field(json, "expiresIn")) * 1000);
    }

    private static String send(HttpRequest req) throws IOException, InterruptedException {
        HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() >= 400) throw new IOException(friendly(field(r.body(), "message")));
        return r.body();
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static String friendly(String code) {
        if (code.startsWith("EMAIL_EXISTS")) return "Account already exists";
        if (code.startsWith("WEAK_PASSWORD")) return "Password must be at least 6 characters";
        return switch (code) {
            case "INVALID_LOGIN_CREDENTIALS", "INVALID_PASSWORD", "EMAIL_NOT_FOUND" -> "Wrong email or password";
            case "INVALID_EMAIL" -> "Invalid email address";
            case "TOO_MANY_ATTEMPTS_TRY_LATER" -> "Too many attempts, try later";
            default -> code.isEmpty() ? "Login failed" : code;
        };
    }
}