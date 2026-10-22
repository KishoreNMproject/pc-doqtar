
package org.pcdoqtar.diagnostics.storage;

import java.util.ArrayList;
import java.util.List;

import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

import org.pcdoqtar.diagnostics.HealthEvaluator;
import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.HealthStatus;

public class StorageDiagnostics {

    private final FileSystem fileSystem;
    private final HealthEvaluator healthEvaluator;

    public StorageDiagnostics() {
        SystemInfo systemInfo = new SystemInfo();
        fileSystem = systemInfo.getOperatingSystem().getFileSystem();
        healthEvaluator = new HealthEvaluator();
    }

    private double toGigabytes(long bytes) {
        return bytes / (1024.0 * 1024 * 1024);
    }

    public List<DiagnosticResult> diagnose() {
        List<DiagnosticResult> results = new ArrayList<>();

        for (OSFileStore store : fileSystem.getFileStores()) {
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();

            if (total <= 0 || usable < 0 || usable > total) {
                continue;
            }

            long used = total - usable;
            double percentage = used * 100.0 / total;

            DiagnosticResult evaluated =
                    healthEvaluator.evaluateUtilisation(
                            "Storage utilisation (" + store.getMount() + ")",
                            percentage);

            results.add(evaluated);
        }

        return results;
    }

    public void runCheck() {
        System.out.println("===== STORAGE DIAGNOSTICS =====");

        List<OSFileStore> stores = fileSystem.getFileStores();

        if (stores.isEmpty()) {
            System.out.println("No storage volumes detected.");
            System.out.println("===============================");
            return;
        }

        for (OSFileStore store : stores) {
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();

            if (total <= 0 || usable < 0 || usable > total) {
                continue;
            }

            long used = total - usable;
            double percentage = used * 100.0 / total;

            DiagnosticResult result =
                    healthEvaluator.evaluateUtilisation(
                            "Storage utilisation (" + store.getMount() + ")",
                            percentage);

            System.out.println("Volume: " + store.getMount());
            System.out.printf(
                    "Total capacity: %.2f GB%n", toGigabytes(total));
            System.out.printf(
                    "Used space: %.2f GB%n", toGigabytes(used));
            System.out.printf(
                    "Available space: %.2f GB%n", toGigabytes(usable));
            System.out.printf(
                    "Storage utilisation: %.2f%%%n", percentage);
            System.out.println("Health status: " + result.getStatus());

            if (result.getStatus() != HealthStatus.NORMAL) {
                System.out.println("NOTICE: " + result.getMessage());
            }

            System.out.println("-------------------------------");
        }

        System.out.println("Storage check complete.");
        System.out.println("===============================");
    }
}
