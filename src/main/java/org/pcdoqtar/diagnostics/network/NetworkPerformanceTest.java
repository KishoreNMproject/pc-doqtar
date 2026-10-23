package org.pcdoqtar.diagnostics.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class NetworkPerformanceTest {

    private static final String DOWNLOAD_URL =
            "https://speed.cloudflare.com/__down?bytes=";

    private static final String UPLOAD_URL =
            "https://speed.cloudflare.com/__up";

    private static final int DOWNLOAD_BYTES = 5_000_000;
    private static final int UPLOAD_BYTES = 1_000_000;
    private static final int PING_COUNT = 5;
    private static final int SPEED_TEST_COUNT = 5;

    private final HttpClient httpClient;

    public NetworkPerformanceTest() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private long measureLatency()
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DOWNLOAD_URL + "0"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        long start = System.nanoTime();

        HttpResponse<Void> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.discarding()
        );

        long elapsed = System.nanoTime() - start;

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Latency request failed with HTTP "
                            + response.statusCode()
            );
        }

        return elapsed / 1_000_000;
    }

    public LatencyResult measureLatencySamples()
            throws InterruptedException {

        List<Long> samples = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (int i = 1; i <= PING_COUNT; i++) {
            try {
                long latency = measureLatency();
                samples.add(latency);

                System.out.printf(
                        "HTTP latency sample %d: %d ms%n",
                        i, latency
                );

            } catch (IOException exception) {
                failures.add(
                        "Sample " + i + ": " + exception.getMessage()
                );
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw exception;
            }
        }

        double average = -1;
        long minimum = -1;
        long maximum = -1;

        if (!samples.isEmpty()) {
            long total = 0;
            minimum = Long.MAX_VALUE;
            maximum = Long.MIN_VALUE;

            for (long sample : samples) {
                total += sample;
                minimum = Math.min(minimum, sample);
                maximum = Math.max(maximum, sample);
            }

            average = total / (double) samples.size();
        }

        return new LatencyResult(
                samples, failures, average, minimum, maximum
        );
    }

    private double measureDownloadSpeed()
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DOWNLOAD_URL + DOWNLOAD_BYTES))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        long start = System.nanoTime();

        HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );

        long elapsed = System.nanoTime() - start;

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Download failed with HTTP "
                            + response.statusCode()
            );
        }

        long receivedBytes = response.body().length;

        if (receivedBytes == 0 || elapsed <= 0) {
            throw new IOException("No download data received.");
        }

        return receivedBytes * 8.0
                / (elapsed / 1_000_000_000.0)
                / 1_000_000.0;
    }

    private double measureUploadSpeed()
            throws IOException, InterruptedException {

        byte[] data = new byte[UPLOAD_BYTES];

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(UPLOAD_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();

        long start = System.nanoTime();

        HttpResponse<Void> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.discarding()
        );

        long elapsed = System.nanoTime() - start;

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IOException(
                    "Upload failed with HTTP "
                            + response.statusCode()
            );
        }

        if (elapsed <= 0) {
            throw new IOException("Invalid upload timing.");
        }

        return UPLOAD_BYTES * 8.0
                / (elapsed / 1_000_000_000.0)
                / 1_000_000.0;
    }

    private double measureBestOfFive(boolean download)
            throws IOException, InterruptedException {

        double bestSpeed = -1;
        int successfulTests = 0;

        for (int i = 1; i <= SPEED_TEST_COUNT; i++) {
            try {
                double speed = download
                        ? measureDownloadSpeed()
                        : measureUploadSpeed();

                successfulTests++;

                System.out.printf(
                        "%s test %d: %.2f Mbps%n",
                        download ? "Download" : "Upload",
                        i, speed
                );

                if (speed > bestSpeed) {
                    bestSpeed = speed;
                }

            } catch (IOException exception) {
                System.out.printf(
                        "%s test %d failed: %s%n",
                        download ? "Download" : "Upload",
                        i, exception.getMessage()
                );
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw exception;
            }
        }

        if (successfulTests == 0) {
            throw new IOException(
                    "All five "
                            + (download ? "download" : "upload")
                            + " tests failed."
            );
        }

        return bestSpeed;
    }

    /**
     * Returns {best download Mbps, best upload Mbps}.
     */
    public double[] measureSpeeds()
            throws IOException, InterruptedException {

        System.out.println("----- BEST-OF-5 SPEED TEST -----");

        double download = measureBestOfFive(true);
        double upload = measureBestOfFive(false);

        System.out.println("--------------------------------");

        return new double[] { download, upload };
    }

    public void runCheck() {
        System.out.println("===== INTERNET PERFORMANCE TEST =====");

        try {
            LatencyResult latency = measureLatencySamples();

            if (latency.getAverageMs() >= 0) {
                System.out.printf(
                        "Average HTTP latency: %.2f ms%n",
                        latency.getAverageMs()
                );
            } else {
                System.out.println("HTTP latency unavailable.");
            }

            double[] speeds = measureSpeeds();

            System.out.printf(
                    "Final best download speed: %.2f Mbps%n",
                    speeds[0]
            );

            System.out.printf(
                    "Final best upload speed: %.2f Mbps%n",
                    speeds[1]
            );

        } catch (IOException exception) {
            System.out.println(
                    "Network test unavailable: " + exception.getMessage()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.out.println("Network test interrupted.");
        }

        System.out.println("======================================");
    }

    public static class LatencyResult {
        private final List<Long> samples;
        private final List<String> failures;
        private final double averageMs;
        private final long minimumMs;
        private final long maximumMs;

        public LatencyResult(
                List<Long> samples,
                List<String> failures,
                double averageMs,
                long minimumMs,
                long maximumMs) {

            this.samples = List.copyOf(samples);
            this.failures = List.copyOf(failures);
            this.averageMs = averageMs;
            this.minimumMs = minimumMs;
            this.maximumMs = maximumMs;
        }

        public List<Long> getSamples() {
            return samples;
        }

        public List<String> getFailures() {
            return failures;
        }

        public double getAverageMs() {
            return averageMs;
        }

        public long getMinimumMs() {
            return minimumMs;
        }

        public long getMaximumMs() {
            return maximumMs;
        }

        public int getSuccessfulCount() {
            return samples.size();
        }

        public int getFailedCount() {
            return failures.size();
        }
    }
}