package org.pcdoqtar.ota;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides access to the current application version and semver comparison utilities.
 */
public final class AppVersion {

    private static final Logger LOGGER = Logger.getLogger(AppVersion.class.getName());
    public static final String DEFAULT_VERSION = "1.2.0";

    private static String cachedVersion;

    private AppVersion() { }

    /**
     * Resolves the current application version string (e.g. "1.2.0").
     */
    public static synchronized String get() {
        if (cachedVersion != null) {
            return cachedVersion;
        }

        // 1. Try reading from version.properties resource
        try (InputStream in = AppVersion.class.getResourceAsStream("/org/pcdoqtar/version.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String ver = props.getProperty("version");
                if (ver != null && !ver.isBlank()) {
                    cachedVersion = cleanVersion(ver);
                    return cachedVersion;
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Could not load version.properties: {0}", ex.getMessage());
        }

        // 2. Try package implementation version
        Package pkg = AppVersion.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null && !pkg.getImplementationVersion().isBlank()) {
            cachedVersion = cleanVersion(pkg.getImplementationVersion());
            return cachedVersion;
        }

        cachedVersion = DEFAULT_VERSION;
        return cachedVersion;
    }

    /**
     * Cleans and normalizes a version string (strips 'v' or 'V' prefix, trim whitespace).
     */
    public static String cleanVersion(String version) {
        if (version == null) {
            return "";
        }
        String clean = version.trim();
        if (clean.startsWith("v") || clean.startsWith("V")) {
            clean = clean.substring(1).trim();
        }
        return clean;
    }

    /**
     * Compares two semantic version strings (e.g. "1.2.0" and "1.1.4").
     *
     * @return a positive number if v1 is newer than v2, negative if v1 is older than v2, 0 if equal.
     */
    public static int compare(String v1, String v2) {
        String c1 = cleanVersion(v1);
        String c2 = cleanVersion(v2);

        if (c1.equalsIgnoreCase(c2)) {
            return 0;
        }

        // Separate core version and pre-release tag (e.g. "1.2.0-beta.1")
        String[] parts1 = c1.split("-", 2);
        String[] parts2 = c2.split("-", 2);

        String[] segs1 = parts1[0].split("\\.");
        String[] segs2 = parts2[0].split("\\.");

        int maxLen = Math.max(segs1.length, segs2.length);
        for (int i = 0; i < maxLen; i++) {
            int num1 = i < segs1.length ? parseSafeInt(segs1[i]) : 0;
            int num2 = i < segs2.length ? parseSafeInt(segs2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        // Both numeric versions match; check pre-release suffix
        // Standard SemVer: 1.2.0 is NEWER than 1.2.0-beta
        boolean hasPre1 = parts1.length > 1 && !parts1[1].isBlank();
        boolean hasPre2 = parts2.length > 1 && !parts2[1].isBlank();

        if (hasPre1 && !hasPre2) {
            return -1; // 1.2.0-beta is older than 1.2.0
        } else if (!hasPre1 && hasPre2) {
            return 1;  // 1.2.0 is newer than 1.2.0-beta
        } else if (hasPre1 && hasPre2) {
            return parts1[1].compareToIgnoreCase(parts2[1]);
        }

        return 0;
    }

    /**
     * Checks if remoteVersion is strictly newer than localVersion.
     */
    public static boolean isNewer(String remoteVersion, String localVersion) {
        return compare(remoteVersion, localVersion) > 0;
    }

    private static int parseSafeInt(String s) {
        if (s == null || s.isBlank()) {
            return 0;
        }
        // Extract leading numeric characters
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (Character.isDigit(ch)) {
                sb.append(ch);
            } else {
                break;
            }
        }
        if (sb.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
