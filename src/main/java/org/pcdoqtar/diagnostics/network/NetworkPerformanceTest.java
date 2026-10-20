
package org.pcdoqtar.diagnostics.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class NetworkPerformanceTest {

    private static final String DOWNLOAD_URL =
            "https://speed.cloudflare.com/__down?bytes=";

    private static final String UPLOAD_URL =
            "https://speed.cloudflare.com/__up";

    private static final int DOWNLOAD_BYTES = 5_000_000;
    private static final int UPLOAD_BYTES = 1_000_000;
    private static final int PING_COUNT = 5;

    private final HttpClient httpClient;

    public NetworkPerformanceTest() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private long measureLatency() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DOWNLOAD_URL + "0"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        long start = System.nanoTime();

        HttpResponse<Void> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.discarding());

        long elapsed = System.nanoTime() - start;

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Latency request failed with HTTP "
                            + response.statusCode());
        }

        return elapsed / 1_000_000;
    }

    private void runLatencyTest() {
        System.out.println("----- INTERNET LATENCY TEST -----");

        long totalLatency = 0;
        int successfulRequests = 0;

        for (int i = 0; i < PING_COUNT; i++) {
            try {
                long latency = measureLatency();

                totalLatency += latency;
                successfulRequests++;

                System.out.printf(
                        "Request %d: %d ms%n",
                        i + 1, latency);

            } catch (IOException e) {
                System.out.printf(
                        "Request %d failed: %s%n",
                        i + 1, e.getMessage());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Latency test interrupted.");
                return;
            }
        }

        if (successfulRequests > 0) {
            System.out.printf(
                    "Average HTTP latency: %.2f ms%n",
                    totalLatency / (double) successfulRequests);
        } else {
            System.out.println("Latency test unavailable.");
        }

        System.out.println("---------------------------------");
    }

    private double measureDownloadSpeed()
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DOWNLOAD_URL + DOWNLOAD_BYTES))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        long start = System.nanoTime();

        HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray());

        long elapsed = System.nanoTime() - start;

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Download failed with HTTP "
                            + response.statusCode());
        }

        long receivedBytes = response.body().length;

        if (receivedBytes == 0 || elapsed <= 0) {
            throw new IOException("No download data received.");
        }

        return receivedBytes * 8.0 / (elapsed / 1_000_000_000.0)
                / 1_000_000.0;
    }

    private double measureUploadSpeed()
            throws IOException, InterruptedException {

        byte[] data = new byte[UPLOAD_BYTES];

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(UPLOAD_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();

        long start = System.nanoTime();

        HttpResponse<Void> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.discarding());

        long elapsed = System.nanoTime() - start;

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IOException(
                    "Upload failed with HTTP "
                            + response.statusCode());
        }

        if (elapsed <= 0) {
            throw new IOException("Invalid upload timing.");
        }

        return UPLOAD_BYTES * 8.0
                / (elapsed / 1_000_000_000.0)
                / 1_000_000.0;
    }

    public void runCheck() {
        System.out.println("===== INTERNET PERFORMANCE TEST =====");

        runLatencyTest();

        try {
            double downloadMbps = measureDownloadSpeed();

            System.out.printf(
                    "Download speed: %.2f Mbps%n",
                    downloadMbps);

        } catch (IOException e) {
            System.out.println(
                    "Download test unavailable: " + e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Download test interrupted.");
            return;
        }

        try {
            double uploadMbps = measureUploadSpeed();

            System.out.printf(
                    "Upload speed: %.2f Mbps%n",
                    uploadMbps);

        } catch (IOException e) {
            System.out.println(
                    "Upload test unavailable: " + e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Upload test interrupted.");
        }

        System.out.println("Internet performance test complete.");
        System.out.println("======================================");
    }
}
