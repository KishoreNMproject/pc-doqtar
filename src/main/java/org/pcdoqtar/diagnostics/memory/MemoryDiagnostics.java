
package org.pcdoqtar.diagnostics.memory;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

public class MemoryDiagnostics {

    private final GlobalMemory memory;

    public MemoryDiagnostics() {
        SystemInfo systemInfo = new SystemInfo();
        memory = systemInfo.getHardware().getMemory();
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

    private double toGigabytes(long bytes) {
        return bytes / (1024.0 * 1024 * 1024);
    }

    public void runCheck() {
        long total = getTotalMemory();
        long available = getAvailableMemory();
        long used = total - available;

        double usagePercentage = total > 0
                ? (used * 100.0 / total)
                : 0;

        System.out.println("===== MEMORY DIAGNOSTICS =====");
        System.out.printf("Total RAM: %.2f GB%n", toGigabytes(total));
        System.out.printf("Used RAM: %.2f GB%n", toGigabytes(used));
        System.out.printf("Available RAM: %.2f GB%n", toGigabytes(available));
        System.out.printf("Memory utilisation: %.2f%%%n", usagePercentage);
        System.out.println("==============================");
    }
}
