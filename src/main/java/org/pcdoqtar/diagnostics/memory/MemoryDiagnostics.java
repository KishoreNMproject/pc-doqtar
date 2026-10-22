
package org.pcdoqtar.diagnostics.memory;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import org.pcdoqtar.diagnostics.HealthEvaluator;
import org.pcdoqtar.model.DiagnosticResult;
import org.pcdoqtar.model.HealthStatus;

public class MemoryDiagnostics {

    private final GlobalMemory memory;
    private final HealthEvaluator healthEvaluator;

    public MemoryDiagnostics() {
        SystemInfo systemInfo = new SystemInfo();
        memory = systemInfo.getHardware().getMemory();
        healthEvaluator = new HealthEvaluator();
    }

    public long getTotalMemory() {
        return memory.getTotal();
    }

    public long getAvailableMemory() {
        return memory.getAvailable();
    }

    public long getUsedMemory() {
        return getTotalMemory() - getAvailableMemory();
    }

    public double getMemoryUsagePercentage() {
        long total = getTotalMemory();

        if (total <= 0) {
            return Double.NaN;
        }

        return getUsedMemory() * 100.0 / total;
    }

    private double toGigabytes(long bytes) {
        return bytes / (1024.0 * 1024 * 1024);
    }

    public DiagnosticResult diagnose() {
        return healthEvaluator.evaluateUtilisation(
                "Memory utilisation",
                getMemoryUsagePercentage());
    }

    public void runCheck() {
        long total = getTotalMemory();
        long available = getAvailableMemory();
        long used = getUsedMemory();

        DiagnosticResult result = diagnose();

        System.out.println("===== MEMORY DIAGNOSTICS =====");
        System.out.printf("Total RAM: %.2f GB%n", toGigabytes(total));
        System.out.printf("Used RAM: %.2f GB%n", toGigabytes(used));
        System.out.printf(
                "Available RAM: %.2f GB%n", toGigabytes(available));
        System.out.printf(
                "Memory utilisation: %.2f%%%n", result.getValue());
        System.out.println("Health status: " + result.getStatus());

        if (result.getStatus() != HealthStatus.NORMAL) {
            System.out.println("NOTICE: " + result.getMessage());
        }

        System.out.println("==============================");
    }
}
