package org.pcdoqtar.ota;

/**
 * Immutable value object holding metadata for an available application update.
 */
public record UpdateInfo(
        String tagName,
        String version,
        String name,
        String releaseNotes,
        String downloadUrl,
        long assetSizeBytes,
        String htmlUrl,
        String publishedAt,
        boolean updateAvailable
) {
    public String getFormattedSize() {
        if (assetSizeBytes <= 0) {
            return "Unknown size";
        }
        double mb = assetSizeBytes / (1024.0 * 1024.0);
        return String.format(java.util.Locale.US, "%.1f MB", mb);
    }
}
