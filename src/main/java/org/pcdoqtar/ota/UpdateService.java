package org.pcdoqtar.ota;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pcdoqtar.settings.AppSettings;

import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Service managing Over-The-Air (OTA) update checks, installer downloads,
 * and seamless application upgrades via GitHub Releases.
 */
public class UpdateService {

    private static final Logger LOGGER = Logger.getLogger(UpdateService.class.getName());
    public static final String DEFAULT_REPO = "KishoreNMproject/pc-doqtar";
    private static final String API_ENDPOINT_TEMPLATE = "https://api.github.com/repos/%s/releases/latest";

    private static final UpdateService INSTANCE = new UpdateService();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final String repository;

    public static UpdateService getInstance() {
        return INSTANCE;
    }

    public UpdateService() {
        this(resolveRepository(),
                HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(12))
                        .build(),
                new ObjectMapper());
    }

    public UpdateService(String repository, HttpClient httpClient, ObjectMapper objectMapper) {
        this.repository = repository;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pc-doqtar-ota-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    private static String resolveRepository() {
        String sysProp = System.getProperty("pcdoqtar.github.repo");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.trim();
        }
        String env = System.getenv("PCDOQTAR_GITHUB_REPO");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return DEFAULT_REPO;
    }

    public String getRepository() {
        return repository;
    }

    /**
     * Queries the latest release on GitHub and evaluates whether a newer version is available.
     */
    public CompletableFuture<Optional<UpdateInfo>> checkForUpdates() {
        String endpoint = String.format(API_ENDPOINT_TEMPLATE, repository);
        String currentVersion = AppVersion.get();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "PC-DOQTAR-OTA/" + currentVersion)
                .GET()
                .build();

        AppSettings.setLastUpdateCheckTime(System.currentTimeMillis());

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 404) {
                        LOGGER.log(Level.INFO, "No GitHub releases found for repository: {0}", repository);
                        return Optional.<UpdateInfo>empty();
                    }
                    if (response.statusCode() != 200) {
                        LOGGER.log(Level.WARNING, "GitHub Releases API returned status {0}: {1}",
                                new Object[]{response.statusCode(), response.body()});
                        return Optional.<UpdateInfo>empty();
                    }
                    return parseReleasePayload(response.body(), currentVersion);
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.INFO, "Could not check for updates (offline or API limit): {0}", ex.getMessage());
                    return Optional.<UpdateInfo>empty();
                });
    }

    /**
     * Parses the GitHub release JSON payload into an UpdateInfo object.
     */
    public Optional<UpdateInfo> parseReleasePayload(String jsonBody, String currentVersion) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            String tagName = root.path("tag_name").asText("");
            String releaseName = root.path("name").asText(tagName);
            String body = root.path("body").asText("");
            String htmlUrl = root.path("html_url").asText("");
            String publishedAt = root.path("published_at").asText("");

            String remoteVersion = AppVersion.cleanVersion(tagName);
            if (remoteVersion.isBlank()) {
                return Optional.empty();
            }

            boolean isNewer = AppVersion.isNewer(remoteVersion, currentVersion);

            // Locate the installer executable in assets
            String downloadUrl = null;
            long assetSize = 0L;

            JsonNode assetsNode = root.path("assets");
            if (assetsNode.isArray()) {
                // Priority 1: Exact match "PCDOQTAR-Setup.exe"
                for (JsonNode asset : assetsNode) {
                    String name = asset.path("name").asText("");
                    if ("PCDOQTAR-Setup.exe".equalsIgnoreCase(name)) {
                        downloadUrl = asset.path("browser_download_url").asText();
                        assetSize = asset.path("size").asLong(0L);
                        break;
                    }
                }
                // Priority 2: Any asset ending with .exe
                if (downloadUrl == null) {
                    for (JsonNode asset : assetsNode) {
                        String name = asset.path("name").asText("");
                        if (name.toLowerCase().endsWith(".exe")) {
                            downloadUrl = asset.path("browser_download_url").asText();
                            assetSize = asset.path("size").asLong(0L);
                            break;
                        }
                    }
                }
            }

            return Optional.of(new UpdateInfo(
                    tagName,
                    remoteVersion,
                    releaseName,
                    body,
                    downloadUrl,
                    assetSize,
                    htmlUrl,
                    publishedAt,
                    isNewer
            ));
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to parse GitHub release JSON: {0}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Downloads the installer executable to a temporary file while providing download progress updates.
     *
     * @param info             The update metadata containing the download URL.
     * @param progressConsumer Callback receiving progress fractions (0.0 to 1.0).
     * @param cancelFlag       Atomic flag allowing the caller to abort the download.
     * @return CompletableFuture completing with the Path to the downloaded installer.
     */
    public CompletableFuture<Path> downloadUpdate(
            UpdateInfo info,
            DoubleConsumer progressConsumer,
            AtomicBoolean cancelFlag) {

        if (info.downloadUrl() == null || info.downloadUrl().isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No download URL provided in release assets for this update."));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(info.downloadUrl()))
                .timeout(Duration.ofMinutes(5))
                .header("User-Agent", "PC-DOQTAR-OTA/" + AppVersion.get())
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Download failed with HTTP status " + response.statusCode());
                    }

                    long contentLength = response.headers()
                            .firstValueAsLong("Content-Length")
                            .orElse(info.assetSizeBytes());

                    try {
                        Path tempFile = Files.createTempFile("PCDOQTAR-Setup-v" + info.version() + "-", ".exe");
                        tempFile.toFile().deleteOnExit();

                        try (InputStream in = new BufferedInputStream(response.body());
                             OutputStream out = new BufferedOutputStream(Files.newOutputStream(tempFile))) {

                            byte[] buffer = new byte[32768];
                            int bytesRead;
                            long totalRead = 0;

                            while ((bytesRead = in.read(buffer)) != -1) {
                                if (cancelFlag != null && cancelFlag.get()) {
                                    Files.deleteIfExists(tempFile);
                                    throw new RuntimeException("Download cancelled by user.");
                                }

                                out.write(buffer, 0, bytesRead);
                                totalRead += bytesRead;

                                if (progressConsumer != null && contentLength > 0) {
                                    double progress = (double) totalRead / contentLength;
                                    progressConsumer.accept(Math.min(1.0, progress));
                                }
                            }
                            out.flush();
                        }

                        if (progressConsumer != null) {
                            progressConsumer.accept(1.0);
                        }

                        LOGGER.log(Level.INFO, "Downloaded installer to: {0} ({1} bytes)",
                                new Object[]{tempFile, Files.size(tempFile)});
                        return tempFile;

                    } catch (IOException ex) {
                        throw new RuntimeException("Error writing installer to disk: " + ex.getMessage(), ex);
                    }
                });
    }

    /**
     * Executes the downloaded installer and terminates the current application instance cleanly.
     *
     * @param installerPath Path to the downloaded installer executable.
     */
    public void installAndRestart(Path installerPath) throws IOException {
        if (installerPath == null || !Files.exists(installerPath)) {
            throw new IllegalArgumentException("Installer file does not exist: " + installerPath);
        }

        LOGGER.log(Level.INFO, "Launching installer: {0}", installerPath);

        // Launch the Inno Setup installer executable
        ProcessBuilder pb = new ProcessBuilder(
                installerPath.toAbsolutePath().toString(),
                "/CLOSEAPPLICATIONS"
        );
        pb.directory(installerPath.getParent().toFile());
        pb.start();

        // Exit the current JavaFX application to release lock on executable files
        Platform.runLater(() -> {
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Schedules a silent update check shortly after application launch.
     */
    public void scheduleStartupCheck(Stage stage) {
        scheduler.schedule(() -> {
            if (!AppSettings.isAutoUpdateCheckEnabled()) {
                return;
            }

            checkForUpdates().thenAccept(optionalInfo -> {
                if (optionalInfo.isPresent()) {
                    UpdateInfo info = optionalInfo.get();
                    if (info.updateAvailable()) {
                        Platform.runLater(() -> UpdateDialog.showUpdateAvailable(stage, info, this));
                    }
                }
            }).exceptionally(ex -> {
                LOGGER.log(Level.FINE, "Startup update check completed without notification: {0}", ex.getMessage());
                return null;
            });
        }, 4, TimeUnit.SECONDS);
    }
}
