package org.pcdoqtar.auth;

public final class AuthSession {

    private String uid;
    private String email;
    private String idToken;
    private String refreshToken;
    private long expiresAtMillis;

    public boolean isLoggedIn() {
        return idToken != null && !idToken.isBlank();
    }

    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public void setTokens(
            String uid,
            String email,
            String idToken,
            String refreshToken,
            long expiresInSeconds) {

        this.uid = uid;
        this.email = email;
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.expiresAtMillis =
                System.currentTimeMillis() + expiresInSeconds * 1000L;
    }

    public void clear() {
        uid = null;
        email = null;
        idToken = null;
        refreshToken = null;
        expiresAtMillis = 0L;
    }
}