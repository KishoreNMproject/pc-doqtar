
package org.pcdoqtar.diagnostics.storage;

import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

public class StorageDiagnostics {

    private final FileSystem fileSystem;

    public StorageDiagnostics() {
        SystemInfo systemInfo = new SystemInfo();
        fileSystem = systemInfo.getOperatingSystem().getFileSystem();
    }

    private double toGigabytes(long bytes) {
        return bytes / (1024.0 * 1024 * 1024);
    }

    public void runCheck() {
        System.out.println("===== STORAGE DIAGNOSTICS =====");

        java.util.List<OSFileStore> fileStores = fileSystem.getFileStores();
        if (fileStores.isEmpty()) {            System.out.println("No storage volumes detected.");
            return;
        }

        for (OSFileStore store : fileStores) {
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();

            if (total <= 0) {
                continue;
            }

            long used = total - usable;
            double usagePercentage = used * 100.0 / total;

            System.out.println("Volume: " + store.getMount());
            System.out.printf("Total capacity: %.2f GB%n",
                    toGigabytes(total));
            System.out.printf("Used space: %.2f GB%n",
                    toGigabytes(used));
            System.out.printf("Available space: %.2f GB%n",
                    toGigabytes(usable));
            System.out.printf("Storage utilisation: %.2f%%%n",
                    usagePercentage);

            if (usagePercentage > 75.0) {
                System.out.println("WARNING: Storage usage exceeds 75%.");
            }

            System.out.println("-------------------------------");
        }

        System.out.println("Storage check complete.");
        System.out.println("===============================");
    }
}
